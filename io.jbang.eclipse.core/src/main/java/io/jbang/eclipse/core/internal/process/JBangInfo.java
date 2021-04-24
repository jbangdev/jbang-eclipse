package io.jbang.eclipse.core.internal.process;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JBangInfo {

	private String backingResource;

	private List<String> resolvedDependencies;

	private List<String> sources;
	
	private Map<String,String> files;

	private List<JBangError> resolutionErrors;

	private String javaVersion;

	public List<String> getResolvedDependencies() {
		return resolvedDependencies;
	}

	public void setResolvedDependencies(List<String> resolvedDependencies) {
		this.resolvedDependencies = resolvedDependencies;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("backingResource: ");
		sb.append(backingResource);
		if (sources != null && !sources.isEmpty()) {
			sources.forEach(source -> {
				sb.append(System.lineSeparator()).append("additional source: ").append(source);
			});
		}
		if (files != null && !files.isEmpty()) {
			files.forEach((link, file) -> {
				sb.append(System.lineSeparator()).append("additional file: ").append(link).append("[").append(file).append("]");
			});
		}
		if (resolvedDependencies != null && !resolvedDependencies.isEmpty()) {
			sb.append(System.lineSeparator()).append("resolvedDependencies: [");
			sb.append(String.join("," + System.lineSeparator(), resolvedDependencies));
			sb.append("]");
		}
		if (javaVersion != null && !javaVersion.isBlank()) {
			sb.append(System.lineSeparator()).append("javaVersion: " + javaVersion);
		}

		if (hasErrors()) {
			sb.append(System.lineSeparator()).append("resolutionErrors: [");
			sb.append(getResolutionErrors().stream().map(Object::toString).collect(Collectors.joining(", ")));
			sb.append("]");
		}
		return sb.toString();
	}

	public String getBackingResource() {
		return backingResource;
	}

	public void setBackingResource(String backingResource) {
		this.backingResource = backingResource;
	}

	public List<JBangError> getResolutionErrors() {
		return resolutionErrors;
	}

	public void setResolutionErrors(List<JBangError> resolutionErrors) {
		this.resolutionErrors = resolutionErrors;
	}

	public boolean hasErrors() {
		return resolutionErrors != null && !resolutionErrors.isEmpty();
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public String getTargetRuntime() {
		if (javaVersion == null || javaVersion.isBlank()) {
			return null;
		}
		String version = javaVersion.endsWith("+") ? javaVersion.substring(0, javaVersion.length() - 1) : javaVersion;
		try {
			int v = Integer.parseInt(version);
			if (v < 9) {
				version = "1." + v;
			}
			return "JavaSE-" + version;
		} catch (NumberFormatException e) {
		}
		return null;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	public List<String> getSources() {
		return sources;
	}

	public void setSources(List<String> sources) {
		this.sources = sources;
	}

	public Map<String, String> getFiles() {
		return files;
	}

	public void setFiles(Map<String, String> files) {
		this.files = files;
	}

}
