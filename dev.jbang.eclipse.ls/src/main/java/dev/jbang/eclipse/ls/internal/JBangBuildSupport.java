package dev.jbang.eclipse.ls.internal;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;

import dev.jbang.eclipse.core.internal.JBangFileUtils;
import dev.jbang.eclipse.core.internal.ProjectUtils;

@SuppressWarnings("restriction")
public class JBangBuildSupport implements IBuildSupport {

	@Override
	public boolean applies(IProject project) {
		return ProjectUtils.isJBangProject(project);
	}

	@Override
	public boolean isBuildFile(IResource resource) {
		return JBangFileUtils.isJBangBuildFile(resource);
	}

	@Override
	public boolean hasSpecificDeleteProjectLogic() {
		return true;
	}

	@Override
	public void deleteInvalidProjects(Collection<IPath> rootPaths, ArrayList<IProject> deleteProjectCandates, IProgressMonitor monitor) {
		for (IProject project : deleteProjectCandates) {
			if (applies(project)) {
				try {
					project.getDescription();
				} catch (CoreException e) {
					try {
						project.delete(true, monitor);
					} catch (CoreException e1) {
						JavaLanguageServerPlugin.logException(e1.getMessage(), e1);
					}
				}
			}
		}
	}
	
	@Override
	public void discoverSource(IClassFile classFile, IProgressMonitor monitor) throws CoreException {
		JavaLanguageServerPlugin.getDefaultSourceDownloader().discoverSource(classFile, monitor);
	}

}
