package dev.jbang.eclipse.ls.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBangLSPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "jbang.eclipse.ls";

	// The shared instance
	private static JBangLSPlugin plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JBangLSPlugin getDefault() {
		return plugin;
	}

}
