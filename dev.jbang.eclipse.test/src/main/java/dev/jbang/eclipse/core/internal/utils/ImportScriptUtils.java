package dev.jbang.eclipse.core.internal.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangClasspathUtils;
import dev.jbang.eclipse.core.internal.project.JBangFileDetector;
import dev.jbang.eclipse.core.internal.project.JBangProject;
import dev.jbang.eclipse.core.internal.project.JBangProjectConfiguration;

public class ImportScriptUtils {

	private static Path scriptsDir = Paths.get("scripts");

	private ImportScriptUtils() {}


	public static JBangProject importJBangScript(String... relativePaths) throws Exception {
		Path mainScript = null;

		var workDir = Paths.get("target", "workdir", System.currentTimeMillis()+"");
		Files.createDirectories(workDir);

		for (String relativePath : relativePaths) {
			var sourcetPath = scriptsDir.resolve(relativePath);
			if (!Files.exists(sourcetPath)) {
				throw new IOException("Import failed: "+sourcetPath + " does not exist");
			}

			var destPath = copy(sourcetPath, workDir);
			if (mainScript == null) {
				mainScript = destPath;
			}
		}

		var projectManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		var configuration = new JBangProjectConfiguration();
		var sourceFolder = JBangClasspathUtils.inferSourceDirectory(mainScript.toAbsolutePath(), new NullProgressMonitor());
		if (sourceFolder != null) {
				configuration.setLinkedSourceFolder(sourceFolder);
		}
		var jbangProject = projectManager.createJBangProject(mainScript, configuration , new NullProgressMonitor());

		return jbangProject;
	}


	private static Path copy(Path sourcePath, Path destFolder) throws IOException {
		var destPath = destFolder.resolve(scriptsDir.relativize(sourcePath));
		Files.deleteIfExists(destPath);
		Files.createDirectories(destPath.getParent());
		Files.copy(sourcePath, destPath);
		return destPath;
	}


	public static JBangProject importJBangFolder(String folder) throws Exception {
		var srcDir = scriptsDir.resolve(folder);
		var workDir = Paths.get("target", "workdir");
		Files.createDirectories(workDir);


		FileUtils.copyDirectoryToDirectory(srcDir.toFile(), workDir.toFile());

		var destDir = workDir.resolve(srcDir.getFileName());
		var monitor = new NullProgressMonitor();
		var scripts = new JBangFileDetector(destDir).scan(monitor);
		assertFalse(scripts.isEmpty(), "No scripts found under "+destDir);
		Path mainScript = scripts.iterator().next();

		var projectManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
		var config = new JBangProjectConfiguration();
		config.setLinkedSourceFolder(org.eclipse.core.runtime.Path.fromOSString(destDir.toAbsolutePath().toString()));
		var jbangProject = projectManager.createJBangProject(mainScript, config,monitor);
		return jbangProject;
	}
}
