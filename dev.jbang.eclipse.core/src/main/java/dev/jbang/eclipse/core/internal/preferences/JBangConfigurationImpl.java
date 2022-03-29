package dev.jbang.eclipse.core.internal.preferences;

import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.osgi.service.prefs.BackingStoreException;

import dev.jbang.eclipse.core.internal.JBangConstants;
import dev.jbang.eclipse.core.preferences.IJBangConfigurationChangeListener;
import dev.jbang.eclipse.core.preferences.JBangConfigurationChangeEvent;


public class JBangConfigurationImpl implements IPreferenceChangeListener, INodeChangeListener {

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  private final IPreferencesService preferenceStore;

  private final ListenerList<IJBangConfigurationChangeListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

  public JBangConfigurationImpl() {
    preferenceStore = Platform.getPreferencesService();

    init();
  }

  private boolean exists(IEclipsePreferences preferenceNode) {
    if(preferenceNode == null) {
      return false;
    }
    try {
      return preferenceNode.nodeExists("");
    } catch(BackingStoreException ex) {
      return false;
    }
  }

  private void init() {
    if(exists(preferencesLookup[0])) {
      ((IEclipsePreferences) preferencesLookup[0].parent()).removeNodeChangeListener(this);
      preferencesLookup[0].removePreferenceChangeListener(this);
    }
    preferencesLookup[0] = InstanceScope.INSTANCE.getNode(JBangConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[0].parent()).addNodeChangeListener(this);
    preferencesLookup[0].addPreferenceChangeListener(this);

    if(exists(preferencesLookup[1])) {
      ((IEclipsePreferences) preferencesLookup[1].parent()).removeNodeChangeListener(this);
      preferencesLookup[1].removePreferenceChangeListener(this);
    }
    preferencesLookup[1] = DefaultScope.INSTANCE.getNode(JBangConstants.PLUGIN_ID);
    ((IEclipsePreferences) preferencesLookup[1].parent()).addNodeChangeListener(this);
  }

  public synchronized void addConfigurationChangeListener(IJBangConfigurationChangeListener listener) {
    this.listeners.add(listener);
  }

  public void preferenceChange(PreferenceChangeEvent event) {
    JBangConfigurationChangeEvent jbangEvent = new JBangConfigurationChangeEvent(event.getKey(), event.getNewValue(), event.getOldValue());
    for(IJBangConfigurationChangeListener listener : listeners) {
      try {
        listener.jbangConfigurationChange(jbangEvent);
      } catch(Exception e) {
        //log.error("Could not deliver JBang configuration change event", e);
      }
    }
  }

  public void added(NodeChangeEvent event) {
  }

  public void removed(NodeChangeEvent event) {
    if(event.getChild() == preferencesLookup[0] || event.getChild() == preferencesLookup[1]) {
      init();
    }
  }

  private IPreferenceFilter getPreferenceFilter() {
    return new IPreferenceFilter() {
      public String[] getScopes() {
        return new String[] {InstanceScope.SCOPE, DefaultScope.SCOPE};
      }

      @SuppressWarnings("rawtypes")
      public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
        return null;
      }
    };
  }

}