package dev.jbang.eclipse.core.internal.process;

import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(dependency);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JBangDependencyError other = (JBangDependencyError) obj;
		return Objects.equals(dependency, other.dependency);
	}
}
