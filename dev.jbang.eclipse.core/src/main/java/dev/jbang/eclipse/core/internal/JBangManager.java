package dev.jbang.eclipse.core.internal;

import dev.jbang.eclipse.core.IJBang;
import dev.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;

public class JBangManager implements IJBang {

	@Override
	public ProjectConfigurationManager getProjectConfigurationManager() {
		return ProjectConfigurationManagerHolder.INSTANCE;
	}

	private static class ProjectConfigurationManagerHolder {
		static final ProjectConfigurationManager INSTANCE = new ProjectConfigurationManager(JBangRuntimeManagerHolder.JRM_INSTANCE);
	}

	@Override
	public JBangRuntimeManager getJBangRuntimeManager() {
		return JBangRuntimeManagerHolder.JRM_INSTANCE;
	}

	private static class JBangRuntimeManagerHolder {
		static final JBangRuntimeManager JRM_INSTANCE = new JBangRuntimeManager();
	}

}
