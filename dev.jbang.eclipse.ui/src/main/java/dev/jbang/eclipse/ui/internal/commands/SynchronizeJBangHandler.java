package dev.jbang.eclipse.ui.internal.commands;

import static dev.jbang.eclipse.core.internal.JBangClasspathUtils.hasAttribute;
import static dev.jbang.eclipse.core.internal.JBangClasspathUtils.isOnClasspath;
import static dev.jbang.eclipse.core.internal.JBangFileUtils.getPackageName;
import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangFile;
import static dev.jbang.eclipse.core.internal.ProjectUtils.addJBangNature;
import static dev.jbang.eclipse.core.internal.ProjectUtils.addJavaNature;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJavaProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import dev.jbang.eclipse.ui.Activator;

public class SynchronizeJBangHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Object[] elements = null;
		if (selection instanceof IStructuredSelection) {
			elements = ((IStructuredSelection) selection).toArray();
		} else {
			elements = getFileInActiveEditor(event);
		}
		if (elements == null || elements.length == 0) {
			return null;
		}
		Set<IFile> jbangFiles = collectFiles(elements);
		if (!jbangFiles.isEmpty()) {
			var configManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
			new SynchronizeJBangJob(configManager, jbangFiles).schedule();
		}
		return null;
	}

	private IFile[] getFileInActiveEditor(ExecutionEvent event) {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof IEditorPart) {
			IEditorPart editorPart = (IEditorPart) activePart;
			if (editorPart.getEditorInput() instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) editorPart.getEditorInput()).getFile();
				IProject project = file.getProject();
				if (project != null && project.isAccessible()) {
					return new IFile[] { file };
				}
			}
		}
		return null;
	}

	private Set<IFile> collectFiles(Object[] elements) {
		Set<IFile> files = new LinkedHashSet<>();
		for (Object element : elements) {
			IResource file = Adapters.adapt(element, IResource.class);
			if (file != null && isJBangFile(file) && file.getLocation() != null) {
				files.add((IFile) file);
			}
		}
		return files;
	}

	private static class SynchronizeJBangJob extends WorkspaceJob {

		private Collection<IFile> jbangFiles;
		private ProjectConfigurationManager configManager;

		public SynchronizeJBangJob(ProjectConfigurationManager configManager, Collection<IFile> jbangFiles) {
			super("Synchronize JBang");
			this.jbangFiles = jbangFiles;
			this.configManager = configManager;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			for (IFile file : jbangFiles) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				try {
					IProject project = file.getProject();
					if (project == null || !project.isAccessible()) {
						continue;
					}
					
					//check Java nature
					boolean newJavaProject = false;
					if (!isJavaProject(project)) {
						newJavaProject = addJavaNature(project, monitor);
					}
					//check JBang nature
					if (!isJBangProject(project)) {
						addJBangNature(project, monitor);
					}

					//check source folder
					IJavaProject javaProject = JavaCore.create(project);
					if (!isOnClasspath(javaProject, file) || newJavaProject) {
						//TODO display wizard to configure build output?
						configureJava(javaProject, file, monitor);
					}
					configManager.synchronize(file, monitor);
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				}
			}
			return Status.OK_STATUS;
		}

		private void configureJava(IJavaProject javaProject, IFile file, IProgressMonitor monitor) throws CoreException {
			IContainer src = inferSourceDirectory(javaProject, file, monitor);
			//
			if (src != null) {
				addJBangSourceToClasspath(javaProject, src,  monitor);
			}
		}
		
		private void addJBangSourceToClasspath(IJavaProject javaProject, IContainer src, IProgressMonitor monitor) throws CoreException {
			var classpath = javaProject.getRawClasspath();
			var newClasspath = new ArrayList<>(classpath.length);
			IClasspathEntry srcClasspath = null;
			for (IClasspathEntry cpe : classpath) {
				if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					if (Objects.equals(src.getFullPath(), cpe.getPath())) {
						srcClasspath = cpe;
						if (!hasAttribute(cpe, "jbang.scope")) {
							var prevAttr = cpe.getExtraAttributes();
							var newAttr = new IClasspathAttribute[prevAttr.length + 1];
							System.arraycopy(prevAttr, 0, newAttr, 1, prevAttr.length);
							newAttr[0] = JavaCore.newClasspathAttribute("jbang.scope", "main");
							srcClasspath =  JavaCore.newSourceEntry(cpe.getPath(), cpe.getInclusionPatterns(), cpe.getExclusionPatterns(), cpe.getOutputLocation(), newAttr );
						}
						newClasspath.add(srcClasspath);										
					}
				} else {
					newClasspath.add(cpe);
				}
			}
			if (srcClasspath == null) {
				IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(src);
				IClasspathAttribute jbangScopeAttr = JavaCore.newClasspathAttribute("jbang.scope", "main");
				srcClasspath = JavaCore.newSourceEntry(root.getPath(), null, null, null, new IClasspathAttribute[] {jbangScopeAttr} );
				newClasspath.add(srcClasspath);				
			}
			
			javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[0]), monitor);
		}

		private IContainer inferSourceDirectory(IJavaProject javaProject, IFile file, IProgressMonitor monitor) {
			//Infer package name
			String packageName = getPackageName(javaProject, file);
			if (packageName == null) {
				//No package => parent container is the source folder
				IContainer sourceContainer = file.getParent();
				if (sourceContainer instanceof IFolder || sourceContainer instanceof IProject) {
					return sourceContainer;
				}
			}
			
			Path packagePath = new Path(packageName.replace(".", "/"));
			String[] segments =  packagePath.segments();
			
			boolean mismatch = false;
			IContainer currContainer = file.getParent();
			for (int i = segments.length - 1;currContainer != null && i >= 0; i--) {
				String packageSegment = segments[i];
				if (packageSegment.equals(currContainer.getName())) {
					currContainer = currContainer.getParent();
				} else {
					mismatch = true;
					break;
				}
			}
			if (currContainer != null && !mismatch) {
				return currContainer;
				
			}
			return null;
		}

	}
}
