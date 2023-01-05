package dev.jbang.eclipse.core.internal.expressions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.jupiter.api.Test;

public class JBangResourceTesterTest {

	JBangResourceTester tester = new JBangResourceTester();
	
	IProgressMonitor monitor = new NullProgressMonitor();

	@Test
	public void testJBangResource() throws CoreException {

		assertIsJBang(getPath("scripts", "hello.java"));
		
		assertIsJBang(getPath("scripts", "foo", "build.jbang"));
		
		assertIsJBang(getPath("scripts", "others", "hello.jsh"));
		
		assertIsJBang(getPath("scripts", "others", "hello.groovy"));
		
		assertIsJBang(getPath("scripts", "others", "hello.kt"));
		
		assertIsNotJBang(getPath("pom.xml"));
		
		assertIsNotJBang(null);
		
		assertFalse(tester.test(getProject(null), null, null, true));

	}
	
	void assertIsJBang(IPath resourcePath) throws CoreException {
		IResource candidate = getResource(resourcePath, monitor);
		assertTrue(tester.test(candidate, null, null, true), ((candidate == null)?"null": candidate.getName()) + " should be a JBang file");
	}
	
	void assertIsNotJBang(IPath resourcePath) throws CoreException {
		IResource candidate = getResource(resourcePath, monitor);
		assertFalse(tester.test(candidate, null, null, true), ((candidate == null)?"null": candidate.getName()) + " should not be a JBang file");
	}

	private IPath getPath(String first, String... more) {
		var p = Paths.get(first, more).toAbsolutePath();
		return new Path(p.toString());
	}

	private IProject getProject(IProgressMonitor monitor) throws CoreException {
		String name = "tester-project";
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.exists()) {
			IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(name);
			project.create(description, monitor);
			project.open(monitor);
			project.setDescription(description, monitor);
		}
		return project;
	}

	private IFile getResource(IPath absPath, IProgressMonitor monitor) throws CoreException {
		if (absPath == null) {
			return null;
		}
		var fileName = absPath.segment(absPath.segmentCount() -1);
		var project = getProject(monitor);
		
		IFile linkedFile = project.getFile(fileName);
		if (linkedFile.exists()) {
			return linkedFile;
		}
		linkedFile.createLink(absPath.toFile().toURI(), IResource.REPLACE, monitor);
		return linkedFile;
	}
}
