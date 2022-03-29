package dev.jbang.eclipse.ui.internal.preferences;
import java.util.Set;

import org.eclipse.jface.wizard.Wizard;

import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;


public class JBangInstallationWizard extends Wizard {

  private final JBangInstallationWizardPage runtimePage;

  private JBangRuntime result;

  public JBangInstallationWizard(Set<String> names) {
    this.runtimePage = new JBangInstallationWizardPage(null, names);
    setWindowTitle("New JBang installation");
  }

  public JBangInstallationWizard(JBangRuntime original, Set<String> names) {
    this.runtimePage = new JBangInstallationWizardPage(original, names);
    setWindowTitle("Edit JBang installation");
  }

  @Override
  public void addPages() {
    addPage(runtimePage);
  }

  @Override
  public boolean performFinish() {
    result = runtimePage.getResult();
    return true;
  }

  public JBangRuntime getResult() {
    return result;
  }

}