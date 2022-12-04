package dev.jbang.eclipse.ui.internal.wizards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import dev.jbang.eclipse.ui.Activator;

public class JBangImportWizardPage extends WizardPage {

	private static final int MAX_HISTORY = 10;

	private Combo scriptCombo;

	/** the Map of field ids to List of comboboxes that share the same history */
	private final Map<String, List<Combo>> fieldsWithHistory = new HashMap<>();

	private boolean isHistoryLoaded = false;

	protected JBangImportWizardPage() {
		super("JBang");
		setTitle("Import JBang script");
		setDescription("Select a JBang java file");
		initDialogSettings();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		setControl(composite);

		final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
		selectRootDirectoryLabel.setLayoutData(new GridData());
		selectRootDirectoryLabel.setText("JBang script");

		scriptCombo = new Combo(composite, SWT.NONE);
		scriptCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		scriptCombo.setFocus();

		addFieldWithHistory("scriptCombo", scriptCombo);

		final Button browseButton = new Button(composite, SWT.NONE);
		browseButton.setText("Select script");
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
			dialog.setFilterExtensions(new String[] { "*.java","*.jsh","*.kt","*.groovy", "build.jbang" });
			dialog.setText("Select script");
			String result = dialog.open();
			if (result != null) {
				scriptCombo.setText(result);
			}
		}));

	}

	/** dialog settings to store input history */
	protected IDialogSettings dialogSettings;

	/** Adds an input control to the list of fields to save. */
	protected void addFieldWithHistory(String id, Combo combo) {
		if (combo != null) {
			List<Combo> combos = fieldsWithHistory.get(id);
			if (combos == null) {
				combos = new ArrayList<>();
				fieldsWithHistory.put(id, combos);
			}
			combos.add(combo);
		}
	}

	public Collection<Path> getScripts() {
		String path = scriptCombo.getText();
		Path script = Paths.get(path);
		if (Files.isRegularFile(script)) {
			return Collections.singleton(script);
		}
		return Collections.emptySet();
	}

	  /** Loads the dialog settings using the page name as a section name. */
	  private void initDialogSettings() {
	    IDialogSettings pluginSettings;

	    // This is strictly to get SWT Designer working locally without blowing up.
	    if(Activator.getDefault() == null) {
	      pluginSettings = new DialogSettings("Workbench");
	    } else {
	      pluginSettings = Activator.getDefault().getDialogSettings();
	    }

	    dialogSettings = pluginSettings.getSection(getName());
	    if(dialogSettings == null) {
	      dialogSettings = pluginSettings.addNewSection(getName());
	      pluginSettings.addSection(dialogSettings);
	    }
	  }


	/** Loads the input history from the dialog settings. */
	private void loadInputHistory() {
		for (Map.Entry<String, List<Combo>> e : fieldsWithHistory.entrySet()) {
			String id = e.getKey();
			String[] items = dialogSettings.getArray(id);
			if (items != null) {
				for (Combo combo : e.getValue()) {
					String text = combo.getText();
					combo.setItems(items);
					if (text.length() > 0) {
						// setItems() clears the text input, so we need to restore it
						combo.setText(text);
					}
				}
			}
		}
	}

	/** Saves the input history into the dialog settings. */
	private void saveInputHistory() {
		for (Map.Entry<String, List<Combo>> e : fieldsWithHistory.entrySet()) {
			String id = e.getKey();

			Set<String> history = new LinkedHashSet<>(MAX_HISTORY);

			for (Combo combo : e.getValue()) {
				String lastValue = combo.getText();
				if (lastValue != null && lastValue.trim().length() > 0) {
					history.add(lastValue);
				}
			}

			Combo combo = e.getValue().iterator().next();
			String[] items = combo.getItems();
			for (int j = 0; j < items.length && history.size() < MAX_HISTORY; j++) {
				history.add(items[j]);
			}

			dialogSettings.put(id, history.toArray(new String[history.size()]));
		}
	}

	@Override
	public void setVisible(boolean visible) {
	    if(visible) {
	      if(!isHistoryLoaded) {
	        // load data before history kicks in
	        loadInputHistory();
	        isHistoryLoaded = true;
	      } else {
	        saveInputHistory();
	      }
	    }
	    super.setVisible(visible);
	  }


	@Override
	public void dispose() {
		saveInputHistory();
		super.dispose();
	}

}
