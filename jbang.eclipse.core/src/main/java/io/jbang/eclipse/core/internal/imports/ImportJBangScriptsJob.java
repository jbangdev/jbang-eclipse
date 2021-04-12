package io.jbang.eclipse.core.internal.imports;

import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import io.jbang.eclipse.core.JBangCorePlugin;


public class ImportJBangScriptsJob extends Job {

	private Path[] scripts;

	public ImportJBangScriptsJob(Path[] scripts) {
		super("Import JBang scripts");
		// TODO Auto-generated constructor stub
		this.scripts = scripts;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (scripts == null || scripts.length == 0) {
			return Status.OK_STATUS;
		}
		var projectManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		Path script = scripts[0];
		try {
			if (isJBangScript(script)) {
				projectManager.createJBangProject(script, monitor);
			}
		} catch (Exception e) {
			return toStatus("Error configuring JBang Script", e);
		}
		return Status.OK_STATUS;
	}

	private boolean isJBangScript(Path script) {
		return true;
	}

	private IStatus toStatus(String msg, Exception e) {
		return new Status(IStatus.ERROR, JBangCorePlugin.PLUGIN_ID, msg, e);
	}

}
