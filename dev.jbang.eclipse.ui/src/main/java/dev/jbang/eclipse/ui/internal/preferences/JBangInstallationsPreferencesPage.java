package dev.jbang.eclipse.ui.internal.preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;
import dev.jbang.eclipse.ui.Activator;

public class JBangInstallationsPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private final JBangRuntimeManager runtimeManager;

	private String defaultRuntime;

	private List<JBangRuntime> runtimes;

	CheckboxTableViewer runtimesViewer;

	public JBangInstallationsPreferencesPage() {
		super();
		setTitle("JBang installations");
		noDefaultAndApplyButton();
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Manage JBang installations");

		this.runtimeManager = JBangCorePlugin.getJBangManager().getJBangRuntimeManager();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void performDefaults() {
		runtimeManager.reset();
		defaultRuntime = runtimeManager.getRuntime(JBangRuntime.SYSTEM).getName();
		runtimes = runtimeManager.getJBangRuntimes(false);

		runtimesViewer.setInput(runtimes);
		refreshRuntimesViewer();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		runtimeManager.setRuntimes(runtimes);
		runtimeManager.setDefaultRuntime(getDefaultRuntime());
		return true;
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.marginBottom = 5;
		gridLayout.marginRight = 5;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		composite.setLayout(gridLayout);

		createTable(composite);
		new Label(composite, SWT.NONE);

		defaultRuntime = runtimeManager.getDefaultRuntime().getName();
		runtimes = runtimeManager.getJBangRuntimes(false);

		runtimesViewer.setInput(runtimes);
		refreshRuntimesViewer();

		return composite;
	}

	private JBangRuntime getDefaultRuntime() {
		JBangRuntime systemRuntime = null;
		for (JBangRuntime runtime : runtimes) {
			if (defaultRuntime.equals(runtime.getName())) {
				return runtime;
			}
			if (JBangRuntime.SYSTEM.equals(runtime.getName())) {
				systemRuntime = runtime;
			}
		}
		return systemRuntime;
	}

	protected void refreshRuntimesViewer() {
		runtimesViewer.refresh(); // should listen on property changes instead?

		Object[] checkedElements = runtimesViewer.getCheckedElements();
		if (checkedElements == null || checkedElements.length == 0) {
			JBangRuntime runtime = getDefaultRuntime();
			runtimesViewer.setChecked(runtime, true);
			defaultRuntime = runtime.getName();
		}

		for (TableColumn column : runtimesViewer.getTable().getColumns()) {
			column.pack();
		}
	}

	protected JBangRuntime getSelectedJBangRuntime() {
		IStructuredSelection sel = (IStructuredSelection) runtimesViewer.getSelection();
		return (JBangRuntime) sel.getFirstElement();
	}

	private void createTable(Composite composite) {
		runtimesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);

		runtimesViewer.setLabelProvider(new RuntimesLabelProvider());

		runtimesViewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public Object[] getElements(Object input) {
				if (input instanceof List<?> list) {
					if (list.size() > 0) {
						return list.toArray(new JBangRuntime[list.size()]);
					}
				}
				return new Object[0];
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}

		});

		Table table = runtimesViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3);
		gd_table.heightHint = 151;
		gd_table.widthHint = 333;
		table.setLayoutData(gd_table);

		TableColumn tblclmnName = new TableColumn(table, SWT.NONE);
		tblclmnName.setWidth(100);
		tblclmnName.setText("Name");

		TableColumn tblclmnDetails = new TableColumn(table, SWT.NONE);
		tblclmnDetails.setWidth(100);
		tblclmnDetails.setText("Details");

		Button addButton = new Button(composite, SWT.NONE);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.setText("Add...");
		addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			JBangInstallationWizard wizard = new JBangInstallationWizard(getForbiddenNames(null));
			WizardDialog dialog = new WizardDialog(getShell(), wizard);
			if (dialog.open() == Window.OK) {
				runtimes.add(wizard.getResult());
				refreshRuntimesViewer();
			}
		}));

		final Button editButton = new Button(composite, SWT.NONE);
		editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		editButton.setEnabled(false);
		editButton.setText("Edit...");
		editButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			JBangRuntime runtime = getSelectedJBangRuntime();
			JBangInstallationWizard wizard = new JBangInstallationWizard(runtime, getForbiddenNames(runtime));
			WizardDialog dialog = new WizardDialog(getShell(), wizard);
			if (dialog.open() == Window.OK) {
				JBangRuntime updatedRuntime = wizard.getResult();
				for (int i = 0; i < runtimes.size(); i++) {
					if (runtime == runtimes.get(i)) {
						runtimes.set(i, updatedRuntime);
						break;
					}
				}
				refreshRuntimesViewer();
			}
		}));

		final Button removeButton = new Button(composite, SWT.NONE);
		removeButton.setEnabled(false);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeButton.setText("Remove");
		removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			JBangRuntime runtime = getSelectedJBangRuntime();
			runtimes.remove(runtime);
			refreshRuntimesViewer();
		}));

		runtimesViewer.addSelectionChangedListener(event -> {
			if (runtimesViewer.getSelection() instanceof IStructuredSelection) {
				JBangRuntime runtime = getSelectedJBangRuntime();
				boolean isEnabled = runtime != null && runtime.isEditable();
				removeButton.setEnabled(isEnabled);
				editButton.setEnabled(isEnabled);
			}
		});

		runtimesViewer.addCheckStateListener(event -> setCheckedRuntime((JBangRuntime) event.getElement()));

//	    Label noteLabel = new Label(composite, SWT.WRAP);
//	    GridData noteLabelData = new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1);
//	    noteLabelData.widthHint = 100;
//	    noteLabel.setLayoutData(noteLabelData);
//	    noteLabel.setText("Bla bla");
	}

	protected Set<String> getForbiddenNames(JBangRuntime runtime) {
		Set<String> names = new HashSet<>();
		for (JBangRuntime other : runtimes) {
			if (other != runtime) {
				names.add(other.getName());
			}
		}
		return names;
	}

	protected void setCheckedRuntime(JBangRuntime runtime) {
		runtimesViewer.setAllChecked(false);
		if (runtime == null || !runtime.isValid()) {
			runtime = getDefaultRuntime();
		} else {
			defaultRuntime = runtime.getName();
		}
		runtimesViewer.setChecked(runtime, true);
	}

	static class RuntimesLabelProvider implements ITableLabelProvider, IColorProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			JBangRuntime runtime = (JBangRuntime) element;
			switch (columnIndex) {
			case 0:
				return runtime.getName();
			case 1:
				StringBuilder sb = new StringBuilder();
				if (!runtime.isValid()) {
					sb.append("[not available] ");
				}
				sb.append(runtime.toString());
				return sb.toString();
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 1 && !((JBangRuntime) element).isValid()) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			JBangRuntime runtime = (JBangRuntime) element;
			if (!runtime.isEditable()) {
				return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			return null;
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}
	}

}
