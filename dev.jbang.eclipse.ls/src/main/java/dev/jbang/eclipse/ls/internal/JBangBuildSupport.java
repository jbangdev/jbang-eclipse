package dev.jbang.eclipse.ls.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ls.core.internal.managers.IBuildSupport;

import dev.jbang.eclipse.core.internal.ProjectUtils;

@SuppressWarnings("restriction")
public class JBangBuildSupport implements IBuildSupport {

	@Override
	public boolean applies(IProject project) {
		return ProjectUtils.isJBangProject(project);
	}

	@Override
	public boolean isBuildFile(IResource resource) {
		return false;
	}

}
