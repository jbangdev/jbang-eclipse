package dev.jbang.eclipse.core.internal.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangConstants;
import dev.jbang.eclipse.core.internal.ProjectUtils;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;

public class JBangProject {

	private static String P_MAIN_SCRIPT = "mainScriptFile";

	private IProject project;

	private JBangRuntime runtime;

	public JBangProject(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	public IFile getMainSourceFile() {
		var prefs = getEclipsePreferences();
		if (prefs != null) {
			String path = prefs.get(P_MAIN_SCRIPT, null);
			IProject jbp = getProject();
			if (path == null || jbp == null) {
				return null;
			}
			IFile maybeScript = jbp.getFile(new Path(path));
			if (maybeScript.exists()) {
				return maybeScript;
			}
		}
		return null;
	}

	public void setMainSourceFile(IFile mainSourceFile) {
		var prefs = getEclipsePreferences();
		if (prefs != null) {
			if (mainSourceFile != null) {
				prefs.put(P_MAIN_SCRIPT, mainSourceFile.getProjectRelativePath().toPortableString());
			} else {
				prefs.remove(P_MAIN_SCRIPT);
			}
			savePreferences(prefs);
		}
	}

	public JBangRuntime getRuntime() {
		return runtime;
	}

	public void setRuntime(JBangRuntime runtime) {
		this.runtime = runtime;
	}

	private IEclipsePreferences getEclipsePreferences() {
		if (!ProjectUtils.isJBangProject(getProject())) {
			return null;
		}
		IScopeContext context = new ProjectScope(getProject());
		final IEclipsePreferences eclipsePreferences = context.getNode(JBangConstants.PLUGIN_ID);
		return eclipsePreferences;
	}
	
	private void savePreferences(IEclipsePreferences prefs) {
		try {
			prefs.flush();
		} catch (BackingStoreException ex) {
			JBangCorePlugin.log("Failed to save preferences for "+getProject(), ex);
		}
	}

}
