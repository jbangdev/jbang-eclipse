package dev.jbang.eclipse.core.internal.process;

public class JBangMissingResource extends JBangError {

	private String resource;

	public JBangMissingResource(String message, String resource, ErrorKind kind) {
		super(message, kind);
		this.resource = resource;
	}

	public String getResource() {
		return resource;
	}

}
