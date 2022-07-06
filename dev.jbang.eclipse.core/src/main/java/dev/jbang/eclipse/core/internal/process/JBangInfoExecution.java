package dev.jbang.eclipse.core.internal.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;

public class JBangInfoExecution {

	private JBangRuntime jbang;
	private String file;
	private String javaHome;

	private static final Pattern RESOLUTION_ERROR_1 = Pattern.compile("Resolving (.*)\\.\\.\\.\\[ERROR\\] Could not resolve dependency");
	private static final Pattern RESOLUTION_ERROR_2 = Pattern.compile("\\[ERROR\\] Could not resolve dependency (.*)");
	private static final Pattern RESOLUTION_ERROR_3 = Pattern.compile(".* Could not find artifact (.*) in ");
	private static final Pattern RESOLUTION_ERROR_4 = Pattern.compile(".*The following artifacts could not be resolved: (.*?): Could");
	
	
	public JBangInfoExecution(JBangRuntime jbang, File file, String javaHome) {
		this.jbang = jbang;
		this.javaHome = javaHome;
		this.file = file.toString();
	}

	public JBangInfoResult getInfo(IProgressMonitor monitor) {
		Set<JBangError> resolutionErrors = new LinkedHashSet<>();
		JBangInfoResult result = new JBangInfoResult();
		result.setResolutionErrors(resolutionErrors);
		result.setBackingResource(file);
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(jbang.getExecutable().toOSString(), "--quiet", "info", "tools", file);
			var env = processBuilder.environment();
			env.put("NO_COLOR", "true");
			var processJavaHome = env.get("JAVA_HOME");
			if (processJavaHome == null || processJavaHome.isBlank()) {
				if (javaHome == null || javaHome.isBlank()) {
					javaHome = System.getProperty("java.home");
				}
				var envPath = env.get("PATH");
				if (javaHome != null) {
					env.put("JAVA_HOME", javaHome);
					envPath =  envPath +File.pathSeparator+javaHome+ (javaHome.endsWith(File.separator)?"bin":File.separator +"bin");
				}
				env.put("PATH", envPath +File.pathSeparator+javaHome+"bin");
			}
			
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			StringBuilder processOutput = new StringBuilder();

			try (BufferedReader processOutputReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));) {
				String readLine;
				while ((readLine = processOutputReader.readLine()) != null) {
					if(readLine.startsWith("Resolving")) { //from stderr
						monitor.setTaskName(readLine);
					}
					System.err.println(readLine);
					if (readLine.contains("[ERROR]") || readLine.contains(" not ")) { //from stderr
						resolutionErrors.addAll(sanitizeError(readLine));
					} else if (!readLine.startsWith("[jbang]") && !readLine.startsWith("Done" )) {
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

			Gson gson = new GsonBuilder().create();
			result = gson.fromJson(output, JBangInfoResult.class);
		} catch (IOException | InterruptedException e) {
			resolutionErrors.add(new JBangError("Failed to execute JBang:" + e.getMessage()));
		}
		return result;
	}
	
	public static Set<JBangError> sanitizeError(String errorLine) {
		// [jbang] Resolving eu.hansolo:tilesfx:1.3.4...[jbang] [ERROR] Could not
		// resolve dependency
		
		String error = errorLine.replace("\\[jbang\\] ", "");
		
		//The following artifacts could not be resolved: com.pulumi:gcp:jar:6.11.0, com.pulumi:kubernetes:jar:3.15.1: 
		//Could not find artifact com.pulumi:gcp:jar:6.11.0 in 

		Matcher matcher = RESOLUTION_ERROR_4.matcher(error);
		if (matcher.find()) {
			var dependencies = matcher.group(1).split(", ");
			return Stream.of(dependencies).map(d -> new JBangDependencyError(simplify(d))).collect(Collectors.toSet()); 
		}
		
		matcher = RESOLUTION_ERROR_3.matcher(error);
		String dependency = null;
		if (matcher.find()) {
			dependency = simplify(matcher.group(1));
		} else {
			//[ERROR] Could not resolve dependency info.picocli:picocli:4.4965.0
			matcher = RESOLUTION_ERROR_2.matcher(error);
			if (matcher.find()) {
				dependency = matcher.group(1);
			} else {
				matcher = RESOLUTION_ERROR_1.matcher(error);
				if (matcher.find()) {
					dependency = matcher.group(1);
				}
			}			
		}
		
		
		var jberror = (dependency == null)? new JBangError(error): new JBangDependencyError(dependency);
		return Collections.singleton(jberror);
	}

	
	private static String simplify(String dependency) {
		return dependency.replace(":jar:", ":");
	}
	
}
