package dev.jbang.eclipse.core.internal.builder;

import static dev.jbang.eclipse.core.JBangCorePlugin.logInfo;
import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.project.JBangProject;
import dev.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimesDiscoveryJob;

public class JBangBuilder extends IncrementalProjectBuilder {

	private static final Integer MISSING_HASH = -1;
	//TODO evict cache of closed/deleted projects
	private Map<IFile, Integer> configCache = new ConcurrentHashMap<>();

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		IResourceDelta delta = getDelta(getProject());
		List<IFile> modified = new ArrayList<>();
		List<IFile> deleted = new ArrayList<>();
		if (delta != null) {
			JBangResourceDeltaVisitor visitor = new JBangResourceDeltaVisitor();
			delta.accept(visitor);
			if (visitor.jbangFiles != null) {
				modified.addAll(visitor.jbangFiles);
			}
			if (visitor.deletedJbangFiles != null) {
				deleted.addAll(visitor.deletedJbangFiles);
			}
		} else if (kind == FULL_BUILD) {
			ProjectConfigurationManager configManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
			JBangProject jbp = configManager.getJBangProject(getProject(), monitor);
			IFile mainScriptFile = jbp.getMainSourceFile();
			if (mainScriptFile != null) {
				modified.add(mainScriptFile);
			} //TODO else find jbang source folder, find first jbang script ?
		}
		
		int filesModified = modified.size();
		int filesDeleted = deleted.size();
		SubMonitor subMonitor = SubMonitor.convert(monitor, filesModified + filesDeleted);
		
		if (!modified.isEmpty()) {
			long startConfig = System.currentTimeMillis();
			var executedJBang = configure(modified, subMonitor.split(filesModified));
			long configured = System.currentTimeMillis() - startConfig;
			logInfo("JBang Builder "+(executedJBang?"configured ":"checked ")+filesModified+" files in "+configured+" ms");
		}
		if (!deleted.isEmpty()) {
			unconfigure(deleted, subMonitor.split(filesDeleted));
		}
		subMonitor.done();
		long elapsed = System.currentTimeMillis() - start;
		logInfo("JBang Builder ran in "+elapsed+" ms ["+ (kind == FULL_BUILD?"Full":"Incremental")+" build]");
		return new IProject[0];
	}

	private boolean configure(List<IFile> files, IProgressMonitor monitor) throws CoreException {
		
		try {
			//FIXME avoid potential deadlock
			Job.getJobManager().join(JBangRuntimesDiscoveryJob.class, monitor);
		} catch (Exception e) {
			//ignore
		}
		
		ProjectConfigurationManager configManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		
		boolean executedJBang = false;
		for (IFile file : files) {
			Integer oldConfigHash = configCache.getOrDefault(file, MISSING_HASH);
			Integer newConfigHash = getConfigHash(file, monitor);
			if (Objects.equals(oldConfigHash, newConfigHash)) {
				continue;
			}
			configCache.put(file, newConfigHash);
			configManager.synchronize(file, monitor);
		}
		return executedJBang;
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
		// TODO implement me
	}

	@SuppressWarnings("unchecked")
	static Integer getConfigHash(IFile file, IProgressMonitor monitor) throws JavaModelException {
		if (!"java".equals(file.getFileExtension())) {
			return null;
		}
		ICompilationUnit typeRoot = JavaCore.createCompilationUnitFrom(file);
		//FIXME This is uber slow. Once a file is saved, its AST is disposed, we're not benefiting from reusing a cached AST, 
		// hence pay the price of recomputing it from scratch
		CompilationUnit root = CoreASTProvider.getInstance().getAST(typeRoot, CoreASTProvider.WAIT_YES, monitor);
		if (root == null) {
			return 0;
		}
		JBangConfigVisitor configCollector = new JBangConfigVisitor(typeRoot.getSource());
		root.accept(configCollector);
		for (Comment comment : (List<Comment>) root.getCommentList()) {
			comment.accept(configCollector);
		}
		return configCollector.getConfigElements().hashCode();
	}	
}
