package dev.jbang.eclipse.ls.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.jdt.ls.core.internal.handlers.MapFlattener;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangClasspathUtils;
import dev.jbang.eclipse.core.internal.JBangFileUtils;
import dev.jbang.eclipse.core.internal.project.JBangFileDetector;
import dev.jbang.eclipse.core.internal.project.JBangProjectConfiguration;

@SuppressWarnings("restriction")
public class JBangImporter extends AbstractProjectImporter {

	private Collection<Path> scripts = null;

	@Override
	public boolean applies(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		scripts = new HashSet<>();
		Preferences preferences = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
		List<String> javaImportExclusions = preferences.getJavaImportExclusions();
		
		Collection<IPath> triggerFiles = preferences.getTriggerFiles();

		IPath rootPath = ResourceUtils.filePathFromURI(rootFolder.toPath().toUri().toString());
		var triggerFile = triggerFiles.stream().filter(tf -> rootPath.isPrefixOf(tf)).findFirst().map(tf -> Path.of(tf.toOSString())).orElse(null);
		
		JBangFileDetector scanner = new JBangFileDetector(Paths.get(rootFolder.toURI()), javaImportExclusions, triggerFile);
		scripts.addAll(scanner.scan(monitor));
		return !scripts.isEmpty();
	}

	@Override
	public void importToWorkspace(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (scripts == null || scripts.isEmpty()) {
			return;
		}

		var jbang = JBangCorePlugin.getJBangManager();
		JBangProjectConfiguration configuration = new JBangProjectConfiguration();
		IPath rootFolderPath = org.eclipse.core.runtime.Path.fromOSString(rootFolder.getAbsolutePath());
		configuration.setRootFolder(rootFolderPath);
		configuration.setSourceFolder(ProjectUtils.WORKSPACE_LINK);
		boolean projectPerScript = MapFlattener.getBoolean(getPreferences().asMap(), "java.import.jbang.projectPerScript");
		if (!projectPerScript) {
			configuration.setLinkedSourceFolder(rootFolderPath);// Linking whole directory doesn't work is messy if there
																													// are multiple JBang scripts in that directory
		}
		JavaLanguageServerPlugin
				.logInfo("JBang import : " + (projectPerScript ? " 1 project per script" : "1 project per folder"));

		Collection<Path> toImport = projectPerScript ? scripts : Set.of(scripts.iterator().next());
		for (Path script : toImport) {
			try {
				String name = script.getFileName().toString();
				if (JBangFileUtils.isJBangBuildFile(script) || JBangFileUtils.isMainFile(script)) {
					name = script.getParent().getFileName().toString();
				}
				var sourceFolder = JBangClasspathUtils.inferSourceDirectory(script, monitor);
				if (sourceFolder != null) {
					// XXX check projectPerScript?
					if (rootFolderPath.isPrefixOf(sourceFolder)) {
						configuration.setLinkedSourceFolder(sourceFolder);
					}
				}
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
				// TODO Better check the script is actually bound to that project
				// TODO Trigger classpath update on the project
				if (project.exists() && dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject(project)) {
					JavaLanguageServerPlugin.logInfo("JBang script " + script + " already imported");
				} else {
					JavaLanguageServerPlugin
							.logInfo("Importing JBang script " + script + (configuration.getLinkedSourceFolder() == null ? ""
									: " (linking " + configuration.getLinkedSourceFolder() + ")"));
					jbang.getProjectConfigurationManager().createJBangProject(script, configuration, monitor);
				}
			} catch (Exception e) {
				JavaLanguageServerPlugin.logException("Error importing " + script, e);
			}
		}
	}

	@Override
	public boolean isResolved(File folder) throws OperationCanceledException, CoreException {
		if (folder != null && scripts != null) {
			Path folderPath = folder.toPath();
			return scripts.stream().anyMatch(scriptPath -> scriptPath.startsWith(folderPath));
		}
		return false;
	}

	@Override
	public void reset() {
		scripts = null;
	}

}
