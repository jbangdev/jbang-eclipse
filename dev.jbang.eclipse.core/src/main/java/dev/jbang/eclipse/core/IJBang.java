package dev.jbang.eclipse.core;

import dev.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import dev.jbang.eclipse.core.internal.runtine.JBangRuntimeManager;

public interface IJBang {

	ProjectConfigurationManager getProjectConfigurationManager();

	JBangRuntimeManager getJBangRuntimeManager();
}
