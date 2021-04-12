package io.jbang.eclipse.core.internal.builder;

import static io.jbang.eclipse.core.internal.JBangFileUtils.isJBangFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;

import io.jbang.eclipse.core.JBangCorePlugin;
import io.jbang.eclipse.core.internal.JBangConstants;
import io.jbang.eclipse.core.internal.process.JBangDependencyError;
import io.jbang.eclipse.core.internal.process.JBangError;
import io.jbang.eclipse.core.internal.process.JBangExecution;
import io.jbang.eclipse.core.internal.process.JBangInfo;
import io.jbang.eclipse.core.internal.project.JBangProject;
import io.jbang.eclipse.core.internal.project.ProjectConfigurationManager;

public class JBangBuilder extends IncrementalProjectBuilder {

	private static final Integer MISSING_HASH = -1;
	private Map<IFile, Integer> configCache = new ConcurrentHashMap<>();
	
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IResourceDelta delta = getDelta(getProject());
		if (delta != null) {
			JBangResourceDeltaVisitor visitor = new JBangResourceDeltaVisitor();
			delta.accept(visitor);
			int filesModified = visitor.jbangFiles.size();
			int filesDeleted = visitor.deletedJbangFiles.size();
			SubMonitor subMonitor = SubMonitor.convert(monitor, filesModified+filesDeleted);
			
			if (!visitor.jbangFiles.isEmpty()) {
				configure(visitor.jbangFiles, subMonitor.split(filesModified));
			}
			if (!visitor.deletedJbangFiles.isEmpty()) {
				unconfigure(visitor.deletedJbangFiles,  subMonitor.split(filesDeleted));
			}
			subMonitor.done();
		}
		return new IProject[0];
	}

	private void configure(List<IFile> files, IProgressMonitor monitor) throws CoreException {
		ProjectConfigurationManager jbangManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		JBangProject jbp = jbangManager.getJBangProject(getProject());
		if (jbp == null) {
			return;
		}
		var jbang = jbp.getRuntime();
		for (IFile file : files) {
			Integer oldConfigHash = configCache.getOrDefault(file,MISSING_HASH);
			Integer newConfigHash = getConfigHash(file, monitor);
			if (Objects.equals(oldConfigHash, newConfigHash)) {
				continue;
			}
			monitor.setTaskName("Updating JBang configuration from "+file.getName());
			configCache.put(file, newConfigHash);
			System.err.println(file + " configuration changed, checking jbang info");
			JBangExecution execution = new JBangExecution(jbang, file.getLocation().toFile());
			JBangInfo info = execution.getInfo();
			clearMarkers(file);
			if (info != null) {
				if (info.getResolutionErrors() == null || info.getResolutionErrors().isEmpty()) {
					jbangManager.configure(jbp, info, monitor);
				} else {
					String source = getSource(file);
					info.getResolutionErrors().forEach(e -> {
						try {
							addErrorMarker(file, source, e);
						} catch (CoreException e1) {
							e1.printStackTrace();
						}
					});
				}
			}
		}
	}
	
	private String getSource(IFile file) throws JavaModelException {
		ICompilationUnit typeRoot = JavaCore.createCompilationUnitFrom(file);
		return typeRoot.getBuffer().getContents();
	}

	private void clearMarkers(IFile file) throws CoreException {
		file.deleteMarkers(JBangConstants.MARKER_ID, true, 1);
	}

	private void addErrorMarker(IFile file, String source, JBangError e) throws CoreException {
        IMarker marker = file.createMarker(JBangConstants.MARKER_RESOLUTION_ID);
        marker.setAttribute(IMarker.MESSAGE, e.getMessage());
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.TRANSIENT, true);
        if (e instanceof JBangDependencyError) {
        	String dependency= ((JBangDependencyError)e).getDependency();
        	Position pos = findPosition(dependency, source);
        	marker.setAttribute(IMarker.LINE_NUMBER, pos.line);
        	if (pos.start > 0) {
        		marker.setAttribute(IMarker.CHAR_START, pos.start);
        		marker.setAttribute(IMarker.CHAR_END, pos.end);
        	}
        } else {
        	marker.setAttribute(IMarker.LINE_NUMBER, 1);
        }
	}

	private Position findPosition(String dependency, String source) {
		Position pos = new Position();
		int line[] = new int[1];
		int lineOffset[] = new int[1];
		source.lines().filter(l -> {
			line[0]++;
			int i = l.indexOf(dependency);
			if (i > -1) {
				pos.line = line[0];
				pos.start = lineOffset[0]+i;
				pos.end = pos.start+ dependency.length();
			}
			lineOffset[0] += (1+l.length());
			return i > 0;
		}).findFirst();
		return pos;
	}

	private void unconfigure(List<IFile> files, IProgressMonitor monitor) throws CoreException {
		var projectConfigManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		projectConfigManager.unconfigure(getProject(), monitor);
	}

	class JBangResourceDeltaVisitor implements IResourceDeltaVisitor {
		
		List<IFile> jbangFiles = new ArrayList<>();
		List<IFile> deletedJbangFiles = new ArrayList<>();
		
		@Override
		public boolean visit(IResourceDelta delta) {
			if (delta != null) {
				IResource resource = delta.getResource();
				switch (resource.getType()) {
				case IResource.PROJECT: {
					if (delta.getKind() == IResourceDelta.REMOVED) {
						
						return false;
					}
					IProject project = (IProject) resource;
					return project.isAccessible();
				}
				case IResource.ROOT:
				case IResource.FOLDER: {
					return true;
				}
				case IResource.FILE: {
					switch (delta.getKind()) {
					case IResourceDelta.REMOVED: {
						IProject p = resource.getProject();
						cleanMarkers(p);
						deletedJbangFiles.add((IFile) resource);
						break;
					}
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED: {
						// if the content has changed clean + scan
						if ((delta.getFlags() & IResourceDelta.CONTENT) > 0) {
							if (isJBangFile(resource)) {
								jbangFiles.add((IFile) resource);
							}
							return true;
						}
						break;
					}
					default: {
					}
					}
				}
				}
			}
			return false;
		}
	}

	void cleanMarkers(IProject p) {
		//TODO implement me
	}
	
	@SuppressWarnings("unchecked")
	static Integer getConfigHash(IFile file, IProgressMonitor monitor) throws JavaModelException {
		ICompilationUnit typeRoot = JavaCore.createCompilationUnitFrom(file);
		CompilationUnit root = CoreASTProvider.getInstance().getAST(typeRoot, CoreASTProvider.WAIT_YES, monitor);
		if (root == null) {
			return 0;
		}
		JBangConfigVisitor configCollector = new JBangConfigVisitor(typeRoot.getSource());
		root.accept(configCollector);
		for (Comment comment : (List<Comment>)root.getCommentList()) {
		    comment.accept(configCollector);
		}
		return configCollector.getConfigElements().hashCode();
	}

	public static class JBangConfigVisitor extends ASTVisitor {
				
		private static final Pattern GROOVY_GRAPES = Pattern.compile("^(@Grab|@Grapes).*$");
		
		private static final Pattern JBANG_INSTRUCTIONS = Pattern.compile("^(//[A-Z]+ ).*$");
		
		private List<String> configElements = new ArrayList<>();
		
		private String source;
		
		public JBangConfigVisitor(String source) {
			this.source = source;
		}

		public List<String> getConfigElements() {
			return configElements;
		}
		
		@Override
		public boolean visit(SingleMemberAnnotation node) {
	        String annotation = getContent(node);
	        if (GROOVY_GRAPES.matcher(annotation).matches()) {
	        	configElements.add(annotation);	
	        }
			return super.visit(node);
		}
		
		@Override
		public boolean visit(MarkerAnnotation node) {
	        String annotation = getContent(node);
	        if (GROOVY_GRAPES.matcher(annotation).matches()) {
	        	configElements.add(annotation);	
	        }
			return super.visit(node);
		}
		
		@Override
		public boolean visit(LineComment node) {
			if (node.isLineComment()) {
				String comment = getContent(node);
				if (JBANG_INSTRUCTIONS.matcher(comment).matches()) {
					configElements.add(comment);									
				}
			}
			return super.visit(node);
		}
		
		private String getContent(ASTNode node) {
			int start = node.getStartPosition();
			int end = start + node.getLength();
			return source.substring(start, end).trim();
		}
		
	}
}
