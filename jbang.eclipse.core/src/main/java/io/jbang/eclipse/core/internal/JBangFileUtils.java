package io.jbang.eclipse.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class JBangFileUtils {

	private static Pattern JBANG_HEADERS = Pattern.compile("(//.*jbang.*)|(//[A-Z:]+ ).*");

	private static Pattern JAVA_INSTRUCTION = Pattern.compile("//JAVA (\\S*).*");

	private static Pattern SOURCES_INSTRUCTION = Pattern.compile("//SOURCES (\\S*).*");
	
	private static Pattern FILES_INSTRUCTION = Pattern.compile("//FILES (\\S*).*");

	private JBangFileUtils() {
	}

	public static boolean isJBangFile(IResource resource) {
		if (!(resource instanceof IFile)
				|| (!"java".equals(resource.getFileExtension()) && !"jsh".equals(resource.getFileExtension()))) {
			return false;
		}
		IFile file = (IFile) resource;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()))) {
			String firstLine = reader.readLine().toString();
			return isJBangInstruction(firstLine);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean isJBangFile(Path file) {
		if (!(Files.isRegularFile(file))) {
			return false;
		}
		String fileName = file.getFileName().toString().toLowerCase();
		if (!fileName.endsWith(".java") && !fileName.endsWith(".jsh")) {
			return false;
		}
		String firstLine;
		try {
			firstLine = Files.lines(file).filter(line -> !line.isBlank()).findFirst().get();
			return isJBangInstruction(firstLine);
		} catch (IOException e) {
		}
		return false;
	}

	public static boolean isJBangInstruction(String line) {
		return JBANG_HEADERS.matcher(line).matches();
	}

	public static String getJavaVersion(String line) {
		return getMatch(JAVA_INSTRUCTION, line);
	}

	public static String getSource(String line) {
		return getMatch(SOURCES_INSTRUCTION, line);
	}

	public static String[] getFile(String line) {
		String file = getMatch(FILES_INSTRUCTION, line);
		String[] tuple = null;
		if (file != null) {
			tuple = file.split("=");
			if (tuple.length == 1) {
				return new String[] {file, file};
			}
		}
		return tuple;
	}
	
	private static String getMatch(Pattern pattern, String line) {
		var matcher = pattern.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

}
