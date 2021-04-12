package io.jbang.eclipse.core.internal.process;

public class JBangError {
	
	private String message;
	
	public JBangError(String error) {
		message= error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
