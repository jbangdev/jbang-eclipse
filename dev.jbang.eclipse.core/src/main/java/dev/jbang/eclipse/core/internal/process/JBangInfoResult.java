package dev.jbang.eclipse.core.internal.process;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JBangInfoResult {

	public class JBangFile {
		public String originalResource;
		public String backingResource;
		public String target;
	}
	
	private String backingResource;

	private List<String> resolvedDependencies;

	private List<JBangFile> sources;
	
	private List<JBangFile> files;

	private Collection<JBangError> resolutionErrors;

	private String requestedJavaVersion;
	
	private List<String> compileOptions;
	
	private List<String> runtimeOptions;
	
	private String availableJdkPath;

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
			files.forEach(file -> {
				sb.append(System.lineSeparator()).append("additional file: ").append(file.target).append("[").append(file.originalResource).append("]");
			});
		}
		if (resolvedDependencies != null && !resolvedDependencies.isEmpty()) {
			sb.append(System.lineSeparator()).append("resolvedDependencies: [");
			sb.append(String.join("," + System.lineSeparator(), resolvedDependencies));
			sb.append("]");
		}
		if (requestedJavaVersion != null && !requestedJavaVersion.isBlank()) {
			sb.append(System.lineSeparator()).append("requestedJavaVersion: ").append(requestedJavaVersion);
		}
		if (availableJdkPath != null && !availableJdkPath.isBlank()) {
			sb.append(System.lineSeparator()).append("availableJdkPath: ").append(availableJdkPath);
		}
		if (compileOptions != null && !compileOptions.isEmpty()) {
			sb.append(System.lineSeparator()).append("compileOptions: " + compileOptions);
		}
		if (runtimeOptions != null && !runtimeOptions.isEmpty()) {
			sb.append(System.lineSeparator()).append("runtimeOptions: " + runtimeOptions);
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

	public Collection<JBangError> getResolutionErrors() {
		return resolutionErrors;
	}

	public void setResolutionErrors(Collection<JBangError> resolutionErrors) {
		this.resolutionErrors = resolutionErrors;
	}

	public boolean hasErrors() {
		return resolutionErrors != null && !resolutionErrors.isEmpty();
	}

	public String getRequestedJavaVersion() {
		return requestedJavaVersion;
	}

	public String getTargetRuntime() {
		if (requestedJavaVersion == null || requestedJavaVersion.isBlank()) {
			return null;
		}
		String version = requestedJavaVersion.endsWith("+") ? requestedJavaVersion.substring(0, requestedJavaVersion.length() - 1) : requestedJavaVersion;
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

	public void setRequestedJavaVersion(String javaVersion) {
		this.requestedJavaVersion = javaVersion;
	}

	public List<JBangFile> getSources() {
		return sources;
	}

	public void setSources(List<JBangFile> sources) {
		this.sources = sources;
	}

	public List<JBangFile> getFiles() {
		return files;
	}

	public void setFiles(List<JBangFile> files) {
		this.files = files;
	}

	public class Resource {
		public String originalResource;
		public String backingResource;
	}

	public class File extends Resource {
		public String target;
	}
	
	public class Source extends Resource {
		public List<Source> sources;
	}

	public List<String> getCompileOptions() {
		return compileOptions;
	}

	public void setCompileOptions(List<String> compileOptions) {
		this.compileOptions = compileOptions;
	}

	public List<String> getRuntimeOptions() {
		return runtimeOptions;
	}

	public void setRuntimeOptions(List<String> runtimeOptions) {
		this.runtimeOptions = runtimeOptions;
	}

	public String getAvailableJdkPath() {
		return availableJdkPath;
	}

	public void setAvailableJdkPath(String availableJdkPath) {
		this.availableJdkPath = availableJdkPath;
	}
	
	
}
