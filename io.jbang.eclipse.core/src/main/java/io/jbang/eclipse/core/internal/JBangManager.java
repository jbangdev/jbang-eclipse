package io.jbang.eclipse.core.internal;

import io.jbang.eclipse.core.IJBang;
import io.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import io.jbang.eclipse.core.internal.runtine.JBangRuntimeManager;

public class JBangManager implements IJBang {

	@Override
	public ProjectConfigurationManager getProjectConfigurationManager() {
		return ProjectConfigurationManagerHolder.INSTANCE;
	}

	private static class ProjectConfigurationManagerHolder {
		static final ProjectConfigurationManager INSTANCE = new ProjectConfigurationManager(JBangRuntimeManagerHolder.INSTANCE);
	}

	@Override
	public JBangRuntimeManager getJBangRuntimeManager() {
		return JBangRuntimeManagerHolder.INSTANCE;
	}
	
	private static class JBangRuntimeManagerHolder {
		static final JBangRuntimeManager INSTANCE = new JBangRuntimeManager();
	}

}
