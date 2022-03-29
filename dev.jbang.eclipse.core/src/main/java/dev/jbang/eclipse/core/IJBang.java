package dev.jbang.eclipse.core;

import dev.jbang.eclipse.core.internal.project.ProjectConfigurationManager;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;

public interface IJBang {

	ProjectConfigurationManager getProjectConfigurationManager();

	JBangRuntimeManager getJBangRuntimeManager();
}
