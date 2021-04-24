package dev.jbang.eclipse.core.internal.runtine;

import org.eclipse.core.runtime.IProgressMonitor;

public class JBangRuntimeManager {

	private boolean initialized;

	private JBangRuntime systemJBang;
	
	//private Map<String, JBangRuntime> runtimes = new HashMap<>(1);
	
	public JBangRuntime getDefaultRuntime(IProgressMonitor monitor) {
		initializeIfNeeded(monitor);
		return systemJBang;
	}

	private void initializeIfNeeded(IProgressMonitor monitor) {
		//TODO add synchronization?
		if (!initialized) {
			if (systemJBang == null) {
				systemJBang = new JBangRuntime();
			}
			//TODO Read JBang preferences
			//runtimes.put("system", systemJBang);
		}
		initialized = true;
	}
}
