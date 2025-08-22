package dev.jbang.eclipse.core.internal;

import static dev.jbang.eclipse.core.internal.utils.WorkspaceHelpers.cleanWorkspace;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimesDiscoveryJob;

public abstract class AbstractJBangTest {

	@BeforeAll
	public static void beforeAll() throws IOException {
		setupJBang();
	}

	@BeforeEach
	public void cleanUp() throws Exception {
		deleteWorkDir();
		cleanWorkspace();
	}
	
	protected JBangRuntime getDefaultRuntime() {
		return JBangCorePlugin.getJBangManager().getJBangRuntimeManager().getDefaultRuntime();
	}

	private static void deleteWorkDir() throws IOException {
		 var workDir = Paths.get("target", "workdir");
		 FileUtils.deleteDirectory(workDir.toFile());
	}

	public static JBangRuntimeManager setupJBang() throws IOException {
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:target/jbang*");
		Path root = Paths.get("");
	    var jbangInstall = Files.walk(root)
	        .filter(path -> pathMatcher.matches(path) && Files.isDirectory(path))
	        .findFirst();
	    if (jbangInstall.isEmpty()) {
	    	Assertions.fail("No target/jbang runtime detected. Please run 'mvn validate' from a terminal");
	    }
	    var defaultRuntime = new JBangRuntime("tests", jbangInstall.get().toAbsolutePath().toString());
	    defaultRuntime.detectVersion(null);
	    var jbangRuntimeManager = JBangCorePlugin.getJBangManager().getJBangRuntimeManager();
	    try {
	    	Job.getJobManager().cancel(JBangRuntimesDiscoveryJob.class);
			Job.getJobManager().join(JBangRuntimesDiscoveryJob.class, new NullProgressMonitor());
		} catch (Exception e) {
			//ignore
		}
	    //Ignore runtimes discovered on startup
	    jbangRuntimeManager.reset();
	    jbangRuntimeManager.setRuntimes(List.of(defaultRuntime));
	    jbangRuntimeManager.setDefaultRuntime(defaultRuntime);
	    return jbangRuntimeManager;
	}

}
