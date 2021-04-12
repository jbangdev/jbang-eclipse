package io.jbang.eclipse.core.internal.runtine;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class JBangRuntime {

	private static final IPath EXECUTABLE;

	static {
		if (System.getProperty("os.name", "").toLowerCase().indexOf("win") > -1) {
			EXECUTABLE = new Path("jbang.cmd");
		} else {
			EXECUTABLE = new Path("jbang");
		}
	}

	private IPath path;

	public JBangRuntime() {
	}

	public JBangRuntime(String path) {
		if (path != null && !path.isBlank()) {
			if (path.startsWith("~")) {
				path = System.getProperty("user.home") + path.substring(1);
			}
			this.path = new Path(path);
		}
	}

	public JBangRuntime(IPath path) {
		this.path = path;
	}

	public IPath getPath() {
		return path;
	}

	public IPath getExecutable() {
		return path == null ? EXECUTABLE : path.append("bin").append(EXECUTABLE);
	}

	@Override
	public String toString() {
		return getExecutable().toOSString();
	}

}
