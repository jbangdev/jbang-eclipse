package io.jbang.eclipse.core.internal.project;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import io.jbang.eclipse.core.internal.JBangConstants;
import io.jbang.eclipse.core.internal.ProjectUtils;
import io.jbang.eclipse.core.internal.ResourceUtil;
import io.jbang.eclipse.core.internal.process.JBangExecution;
import io.jbang.eclipse.core.internal.process.JBangInfo;
import io.jbang.eclipse.core.internal.runtine.JBangRuntime;

public class ProjectConfigurationManager {

	private static final String JAR_SUFFIX = ".jar";

	private static final String SOURCE_JAR_SUFFIX = "-sources.jar";

	private static final String JAVADOC_JAR_SUFFIX = "-javadoc.jar";

	private Map<IProject, JBangInfo> cache = new ConcurrentHashMap<>();

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

	public void configure(JBangProject jbp, JBangInfo info, IProgressMonitor monitor) throws CoreException {
		IProject project = jbp.getProject();
		if (Objects.equals(info, cache.get(project))) {
			// Nothing changed
			return;
		}
		IJavaProject jp = JavaCore.create(jbp.getProject());
		if (jp != null) {
			String environmentId = info.getTargetRuntime();
			boolean hasJRE = false;

			Deque<IClasspathEntry> newEntries = new LinkedList<>();
			List<String> resolvedClasspath = info.getResolvedDependencies() == null ? new ArrayList<>(0)
					: new ArrayList<>(info.getResolvedDependencies());
			IClasspathEntry[] classpath = jp.getRawClasspath();

			IExecutionEnvironment ee = null;
			for (IClasspathEntry entry : classpath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					String path = entry.getPath().toOSString();
					if (resolvedClasspath.remove(path)) {
						newEntries.add(entry);
					}
				} else if (isJRE(entry)) {
					hasJRE = true;
					String currentEnvironment = entry.getPath().lastSegment();
					if (environmentId != null && !environmentId.endsWith(currentEnvironment)) {
						ee = getExecutionEnvironment(environmentId);
						newEntries.add(newJRE(ee));
					} else {
						newEntries.add(entry);
					}
				} else {
					newEntries.add(entry);
				}
			}
			if (!hasJRE) {
				ee = getExecutionEnvironment(environmentId);
				newEntries.add(newJRE(ee));
			}
			// iterate over remaining entries
			for (String path : resolvedClasspath) {
				if (!path.isBlank()) {
					File jar = new File(path);
					newEntries.add(JavaCore.newLibraryEntry(new Path(path), getSources(jar), getJavadoc(jar)));
				}
			}
			IClasspathEntry[] newClasspath = newEntries.toArray(new IClasspathEntry[newEntries.size()]);
			if (!Objects.deepEquals(newClasspath, classpath)) {
				if (ee != null) {
					// TODO support JBANG_JAVAC_OPTIONS, JDK_JAVAC_OPTIONS, JAVAC_OPTIONS
					Map<String, String> options = jp.getOptions(false);
					options.putAll(ee.getComplianceOptions());
					jp.setOptions(options);
				}
				jp.setRawClasspath(newClasspath, monitor);
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

	public JBangProject getJBangProject(IProject project) {
		// TODO use cache / project registry
		if (!ProjectUtils.isJBangProject(project)) {
			return null;
		}
		JBangProject jbp = new JBangProject(project);
		JBangRuntime jbang = new JBangRuntime(System.getProperty("jbang.path", "~/.sdkman/candidates/jbang/current/"));
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
		var jbang = new JBangRuntime("~/.sdkman/candidates/jbang/current/");
		var execution = new JBangExecution(jbang, script.toFile());
		var info = execution.getInfo();

		String fileName = script.getFileName().toString();
		String name = fileName.substring(0, fileName.lastIndexOf(".java"));

		IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(name);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		project.create(description, monitor);
		project.open(monitor);
		description.setNatureIds(new String[] { JBangConstants.NATURE_ID, JavaCore.NATURE_ID });
		project.setDescription(description, monitor);
		IJavaProject javaProject = JavaCore.create(project);

		List<IClasspathEntry> classpaths = new ArrayList<>();
		// Add source folder
		IFolder source = project.getFolder("src");
		source.create(true, true, monitor);
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
			for (Map.Entry<String, String> file : info.getFiles().entrySet()) {
				link(file.getValue(), file.getKey(), project, monitor);
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
		IFolder parent = (IFolder) fakeFile.getParent();
		ResourceUtil.createFolder(parent, monitor);
		java.nio.file.Path p = Paths.get(resource);
		fakeFile.createLink(p.toUri(), IResource.REPLACE, monitor);
		return fakeFile;
	}

}
