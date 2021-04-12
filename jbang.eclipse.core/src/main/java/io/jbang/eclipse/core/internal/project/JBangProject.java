package io.jbang.eclipse.core.internal.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import io.jbang.eclipse.core.internal.runtine.JBangRuntime;

public class JBangProject {

	private IProject project;

	private IFile mainSourceFile;

	private JBangRuntime runtime;

	public JBangProject(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	public IFile getMainSourceFile() {
		return mainSourceFile;
	}

	public void setMainSourceFile(IFile mainSourceFile) {
		this.mainSourceFile = mainSourceFile;
	}

	public JBangRuntime getRuntime() {
		return runtime;
	}

	public void setRuntime(JBangRuntime runtime) {
		this.runtime = runtime;
	}

}
