package dev.jbang.eclipse.core.internal.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.NullProgressMonitor;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.project.JBangProject;
import dev.jbang.eclipse.core.internal.project.JBangProjectConfiguration;

public class ImportScriptUtils {

	private static Path scriptsDir = Paths.get("scripts");
	
	private ImportScriptUtils() {}
	
	public static JBangProject importJBangScript(String... relativePaths) throws Exception {
		Path mainScript = null;
		
		var workDir = Paths.get("target", "workdir");
		Files.createDirectories(workDir);
		
		for (String relativePath : relativePaths) {
			var scriptPath = scriptsDir.resolve(relativePath);
			if (!Files.exists(scriptPath)) {
				throw new IOException(scriptPath + " does not exist");
			}
		
			var destPath = copy(workDir, scriptPath);
			if (mainScript == null) {
				mainScript = destPath;
			}
		}
		
		var projectManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		
		var jbangProject = projectManager.createJBangProject(mainScript, new JBangProjectConfiguration() , new NullProgressMonitor());
		
		return jbangProject;
	}

	private static Path copy(Path destFolder, Path scriptPath) throws IOException {
		
		var destPath = destFolder.resolve(scriptsDir.relativize(scriptPath));
		
		Files.deleteIfExists(destPath);
		
		Files.copy(scriptPath, destPath);
		return destPath;
	}
}
