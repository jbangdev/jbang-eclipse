package dev.jbang.eclipse.core.internal.project;

import static dev.jbang.eclipse.core.internal.JBangClasspathUtils.hasAttribute;
import static dev.jbang.eclipse.core.internal.JBangClasspathUtils.isOnClasspath;
import static dev.jbang.eclipse.core.internal.JBangClasspathUtils.isOnOutputLocation;
import static dev.jbang.eclipse.core.internal.ProjectUtils.addJBangNature;
import static dev.jbang.eclipse.core.internal.ProjectUtils.addJavaNature;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJavaProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import dev.jbang.eclipse.core.internal.JBangClasspathUtils;
import dev.jbang.eclipse.core.internal.JBangConstants;

public class SynchronizeJBangJob extends WorkspaceJob {

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
			monitor.setTaskName("JBang synchronizing on " + file);
			try {
				IProject project = file.getProject();
				if (project == null || !project.isAccessible()) {
					continue;
				}

				// check Java nature
				boolean newJavaProject = false;
				if (!isJavaProject(project)) {
					newJavaProject = addJavaNature(project, monitor);
				}
				// check JBang nature
				if (!isJBangProject(project)) {
					addJBangNature(project, monitor);
				}

				// check source folder
				IJavaProject javaProject = JavaCore.create(project);
				if (isOnOutputLocation(javaProject, file)) {
					continue;
				}
				if (!isOnClasspath(javaProject, file) || newJavaProject) {
					// TODO display wizard to configure build output?
					configureJava(javaProject, file, monitor);
				}
				configManager.synchronize(file, monitor);
			} catch (Exception e) {
				return new Status(IStatus.ERROR, JBangConstants.PLUGIN_ID, e.getMessage(), e);
			}
		}
		return Status.OK_STATUS;
	}

	private void configureJava(IJavaProject javaProject, IFile file, IProgressMonitor monitor) throws CoreException {
		IContainer src = JBangClasspathUtils.inferSourceDirectory(file, monitor);
		if (src != null) {
			addJBangSourceToClasspath(javaProject, src, monitor);
		}
	}

	private void addJBangSourceToClasspath(IJavaProject javaProject, IContainer src, IProgressMonitor monitor)
			throws CoreException {
		var classpath = javaProject.getRawClasspath();
		var newClasspath = new ArrayList<>(classpath.length);
		IClasspathEntry srcClasspath = null;
		for (IClasspathEntry cpe : classpath) {
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE && Objects.equals(src.getFullPath(), cpe.getPath())) {
				srcClasspath = cpe;
				if (!hasAttribute(cpe, "jbang.scope")) {
					var prevAttr = cpe.getExtraAttributes();
					var newAttr = new IClasspathAttribute[prevAttr.length + 1];
					System.arraycopy(prevAttr, 0, newAttr, 1, prevAttr.length);
					newAttr[0] = JavaCore.newClasspathAttribute("jbang.scope", "main");
					srcClasspath = JavaCore.newSourceEntry(cpe.getPath(), cpe.getInclusionPatterns(), cpe.getExclusionPatterns(),
							cpe.getOutputLocation(), newAttr);
				}
				newClasspath.add(srcClasspath);
			} else {
				newClasspath.add(cpe);
			}
		}
		if (srcClasspath == null) {
			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(src);
			IClasspathAttribute jbangScopeAttr = JavaCore.newClasspathAttribute("jbang.scope", "main");
			srcClasspath = JavaCore.newSourceEntry(root.getPath(), null, null, null,
					new IClasspathAttribute[] { jbangScopeAttr });
			newClasspath.add(srcClasspath);
		}

		javaProject.setRawClasspath(newClasspath.toArray(new IClasspathEntry[0]), monitor);
	}

}