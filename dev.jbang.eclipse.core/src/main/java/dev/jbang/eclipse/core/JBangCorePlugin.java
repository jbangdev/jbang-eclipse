package dev.jbang.eclipse.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import dev.jbang.eclipse.core.internal.JBangManager;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimesDiscoveryJob;

/**
 * The activator class controls the plug-in life cycle
 */
public class JBangCorePlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "dev.jbang.eclipse.core";

	// The shared instance
	private static JBangCorePlugin plugin;

	private IJBang jbangManager;

	private JBangRuntimesDiscoveryJob jbangRuntimesDiscoveryJob;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		jbangManager = new JBangManager();
		jbangRuntimesDiscoveryJob = new JBangRuntimesDiscoveryJob(jbangManager.getJBangRuntimeManager());
		jbangRuntimesDiscoveryJob.schedule();
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		jbangManager = null;
		jbangRuntimesDiscoveryJob.cancel();
		jbangRuntimesDiscoveryJob = null;
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

	public static void log(IStatus status) {
		if (plugin != null) {
			Platform.getLog(plugin.getBundle()).log(status);
		}
	}

	public static void log(CoreException e) {
		log(e.getStatus());
	}

	public static void logError(String message) {
		if (plugin != null) {
			log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(), message));
		}
	}

	public static void logInfo(String message) {
		if (plugin != null) {
			log(new Status(IStatus.INFO, plugin.getBundle().getSymbolicName(), message));
		}
	}

	public static void log(String message, Throwable ex) {
		if (plugin != null) {
			log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(), message, ex));
		}
	}

}
