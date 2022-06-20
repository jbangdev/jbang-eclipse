package dev.jbang.eclipse.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

public class ProjectUtils {

	private ProjectUtils() {
		// no public instantiation
	}

	public static boolean isJBangProject(IProject project) {
		return hasNature(project, JBangConstants.NATURE_ID);
	}
	

	public static boolean isJavaProject(IProject project) {
		return hasNature(project, JavaCore.NATURE_ID);
	}

	public static boolean hasNature(IProject project, String natureId) {
		try {
			return project != null && project.isAccessible() && project.hasNature(natureId);
		} catch (CoreException e) {
			return false;
		}
	}
	

	public static boolean addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
		boolean addedNature = false;
		if (!project.hasNature(natureId)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
			newNatures[0] = natureId;
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
			addedNature = true;
		}
		return addedNature;
	}
	
	public static boolean addJBangNature(IProject project, IProgressMonitor monitor) throws CoreException {
		return addNature(project, JBangConstants.NATURE_ID, monitor);
	}
	
	public static boolean addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
		return addNature(project, JavaCore.NATURE_ID, monitor);
	}
}
