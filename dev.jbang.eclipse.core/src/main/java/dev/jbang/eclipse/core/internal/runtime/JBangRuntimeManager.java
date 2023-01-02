package dev.jbang.eclipse.core.internal.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import dev.jbang.eclipse.core.internal.JBangConstants;

public class JBangRuntimeManager {

	  public static final String DEFAULT = "DEFAULT"; //$NON-NLS-1$

	  public static final String EXTERNAL = "EXTERNAL"; //$NON-NLS-1$

	  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

	  private final IPreferencesService preferenceStore;

	  public JBangRuntimeManager() {
	    this.preferenceStore = Platform.getPreferencesService();
	    this.preferencesLookup[0] = InstanceScope.INSTANCE.getNode(JBangConstants.PLUGIN_ID);
	   // this.preferencesLookup[1] = DefaultScope.INSTANCE.getNode(JBangConstants.PLUGIN_ID);
	  }

	  public JBangRuntime getRuntime(String name) {
	    if(name == null || name.trim().isEmpty() || DEFAULT.equals(name.trim())) {
	      return getDefaultRuntime();
	    }
	    JBangRuntime runtime = getRuntimes().get(name);
	    if(runtime == null) {
	      runtime = getDefaultRuntime();
	    }
	    return runtime;
	  }

	  public JBangRuntime getDefaultRuntime() {
	    String name = preferenceStore.get(JBangConstants.P_DEFAULT_RUNTIME, null, preferencesLookup);
	    JBangRuntime runtime = getRuntimes().get(name);
	    if(runtime == null || !runtime.isValid()) {
	      runtime = JBangRuntime.SYSTEM;
	    }
	    return runtime;
	  }

	  public List<JBangRuntime> getJBangRuntimes(boolean availableOnly) {
	    List<JBangRuntime> jbangRuntimes = new ArrayList<>();
	    for(JBangRuntime jbangRuntime : getRuntimes().values()) {
	      if(!availableOnly || jbangRuntime.isValid()) {
	        jbangRuntimes.add(jbangRuntime);
	      }
	    }
	    return jbangRuntimes;
	  }

	  public void reset() {
	    preferencesLookup[0].remove(JBangConstants.P_RUNTIMES);
	    preferencesLookup[0].remove(JBangConstants.P_DEFAULT_RUNTIME);
	    removeRuntimePreferences();
	    flush();
	  }

	  public void setDefaultRuntime(JBangRuntime runtime) {
	    if(runtime == null) {
	      preferencesLookup[0].remove(JBangConstants.P_DEFAULT_RUNTIME);
	    } else {
	      preferencesLookup[0].put(JBangConstants.P_DEFAULT_RUNTIME, runtime.getName());
	    }
	    flush();
	  }

	  private void flush() {
	    try {
	      preferencesLookup[0].flush();
	    } catch(BackingStoreException ex) {
	      // TODO do nothing
	    }
	  }

	  public void setRuntimes(List<JBangRuntime> runtimes) {
	    removeRuntimePreferences();
	    Set<String> names = new HashSet<>();
	    StringBuilder sb = new StringBuilder();
	    for(JBangRuntime runtime : runtimes) {
	      if (!runtime.isEditable()) {
	    	  continue;
	      }
	      String name = runtime.getName();
	      if(!names.add(name)) {
	        throw new IllegalArgumentException();
	      }
	      if(sb.length() > 0) {
	          sb.append('|');
	      }
	      sb.append(name);
	      IEclipsePreferences runtimeNode = getRuntimePreferences(name, true);
          runtimeNode.put("location", runtime.getLocation().toPortableString());
          runtimeNode.put("version", runtime.getVersion());
	    }
	    preferencesLookup[0].put(JBangConstants.P_RUNTIMES, sb.toString());
	    flush();
	  }

	  private void removeRuntimePreferences() {
	    try {
	      if(preferencesLookup[0].nodeExists(JBangConstants.P_RUNTIMES_NODE)) {
	        preferencesLookup[0].node(JBangConstants.P_RUNTIMES_NODE).removeNode();
	      }
	    } catch(BackingStoreException ex) {
	      // assume the node does not exist
	    }
	  }

	  private IEclipsePreferences getRuntimePreferences(String name, boolean create) {
		Preferences runtimesNode = preferencesLookup[0].node(JBangConstants.P_RUNTIMES_NODE);
	    try {
	      if(runtimesNode.nodeExists(name) || create) {
	        return (IEclipsePreferences) runtimesNode.node(name);
	      }
	    } catch(BackingStoreException ex) {
	      // assume the node does not exist
	    }
	    return null;
	  }

	  public Map<String, JBangRuntime> getRuntimes() {
	    Map<String, JBangRuntime> runtimes = new LinkedHashMap<>();
	    runtimes.put(DEFAULT, JBangRuntime.SYSTEM);

	    String runtimesPreference = preferenceStore.get(JBangConstants.P_RUNTIMES, null, preferencesLookup);
	    if(runtimesPreference != null && runtimesPreference.length() > 0) {
	      for(String name : runtimesPreference.split("\\|")) { //$NON-NLS-1$
	        IEclipsePreferences preferences = getRuntimePreferences(name, false);
	        JBangRuntime runtime = createRuntime(name, preferences);
	        if (runtime != null) {
	        	runtimes.put(runtime.getName(), runtime);
	        }
	      }
	    }
	    return runtimes;
	  }

	  private JBangRuntime createRuntime(String name, IEclipsePreferences preferences) {
	    String location = preferences.get("location", null);
	    String version = preferences.get("version", null);
	    JBangRuntime runtime = null;
	    if(location != null) {
	      runtime = new JBangRuntime(name, location, version);
	    }
	    return runtime;
	  }
}
