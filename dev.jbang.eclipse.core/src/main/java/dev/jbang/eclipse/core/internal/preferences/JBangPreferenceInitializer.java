package dev.jbang.eclipse.core.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import dev.jbang.eclipse.core.internal.JBangConstants;


/**
 * JBang preferences initializer.
 *
 * @author Fred Bricon
 */
public class JBangPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(JBangConstants.PLUGIN_ID);
    store.put(JBangConstants.P_RUNTIMES, ""); //$NON-NLS-1$
    store.put(JBangConstants.P_DEFAULT_RUNTIME, ""); //$NON-NLS-1$
  }
 }