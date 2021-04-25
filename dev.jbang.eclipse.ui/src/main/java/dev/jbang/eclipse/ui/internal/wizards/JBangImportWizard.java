package dev.jbang.eclipse.ui.internal.wizards;

import java.nio.file.Path;
import java.util.Collection;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import dev.jbang.eclipse.core.internal.imports.ImportJBangScriptsJob;
import dev.jbang.eclipse.ui.Activator;

public class JBangImportWizard extends Wizard implements IImportWizard {

	private JBangImportWizardPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		ImageDescriptor imgDescriptor = ResourceLocator.imageDescriptorFromBundle(Activator.PLUGIN_ID, "icons/jbang.png").get();
		setDefaultPageImageDescriptor(imgDescriptor);
	}

	@Override
	public boolean performFinish() {
		if (!page.isPageComplete()) {
			return false;
		}
		Collection<Path> scripts = page.getScripts();
		ImportJBangScriptsJob job = new ImportJBangScriptsJob(scripts.toArray(new Path[scripts.size()]));
		job.schedule();
		return true;

	}

	@Override
	public void addPages() {
		page = new JBangImportWizardPage();
		addPage(page);
	}

}
