package dev.jbang.eclipse.core.internal;

import static dev.jbang.eclipse.core.internal.utils.ClasspathHelpers.assertGenerateParameters;
import static dev.jbang.eclipse.core.internal.utils.ClasspathHelpers.assertJava;
import static dev.jbang.eclipse.core.internal.utils.JBangVersionHelper.isHigherOrEqual;
import static dev.jbang.eclipse.core.internal.utils.JobHelpers.waitForJobsToComplete;
import static dev.jbang.eclipse.core.internal.utils.WorkspaceHelpers.assertErrorMarker;
import static dev.jbang.eclipse.core.internal.utils.WorkspaceHelpers.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.jbang.eclipse.core.internal.project.JBangProject;
import dev.jbang.eclipse.core.internal.utils.ImportScriptUtils;

public class JBangBuilderTest extends AbstractJBangTest {

	private JBangProject jbp;

	@BeforeEach
	public void importScript() throws Exception {
		jbp = ImportScriptUtils.importJBangScript("hello.java");
		assertNotNull(jbp);
		assertEquals("hello.java", jbp.getProject().getName());
		waitForJobsToComplete();
		assertNoErrors(jbp.getProject());
	}

	@Test
	public void enableParameters() throws Exception {
		if (isHigherOrEqual(getDefaultRuntime().getVersion(), "0.122.0")) {
			return;
		}
		IProject project = jbp.getProject();
		assertGenerateParameters(project, false);

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String parametersOptions = "//JAVAC_OPTIONS -parameters\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+parametersOptions));

		waitForJobsToComplete();
		assertNoErrors(project);
		assertGenerateParameters(project, true);
	}

	@Test
	public void changeJava() throws Exception {
		IProject project = jbp.getProject();
		assertJava(project, "11");

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 17"));

		waitForJobsToComplete();
		assertNoErrors(project);
		assertJava(project, "17");
	}

	@Test
	public void invalidJava() throws Exception {
		IProject project = jbp.getProject();
		assertJava(project, "11");

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA xxx"));

		waitForJobsToComplete();
		assertErrorMarker(JBangConstants.MARKER_RESOLUTION_ID, "Invalid JAVA version, should be a number optionally followed by a plus sign", 3, "src/hello.java", project);
	}
	
	@Test
	public void invalidSource() throws Exception {
		IProject project = jbp.getProject();
		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String sources = "//SOURCES missing.java\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+sources));

		waitForJobsToComplete();
		assertErrorMarker(JBangConstants.MARKER_RESOLUTION_ID, "Could not find missing.java", 4, "src/hello.java", project);
	}
	
	@Test
	public void invalidFile() throws Exception {
		IProject project = jbp.getProject();
		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String files = "//FILES foo.java=missing.java\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+files));

		waitForJobsToComplete();
		assertErrorMarker(JBangConstants.MARKER_RESOLUTION_ID, "Could not find 'missing.java'", 4, "src/hello.java", project);
	}

	@Test
	public void setReleaseLevel() throws Exception {
		IProject project = jbp.getProject();
		assertGenerateParameters(project, true);

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String compileOptions = "//COMPILE_OPTIONS --release 8\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+compileOptions));

		waitForJobsToComplete();
		assertNoErrors(project);
		assertJava(project, "1.8");
	}

	@Test
	public void setSourceLevel() throws Exception {
		IProject project = jbp.getProject();
		assertGenerateParameters(project, true);

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String compileOptions = "//JAVAC_OPTIONS -source 8\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+compileOptions));

		waitForJobsToComplete();
		assertNoErrors(project);
		assertJava(project, "1.8");
	}
	
	@Test
	public void invalidModule() throws Exception {
		IProject project = jbp.getProject();
		assertGenerateParameters(project, true);

		IFile script = jbp.getMainSourceFile();
		String content = ResourceUtil.getContent(script);
		String badModule = "//MODULE bad module\n";
		ResourceUtil.setContent(script, content.replace("//JAVA 11", "//JAVA 11\n"+badModule));

		waitForJobsToComplete();
		assertErrorMarker(JBangConstants.MARKER_RESOLUTION_ID, "//MODULE line has wrong format, should be '//MODULE [identifier]'", 4, "src/hello.java", project);
		
		ResourceUtil.setContent(script, content.replace("bad module", "goodmodule"));
		waitForJobsToComplete();
		assertNoErrors(project);
	}
}
