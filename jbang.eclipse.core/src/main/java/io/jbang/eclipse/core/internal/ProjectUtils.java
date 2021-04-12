package io.jbang.eclipse.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class ProjectUtils {
	
	private ProjectUtils() {
		// no public instantiation
	}
	
	public static boolean isJBangProject(IProject project) {
		return hasNature(project, JBangConstants.NATURE_ID);
	}

	public static boolean hasNature(IProject project, String natureId) {
		try {
			return project != null && project.isAccessible() && project.hasNature(natureId);
		} catch (CoreException e) {
			return false;
		}
	}
}
