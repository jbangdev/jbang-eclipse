package dev.jbang.eclipse.core.internal.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangClasspathUtils;

public class JBangClasspathContainerInitializer extends ClasspathContainerInitializer {

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {

		if (JBangClasspathUtils.isJBangClasspathContainer(containerPath)) {
			try {
				IClasspathContainer jbangContainer = JBangClasspathUtils.getSavedContainer(project.getProject());
				if (jbangContainer != null) {
					JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project },
							new IClasspathContainer[] { jbangContainer }, new NullProgressMonitor());
					return;
				}
			} catch (CoreException ex) {
				JBangCorePlugin.log("Exception initializing JBang classpath container " + containerPath.toString(), ex);
			}
		}
	}

}
