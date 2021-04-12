package io.jbang.eclipse.core.internal;

import io.jbang.eclipse.core.IJBang;
import io.jbang.eclipse.core.internal.project.ProjectConfigurationManager;

public class JBangManager implements IJBang {

	@Override
	public ProjectConfigurationManager getProjectConfigurationManager() {
		return ProjectConfigurationManagerHolder.INSTANCE;
	}

	private static class ProjectConfigurationManagerHolder {
        static final ProjectConfigurationManager INSTANCE = new ProjectConfigurationManager();
    }

}
