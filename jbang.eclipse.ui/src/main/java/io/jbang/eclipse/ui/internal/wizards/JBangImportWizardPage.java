package io.jbang.eclipse.ui.internal.wizards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

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

public class JBangImportWizardPage extends WizardPage {

	private Combo scriptCombo;

	protected JBangImportWizardPage() {
		super("JBang");
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

		final Button browseButton = new Button(composite, SWT.NONE);
		browseButton.setText("Select script");
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
			dialog.setFilterExtensions(new String[] { "*.java" });
			dialog.setText("Select script");
			String result = dialog.open();
			if (result != null) {
				scriptCombo.setText(result);
			}
		}));

	}

	public Collection<Path> getScripts() {
		String path = scriptCombo.getText();
		Path script = Paths.get(path);
		if (Files.isRegularFile(script)) {
			return Collections.singleton(script);
		}
		return Collections.emptySet();
	}

}
