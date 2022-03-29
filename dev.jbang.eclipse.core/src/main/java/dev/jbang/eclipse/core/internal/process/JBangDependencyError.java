package dev.jbang.eclipse.core.internal.process;

public class JBangDependencyError extends JBangError {

	private String dependency;

	public JBangDependencyError(String dependency) {
		super("Could not resolve dependency " + dependency);
		this.setDependency(dependency);
	}

	public String getDependency() {
		return dependency;
	}

	public void setDependency(String dependency) {
		this.dependency = dependency;
	}

}
