package io.jbang.eclipse.core;

import io.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import io.jbang.eclipse.core.internal.runtine.JBangRuntimeManager;

public interface IJBang {

	ProjectConfigurationManager getProjectConfigurationManager();

	JBangRuntimeManager getJBangRuntimeManager();
}
