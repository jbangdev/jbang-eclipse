package dev.jbang.eclipse.core.internal.utils;

import static dev.jbang.eclipse.core.internal.ProjectUtils.isJavaProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ClasspathHelpers {

	public static void assertJava(IProject project, String javaVersion) {
		assertTrue(isJavaProject(project), project.getName() +" is not a Java project");
		assertEquals(javaVersion, getJavaSourceLevel(project));
	}

	public static void assertGenerateParameters(IProject project, boolean enabled) {
		assertTrue(isJavaProject(project), project.getName() +" is not a Java project");
		var options = getJavaOptions(project);
		if (options == null || options.isEmpty()) {
			assertEquals(enabled, false, "Unexpected -parameters setting");
		} else {
			String genParams = options.get(JavaCore.COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR);
			assertEquals(enabled?JavaCore.GENERATE:JavaCore.DO_NOT_GENERATE, genParams, "Unexpected -parameters setting");
		}
	}

	public static String getJavaSourceLevel(IProject project) {
		Map<String, String> options = getJavaOptions(project);
		return options == null ? null : options.get(JavaCore.COMPILER_SOURCE);
	}

	public static Map<String, String> getJavaOptions(IProject project) {
		if (!isJavaProject(project)) {
			return null;
		}
		IJavaProject javaProject = JavaCore.create(project);
		return javaProject.getOptions(true);
	}

}
