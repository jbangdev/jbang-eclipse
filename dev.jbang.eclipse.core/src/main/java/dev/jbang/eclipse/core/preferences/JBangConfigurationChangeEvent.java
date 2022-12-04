package dev.jbang.eclipse.core.preferences;

public class JBangConfigurationChangeEvent {

	  private final String key;

	  private final Object newValue;

	  private final Object oldValue;

	  public JBangConfigurationChangeEvent(String key, Object newValue, Object oldValue) {
	    this.key = key;
	    this.newValue = newValue;
	    this.oldValue = oldValue;
	  }

	  public String getKey() {
	    return key;
	  }

	  public Object getNewValue() {
	    return newValue;
	  }

	  public Object getOldValue() {
	    return oldValue;
	  }

}
