package dev.jbang.eclipse.ui.internal.preferences;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;

public class JBangInstallationWizardPage extends WizardPage {

	private Text location;

	private Text name;

	private Label version;

	private final JBangRuntime original;

	private Button btnDirectory;

	private final Set<String> usedNames;

	private static final Pattern nameVersionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

	public JBangInstallationWizardPage(JBangRuntime original, Set<String> usedNames) {
		super("Configure JBang installation");
		this.original = original;
		this.usedNames = usedNames;
		setDescription("Enter the home directory of the JBang Installation");

	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(3, false));

		Label lblInstallationLocation = new Label(container, SWT.NONE);
		lblInstallationLocation.setText("Installation location");

		location = new Text(container, SWT.BORDER);
		location.addModifyListener(e -> updateStatus());
		location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		btnDirectory = new Button(container, SWT.NONE);
		btnDirectory.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			selectLocationAction();
		}));
		btnDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDirectory.setText("Directory");

		Label lblInstallationName = new Label(container, SWT.NONE);
		lblInstallationName.setText("Name");

		name = new Text(container, SWT.BORDER);
		name.addModifyListener(e -> updateStatus());
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(container, SWT.NONE);// filler

		Label lblVersion = new Label(container, SWT.NONE);
		lblVersion.setText("Version");

		version = new Label(container, SWT.BORDER);
		version.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		if (original != null) {
			location.setText(original.getLocation().toOSString());
			name.setText(original.getName());
			version.setText(original.getVersion());
		}

		updateStatus();
	}

	protected void selectLocationAction() {
		DirectoryDialog dlg = new DirectoryDialog(getShell());
		dlg.setText("JBang installation directory");
		dlg.setMessage("Select JBang installation directory");
		String dir = dlg.open();
		if (dir == null) {
			return;
		}
		location.setText(dir);
		Job job = new Job("Check JBang Version Job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				var v = new JBangRuntime(dir).detectVersion(monitor);
				if (v != null && !v.isBlank()) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (name != null && !name.isDisposed()) {
								if (name.getText().isBlank()) {
									name.setText("JBang " + v);									
								} else {
									Matcher matcher = nameVersionPattern.matcher(name.getText());
									if (matcher.find()) {
										name.setText(matcher.replaceFirst(v));
									}
								}
							}
							if (version != null && !version.isDisposed()) {
								version.setText(v);
							}

						}

					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private boolean isValidJBangInstall(String dir) {
		if (dir == null || dir.length() == 0) {
			return false;
		}
		File selectedDir = new File(dir);
		if (!selectedDir.isDirectory()) {
			return false;
		}
		return new JBangRuntime(dir).isValid();
	}

	protected void updateStatus() {
		setPageComplete(false);
		setMessage(null);
		setErrorMessage(null);

		if (location.getText().trim().isEmpty()) {
			setMessage("Select an installation directory");
			return;
		}

		if (!isValidJBangInstall(location.getText())) {
			setErrorMessage("Directory is not a valid JBang installation");
			return;
		}

		if (name.getText().trim().isEmpty()) {
			setErrorMessage("Name must not be empty");
			return;
		}

		if (usedNames.contains(name.getText().trim())) {
			setErrorMessage("Name already used");
			return;
		}

		setPageComplete(true);
	}

	public JBangRuntime getResult() {
		return new JBangRuntime(name.getText(), location.getText(), version.getText());
	}
}