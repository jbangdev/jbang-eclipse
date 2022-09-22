package dev.jbang.eclipse.core.internal.process;

import static dev.jbang.eclipse.core.internal.ExceptionFactory.newException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;

public class JBangTemplatesExecution {

	private JBangRuntime jbang;
	private String javaHome;

	public JBangTemplatesExecution(JBangRuntime jbang, String javaHome) {
		this.jbang = jbang;
		this.javaHome = javaHome;
	}

	public List<JBangTemplate> getTemplates(IProgressMonitor monitor) throws CoreException {
		List<JBangTemplate> templates = new ArrayList<>();
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(jbang.getExecutable().toOSString(), "template", "list");
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
			
			Process process = processBuilder.start();

			try (BufferedReader processOutputReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));) {
				String readLine;
				while ((readLine = processOutputReader.readLine()) != null) {
					System.err.println(readLine);
					JBangTemplate template = parseTemplate(readLine);
					if (template != null) {
						templates.add(template);
					}
				}
				process.waitFor();
			}
		} catch (IOException | InterruptedException e) {
			throw newException( "Failed to load JBang templates", e);
		}
		return templates;
	}

	
	public static JBangTemplate parseTemplate(String line) {
		if (line.isBlank() || line.indexOf('=') < 0) {
			return null;
		}
		String[] parts = line.split("=");
		return new JBangTemplate(parts[0], parts[1]);
	}
	
	public static void main(String[] args) throws CoreException {
		var jbang = new JBangRuntime("/Users/fbricon/.sdkman/candidates/jbang/current/");
		var exec = new JBangTemplatesExecution(jbang, null);
		exec.getTemplates(new NullProgressMonitor()).forEach(System.out::println);
		System.out.println("Done");
	}
}
