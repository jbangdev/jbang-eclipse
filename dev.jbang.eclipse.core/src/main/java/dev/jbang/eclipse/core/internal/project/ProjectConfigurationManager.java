package dev.jbang.eclipse.core.internal.project;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangClasspathUtils;
import dev.jbang.eclipse.core.internal.JBangConstants;
import dev.jbang.eclipse.core.internal.ProjectUtils;
import dev.jbang.eclipse.core.internal.ResourceUtil;
import dev.jbang.eclipse.core.internal.classpath.JBangClasspathContainer;
import dev.jbang.eclipse.core.internal.process.JBangExecution;
import dev.jbang.eclipse.core.internal.process.JBangInfoResult;
import dev.jbang.eclipse.core.internal.process.JBangInfoResult.JBangFile;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;

public class ProjectConfigurationManager {

	private static final String JAR_SUFFIX = ".jar";

	private static final String SOURCE_JAR_SUFFIX = "-sources.jar";

	private static final String JAVADOC_JAR_SUFFIX = "-javadoc.jar";

	private Map<IProject, JBangInfoResult> cache = new ConcurrentHashMap<>();

	private JBangRuntimeManager runtimeManager;

	public ProjectConfigurationManager(JBangRuntimeManager runtimeManager) {
		this.runtimeManager = runtimeManager;
	}

	public void addJBangNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (!project.hasNature(JBangConstants.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
			newNatures[0] = JBangConstants.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
	}

	public void configure(JBangProject jbp, JBangInfoResult info, IProgressMonitor monitor) throws CoreException {
		IProject project = jbp.getProject();
		var oldInfo = cache.get(project);
		if (Objects.equals(info, oldInfo)) {
			// Nothing changed
			return;
		}
		IJavaProject jp = JavaCore.create(jbp.getProject());
		if (jp != null) {
			String environmentId = info.getTargetRuntime();
			boolean hasJRE = false;
			boolean changedJRE = false;
			List<IClasspathEntry> newEntries = new ArrayList<>();
			List<String> resolvedClasspath = info.getResolvedDependencies() == null ? new ArrayList<>(0)
					: new ArrayList<>(info.getResolvedDependencies());
			IClasspathEntry[] classpath = jp.getRawClasspath();

			IExecutionEnvironment ee = null;
			IClasspathEntry jbangContainerEntry = null;
			for (IClasspathEntry entry : classpath) {
				if (isJRE(entry)) {
					hasJRE = true;
					String currentEnvironment = entry.getPath().lastSegment();
					if (environmentId != null && !environmentId.endsWith(currentEnvironment)) {
						ee = getExecutionEnvironment(environmentId);
						newEntries.add(newJRE(ee));
						changedJRE = true;
					} else {
						newEntries.add(entry);
					}
				} else if (JBangClasspathUtils.isJBangClasspathContainer(entry.getPath())) {
					jbangContainerEntry = entry;
					newEntries.add(entry);
				} else if (IClasspathEntry.CPE_LIBRARY == entry.getEntryKind()) {
					if (entry.getPath() != null && !resolvedClasspath.contains(entry.getPath().toOSString())) {
						newEntries.add(entry);
					}
				} else {
					newEntries.add(entry);
				}
			}
			if (!hasJRE) {
				ee = getExecutionEnvironment(environmentId);
				newEntries.add(newJRE(ee));
				changedJRE = true;
			}
			// iterate over dependencies
			List<IClasspathEntry> dependenciesList = new ArrayList<>();
			for (String path : resolvedClasspath) {
				if (!path.isBlank()) {
					File jar = new File(path);
					dependenciesList.add(JavaCore.newLibraryEntry(new Path(path), getSources(jar), getJavadoc(jar)));
				}
			}
			IClasspathEntry[] dependencies = dependenciesList.toArray(new IClasspathEntry[dependenciesList.size()]);
			if (jbangContainerEntry == null) {
				jbangContainerEntry = JBangClasspathUtils.getDefaultContainerEntry();
				newEntries.add(jbangContainerEntry);
			}
			try {
				IClasspathContainer container = new JBangClasspathContainer(jbangContainerEntry.getPath(), dependencies);
				JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] { jp }, new IClasspathContainer[] { container }, monitor);
				JBangClasspathUtils.saveContainerState(project, container);
			} catch (CoreException ex) {
				JBangCorePlugin.log(ex.getMessage(), ex);
			}

			IClasspathEntry[] newClasspath = newEntries.toArray(new IClasspathEntry[newEntries.size()]);
			boolean updateCache = false;
			if (changedJRE || oldInfo == null
					|| !Objects.deepEquals(info.getCompileOptions(), oldInfo.getCompileOptions())) {
				Map<String, String> oldOptions = jp.getOptions(false);
				Map<String, String> options = new HashMap<>(oldOptions);
				if (ee != null) {
					options.putAll(ee.getComplianceOptions());
				}
				// TODO support more JBANG_JAVAC_OPTIONS, JDK_JAVAC_OPTIONS, JAVAC_OPTIONS
				String parameters = info.getCompileOptions() != null && info.getCompileOptions().contains("-parameters")
						? JavaCore.GENERATE
						: JavaCore.DO_NOT_GENERATE;
				options.put(JavaCore.COMPILER_CODEGEN_METHOD_PARAMETERS_ATTR, parameters);

				String previewFeatures = info.getCompileOptions() != null
						&& info.getCompileOptions().contains("--enable-preview") ? JavaCore.ENABLED : JavaCore.DISABLED;
				options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, previewFeatures);

				if (!Objects.deepEquals(oldOptions, options)) {
					jp.setOptions(options);
					updateCache = true;
				}
			}

			if (!Objects.deepEquals(newClasspath, classpath)) {
				jp.setRawClasspath(newClasspath, monitor);
				updateCache = true;
			}

			if (updateCache) {
				cache.put(project, info);
			}

		}
	}

	/**
	 * @param entry
	 * @return
	 */
	private boolean isJRE(IClasspathEntry entry) {
		return entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
				&& JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0));
	}

	private IClasspathEntry newJRE(IExecutionEnvironment executionEnvironment) {
		IClasspathEntry cpe;
		if (executionEnvironment == null) {
			cpe = JavaRuntime.getDefaultJREContainerEntry();
		} else {
			IPath containerPath = JavaRuntime.newJREContainerPath(executionEnvironment);
			cpe = JavaCore.newContainerEntry(containerPath);
		}
		return cpe;
	}

	private IExecutionEnvironment getExecutionEnvironment(String environmentId) {
		if (environmentId == null) {
			return null;
		}
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] environments = manager.getExecutionEnvironments();
		for (IExecutionEnvironment environment : environments) {
			if (environment.getId().equals(environmentId)) {
				return environment;
			}
		}
		return null;
	}

	public JBangProject getJBangProject(IProject project, IProgressMonitor monitor) {
		// TODO use cache / project registry
		if (!ProjectUtils.isJBangProject(project)) {
			return null;
		}
		JBangProject jbp = new JBangProject(project);
		JBangRuntime jbang = runtimeManager.getDefaultRuntime();
		jbp.setRuntime(jbang);
		return jbp;
	}

	private IPath getSources(File jar) {
		return getMatchingFile(jar, SOURCE_JAR_SUFFIX);
	}

	private IPath getJavadoc(File jar) {
		return getMatchingFile(jar, JAVADOC_JAR_SUFFIX);
	}

	private IPath getMatchingFile(File jar, String suffix) {
		String filename = jar.getName();
		// See if there's a matching file in the same directory
		String sourceName = filename.substring(0, filename.lastIndexOf(JAR_SUFFIX)) + suffix;
		File sourceJar = new File(jar.getParentFile(), sourceName);
		if (sourceJar.isFile()) {
			return new Path(sourceJar.getAbsolutePath());
		}
		return null;
	}

	public void unconfigure(IProject project, IProgressMonitor monitor) {
		cache.remove(project);
	}

	public JBangProject createJBangProject(java.nio.file.Path script, IProgressMonitor monitor) throws CoreException {
		var jbang = runtimeManager.getDefaultRuntime();
		var execution = new JBangExecution(jbang, script.toFile(), null);
		var info = execution.getInfo(monitor);

		String fileName = script.getFileName().toString();
		String name = fileName.substring(0, fileName.lastIndexOf(".java"));

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.exists()) {
			IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(name);
			project.create(description, monitor);
			project.open(monitor);
			description.setNatureIds(new String[] { JBangConstants.NATURE_ID, JavaCore.NATURE_ID });
			project.setDescription(description, monitor);
		}

		IJavaProject javaProject = JavaCore.create(project);

		List<IClasspathEntry> classpaths = new ArrayList<>();
		// Add source folder
		IFolder source = project.getFolder("src");
		if (!source.exists()) {
			source.create(true, true, monitor);
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(source);
		IClasspathEntry srcClasspath = JavaCore.newSourceEntry(root.getPath());
		classpaths.add(srcClasspath);
		javaProject.setRawClasspath(classpaths.toArray(new IClasspathEntry[0]), monitor);

		IFolder bin = project.getFolder("bin");
		if (!bin.exists()) {
			bin.create(true, true, monitor);
		}
		javaProject.setOutputLocation(bin.getFullPath(), monitor);

		IFile mainFile = link(info.getBackingResource(), project, monitor);
		if (info.getSources() != null && !info.getSources().isEmpty()) {
			for (String s : info.getSources()) {
				link(s, project, monitor);
			}
		}
		if (info.getFiles() != null && !info.getFiles().isEmpty()) {
			for (JBangFile file : info.getFiles()) {
				link(file.originalResource, file.target, project, monitor);
			}
		}

		var jbp = new JBangProject(project);
		jbp.setMainSourceFile(mainFile);

		configure(jbp, info, monitor);

		return jbp;
	}

	private IFile link(String resource, IProject project, IProgressMonitor monitor) throws CoreException {
		java.nio.file.Path p = Paths.get(resource);
		String fileName = p.getFileName().toString();
		return link(resource, fileName, project, monitor);
	}

	private IFile link(String resource, String link, IProject project, IProgressMonitor monitor) throws CoreException {
		IPath filePath = new Path("src").append(link);
		IFile fakeFile = project.getFile(filePath);
		if (fakeFile.exists()) {
			return fakeFile;
		}
		IFolder parent = (IFolder) fakeFile.getParent();
		ResourceUtil.createFolder(parent, monitor);
		java.nio.file.Path p = Paths.get(resource);
		fakeFile.createLink(p.toUri(), IResource.REPLACE, monitor);
		return fakeFile;
	}

}
