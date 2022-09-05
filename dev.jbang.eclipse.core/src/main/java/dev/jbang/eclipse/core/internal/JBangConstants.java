package dev.jbang.eclipse.core.internal;

import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;

public final class JBangConstants {

	public static final String PLUGIN_ID = "dev.jbang.eclipse.core"; //$NON-NLS-1$

	public static final String NATURE_ID = PLUGIN_ID + ".jbangnature"; //$NON-NLS-1$

	public static final String BUILDER_ID = PLUGIN_ID + ".jbangbuilder"; //$NON-NLS-1$

	public static final String MARKER_ID = PLUGIN_ID + ".jbangproblem"; //$NON-NLS-1$

	public static final String MARKER_RESOLUTION_ID = MARKER_ID + ".resolution"; //$NON-NLS-1$

	private static final String PREFIX = "dev.jbang.";

	/**
	 * String, list of configured JBang installations separated by '|', see
	 * {@link JBangRuntimeManager}
	 */
	public static final String P_RUNTIMES = PREFIX + "runtimes"; //$NON-NLS-1$

	/**
	 * Root node of extended JBang installation attributes, see
	 * {@link JBangRuntimeManager}
	 */
	public static final String P_RUNTIMES_NODE = PREFIX + "runtimesNodes"; //$NON-NLS-1$

	public static final String P_DEFAULT_RUNTIME = PREFIX + "defaultRuntime"; //$NON-NLS-1$

	public static final String JBANG_BUILD = "build.jbang";
	
	public static final String JBANG_MAIN = "main.java";

	private JBangConstants() {
		// no instanciation
	}

}
