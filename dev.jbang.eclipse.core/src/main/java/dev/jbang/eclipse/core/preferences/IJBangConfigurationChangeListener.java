package dev.jbang.eclipse.core.preferences;

import org.eclipse.core.runtime.CoreException;

public interface IJBangConfigurationChangeListener {
	
	public void jbangConfigurationChange(JBangConfigurationChangeEvent event) throws CoreException;

}
