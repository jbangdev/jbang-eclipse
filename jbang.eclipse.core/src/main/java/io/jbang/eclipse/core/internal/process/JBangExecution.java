package io.jbang.eclipse.core.internal.process;

import static io.jbang.eclipse.core.internal.JBangFileUtils.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.launching.JavaRuntime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.jbang.eclipse.core.internal.runtine.JBangRuntime;

public class JBangExecution {

	private JBangRuntime jbang;
	private String file;

	private static final Pattern RESOLUTION_ERROR = Pattern
			.compile("Resolving (.*)\\.\\.\\.\\[ERROR\\] Could not resolve dependency");

	public JBangExecution(JBangRuntime jbang, File file) {
		this.jbang = jbang;
		this.file = file.toString();
	}

	public JBangInfo getInfo() {
		List<JBangError> resolutionErrors = new ArrayList<>();
		JBangInfo result = new JBangInfo();
		result.setResolutionErrors(resolutionErrors);
		result.setBackingResource(file);
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(jbang.getExecutable().toOSString(), "info", "tools",
					file);
			Map<String, String> env = processBuilder.environment();
			if (!env.containsKey("JAVA_HOME")) {
				String javaHome = System.getProperty("user.home") + "/.sdkman/candidates/java/current";				
				var defaultVM = JavaRuntime.getDefaultVMInstall();
				if (defaultVM != null) {
					var defaultJavaHome = defaultVM.getInstallLocation();
					if (defaultJavaHome != null) {
						javaHome = defaultJavaHome.getAbsoluteFile().toString();
					}
				}
				env.put("JAVA_HOME", javaHome);
			}
			// env.put("JAVA_HOME", System.getenv("JAVA_HOME"));
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			StringBuilder processOutput = new StringBuilder();

			try (BufferedReader processOutputReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));) {
				String readLine;
				while ((readLine = processOutputReader.readLine()) != null) {
					if (readLine.contains("[ERROR]")) {
						resolutionErrors.add(sanitizeError(readLine));
					} else if (!readLine.startsWith("[jbang]")) {
						processOutput.append(readLine + System.lineSeparator());
					}
				}

				process.waitFor();
			}
			String output = processOutput.toString().trim();
			if (resolutionErrors.isEmpty() && output.isBlank()) {
				resolutionErrors.add(new JBangError("Failed to get JBang informations"));
			}
			if (!output.isBlank() && !output.startsWith("{")) {
				resolutionErrors.add(new JBangError(output));
			}

			if (!resolutionErrors.isEmpty()) {
				return result;
			}

			if (!output.isBlank()) {
				Gson gson = new GsonBuilder().create();
				result = gson.fromJson(output, JBangInfo.class);
				result.setBackingResource(file);
				scanForAdditionalInfos(result);
			}
		} catch (IOException | InterruptedException e) {
			resolutionErrors.add(new JBangError("Failed to execute JBang:" + e.getMessage()));
		}
		return result;
	}

	private void scanForAdditionalInfos(JBangInfo info) {
		try (BufferedReader reader = new BufferedReader(new FileReader(info.getBackingResource()))) {
			String line;
			List<String> sources = new ArrayList<>();
			java.nio.file.Path baseDir = Paths.get(info.getBackingResource()).getParent();

			while ((line = reader.readLine()) != null) {
				if (!line.isBlank() && !isJBangInstruction(line)) {
					break;
				}
				if (info.getJavaVersion() == null) {
					var javaVersion = getJavaVersion(line);
					if (javaVersion != null) {
						info.setJavaVersion(javaVersion);
					}
				}
				if (info.getSources() == null) {
					String source = getSource(line);
					if (source != null) {
						String sourcePath = baseDir.resolve(source).toString();
						sources.add(sourcePath);
					}
				}
			}
			if (!sources.isEmpty()) {
				info.setSources(sources);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private JBangError sanitizeError(String errorLine) {
		// [jbang] Resolving eu.hansolo:tilesfx:1.3.4...[jbang] [ERROR] Could not
		// resolve dependency
		String error = errorLine.replaceAll("\\[jbang\\] ", "");
		Matcher matcher = RESOLUTION_ERROR.matcher(error);
		if (matcher.find()) {
			String dependency = matcher.group(1);
			return new JBangDependencyError(dependency);
		}
		return new JBangError(error);
	}

	public static void main(String[] args) throws Exception {
		var jbang = new JBangRuntime("~/.sdkman/candidates/jbang/current/");
		var script = new File("/Users/fbricon/Dev/souk/javafx.java");
		var execution = new JBangExecution(jbang, script);
		// System.err.println(execution.getInfo());

		script = new File("/Users/fbricon/Dev/workspaces/runtime-jbang/jbanged/src/jfxtiles.java");
		execution = new JBangExecution(jbang, script);
		System.err.println(execution.getInfo());

	}
}
