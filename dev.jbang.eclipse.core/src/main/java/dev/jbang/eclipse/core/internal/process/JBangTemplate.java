package dev.jbang.eclipse.core.internal.process;

import java.util.Objects;

public class JBangTemplate {

	private String id;

	private String label;

	public JBangTemplate(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "JBangTemplate [id=" + id + ", label=" + label + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, label);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		JBangTemplate other = (JBangTemplate) obj;
		return Objects.equals(id, other.id) && Objects.equals(label, other.label);
	}
}
