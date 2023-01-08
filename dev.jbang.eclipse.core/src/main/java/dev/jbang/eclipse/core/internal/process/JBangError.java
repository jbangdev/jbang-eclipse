package dev.jbang.eclipse.core.internal.process;

public class JBangError {

	private String message;
	
	private ErrorKind kind;

	public JBangError(String error) {
		this(error, ErrorKind.UnknownError);
	}
	
	public JBangError(String error, ErrorKind kind) {
		message = error;
		this.kind = kind;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ErrorKind getKind() {
		return kind;
	}

}
