package dev.jbang.eclipse.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import dev.jbang.eclipse.core.internal.JBangManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBangCorePlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "jbang.eclipse.core";

	// The shared instance
	private static JBangCorePlugin plugin;

	private IJBang jbangManager;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		jbangManager = new JBangManager();
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		jbangManager = null;
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JBangCorePlugin getDefault() {
		return plugin;
	}

	public static IJBang getJBangManager() {
		return plugin == null ? null : plugin.jbangManager;
	}

}
