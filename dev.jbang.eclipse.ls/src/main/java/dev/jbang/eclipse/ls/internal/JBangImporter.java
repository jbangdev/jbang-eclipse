package dev.jbang.eclipse.ls.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.jdt.ls.core.internal.handlers.MapFlattener;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangFileUtils;
import dev.jbang.eclipse.core.internal.project.JBangFileDetector;
import dev.jbang.eclipse.core.internal.project.JBangProjectConfiguration;

@SuppressWarnings("restriction")
public class JBangImporter extends AbstractProjectImporter  {
	
	private Collection<Path> scripts = null;

	@Override
	public boolean applies(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		scripts = new HashSet<>();
		List<String> javaImportExclusions = JavaLanguageServerPlugin.getPreferencesManager().getPreferences().getJavaImportExclusions();
		JBangFileDetector scanner = new JBangFileDetector(Paths.get(rootFolder.toURI()), javaImportExclusions);
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
		configuration.setSourceFolder(ProjectUtils.WORKSPACE_LINK); 
		boolean projectPerScript = MapFlattener.getBoolean(getPreferences().asMap(), "java.jbang.import.projectPerScript"); 
		if (!projectPerScript) {
			configuration.setLinkedSourceFolder(rootFolder.toURI());//Linking whole directory doesn't work is messy if there are multiple JBang scripts in that directory			
		}
		JavaLanguageServerPlugin.logInfo("JBang import : " + ((projectPerScript)?" 1 project per script" : "1 project per folder"));
		
		Collection<Path> toImport = (projectPerScript)? scripts: Set.of(scripts.iterator().next());
		for (Path script : toImport) {
			try {
				String name = script.getFileName().toString();
				if (JBangFileUtils.isJBangBuildFile(script) || JBangFileUtils.isMainFile(script)) {
					name = script.getParent().getFileName().toString();
				}
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
				//TODO Better check the script is actually bound to that project
				//TODO Trigger classpath update on the project
				if (project.exists() && dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject(project)) {
					JavaLanguageServerPlugin.logInfo("JBang script " + script + " already imported");
				} else {
					JavaLanguageServerPlugin.logInfo("Importing JBang script " + script + ((configuration.getLinkedSourceFolder() == null)?"": " (linking "+configuration.getLinkedSourceFolder()+")"));
					jbang.getProjectConfigurationManager().createJBangProject(script, configuration, monitor);		
				}
			} catch (Exception e) {
				JavaLanguageServerPlugin.logException("Error importing "+script, e);
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
