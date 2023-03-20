package dev.jbang.eclipse.core.internal.project;

import static dev.jbang.eclipse.core.JBangCorePlugin.log;
import static dev.jbang.eclipse.core.internal.ExceptionFactory.newException;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJavaProject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer.FactoryType;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.core.util.IFactoryPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.JBangClasspathUtils;
import dev.jbang.eclipse.core.internal.JBangConstants;
import dev.jbang.eclipse.core.internal.JBangFileUtils;
import dev.jbang.eclipse.core.internal.ProjectUtils;
import dev.jbang.eclipse.core.internal.ResourceUtil;
import dev.jbang.eclipse.core.internal.classpath.AnnotationProcessorUtils;
import dev.jbang.eclipse.core.internal.classpath.JBangClasspathContainer;
import dev.jbang.eclipse.core.internal.process.JBangDependencyError;
import dev.jbang.eclipse.core.internal.process.JBangError;
import dev.jbang.eclipse.core.internal.process.JBangInfoExecution;
import dev.jbang.eclipse.core.internal.process.JBangInfoResult;
import dev.jbang.eclipse.core.internal.process.JBangInfoResult.JBangFile;
import dev.jbang.eclipse.core.internal.process.JBangMissingResource;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntime;
import dev.jbang.eclipse.core.internal.runtime.JBangRuntimeManager;
import dev.jbang.eclipse.core.internal.vms.JBangManagedVMService;

@SuppressWarnings("restriction")
public class ProjectConfigurationManager {

	private static final String M2_REPO = "M2_REPO";

	private static final String JAR_SUFFIX = ".jar";

	private static final String SOURCE_JAR_SUFFIX = "-sources.jar";

	private static final String JAVADOC_JAR_SUFFIX = "-javadoc.jar";

	private Map<IProject, JBangInfoResult> cache = new ConcurrentHashMap<>();

	private JBangRuntimeManager runtimeManager;

	public ProjectConfigurationManager(JBangRuntimeManager runtimeManager) {
		this.runtimeManager = runtimeManager;
	}

	public void configure(JBangProject jbp, JBangInfoResult info, IProgressMonitor monitor) throws CoreException {
		IProject project = jbp.getProject();
		if (!isJavaProject(project)) {
			return;
		}
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

			if (info.getAvailableJdkPath() != null && !info.getAvailableJdkPath().isBlank()) {
				// Ensure we'll have a proper Execution Environment available
				new JBangManagedVMService().getJVM(new File(info.getAvailableJdkPath()), monitor);
			}

			IExecutionEnvironment ee = null;
			IClasspathEntry jbangContainerEntry = null;
			for (IClasspathEntry entry : classpath) {
				if (isJRE(entry)) {
					hasJRE = true;
					String currentEnvironment = entry.getPath().lastSegment();
					ee = getExecutionEnvironment(currentEnvironment);
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
				JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] { jp },
						new IClasspathContainer[] { container }, monitor);
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

				if (info.getCompileOptions() != null) {
					int sourceIdx = info.getCompileOptions().indexOf("-source");
					if (sourceIdx > 0 && info.getCompileOptions().size() > sourceIdx + 1) {
						String sourceString = info.getCompileOptions().get(sourceIdx + 1);
						// XXX In theory, should not set compliance options, but this is more convenient
						JavaCore.setComplianceOptions(sourceString, options);
					}
					// FIXME Having both --release and -source should be an error!
					int releaseIdx = info.getCompileOptions().indexOf("--release");
					if (releaseIdx > 0 && info.getCompileOptions().size() > releaseIdx + 1) {
						String releaseString = info.getCompileOptions().get(releaseIdx + 1);
						JavaCore.setComplianceOptions(releaseString, options);
					}
				}
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

			configureAnnotationProcessors(jp, info.getCompileOptions(), resolvedClasspath);
		}
	}

	// Code mostly copied from
	// https://github.com/eclipse-m2e/m2e-core/blob/bb14b75bfa14a7548fd59707965e3e281c7bb415/org.eclipse.m2e.apt.core/src/org/eclipse/m2e/apt/internal/AbstractAptConfiguratorDelegate.java#L191-L225
	private void configureAnnotationProcessors(IJavaProject javaProject, List<String> options,
			List<String> resolvedClasspath) throws CoreException {

		List<File> jars = resolvedClasspath.stream().map(File::new)
				.filter(AnnotationProcessorUtils::isJar)
				.collect(Collectors.toList());

		if (!AnnotationProcessorUtils.containsAptProcessors(jars)) {
			if (AptConfig.isEnabled(javaProject) && ProjectUtils.isJBangProjectOnly(javaProject.getProject())) {
				AptConfig.setEnabled(javaProject, false);
			}
			return;
		}

		List<File> resolvedJarArtifactsInReverseOrder = new ArrayList<>(jars);
		Collections.reverse(resolvedJarArtifactsInReverseOrder);
		IFactoryPath factoryPath = AptConfig.getDefaultFactoryPath(javaProject);

		// Disable Plugin factories, has they're unknown to JBang
		for (FactoryContainer fc : ((FactoryPath) factoryPath).getEnabledContainers().keySet()) {
			if (FactoryType.PLUGIN.equals(fc.getType())) {
				factoryPath.disablePlugin(fc.getId());
			}
		}

		// Reuse M2_REPO variable if it exists
		IPath m2RepoPath = JavaCore.getClasspathVariable(M2_REPO);

		for (File resolvedJarArtifact : resolvedJarArtifactsInReverseOrder) {
			IPath absolutePath = new Path(resolvedJarArtifact.getAbsolutePath());
			// reference jars in a portable way
			if (m2RepoPath != null && m2RepoPath.isPrefixOf(absolutePath)) {
				IPath relativePath = absolutePath.removeFirstSegments(m2RepoPath.segmentCount()).makeRelative().setDevice(null);
				IPath variablePath = new Path(M2_REPO).append(relativePath);
				factoryPath.addVarJar(variablePath);
			} else {
				// fall back on using absolute references.
				factoryPath.addExternalJar(resolvedJarArtifact);
			}
		}

		Map<String, String> currentOptions = AptConfig.getRawProcessorOptions(javaProject);
		Map<String, String> newOptions = AnnotationProcessorUtils.parseProcessorOptions(options);
		if (!currentOptions.equals(newOptions)) {
			AptConfig.setProcessorOptions(newOptions, javaProject);
		}

		// Apply that IFactoryPath to the project
		AptConfig.setFactoryPath(javaProject, factoryPath);

		AptConfig.setEnabled(javaProject, true);
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
		if (!isJBangProject(project)) {
			return null;
		}
		JBangProject jbp = new JBangProject(project);
		// TODO use JBang runtime specific to this project
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

	public JBangProject createJBangProject(java.nio.file.Path script, JBangProjectConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {

		var jbang = runtimeManager.getDefaultRuntime();
		var execution = new JBangInfoExecution(jbang, script.toFile(), null);
		var info = execution.getInfo(monitor);

		String fileName = script.getFileName().toString();
		String name = fileName; // fileName.substring(0, fileName.lastIndexOf("."));
		if (JBangFileUtils.isJBangBuildFile(script) || JBangFileUtils.isMainFile(script)) {
			name = script.getParent().getFileName().toString();
		}

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
		if (configuration == null) {
			configuration = new JBangProjectConfiguration();
		}
		IFolder source = project.getFolder(configuration.getSourceFolder());
		if (!source.exists()) {
			ResourceUtil.createFolder(source, monitor);
		}

		var sourceDir = configuration.getLinkedSourceFolder();
		if (sourceDir != null) {
			source.createLink(sourceDir, IResource.REPLACE, monitor);
		}

		IFolder bin = project.getFolder(".jbang/bin");
		if (!bin.exists()) {
			ResourceUtil.createFolder(bin, monitor);
		}
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(source);

		IClasspathAttribute jbangScopeAttr = JavaCore.newClasspathAttribute("jbang.scope", "main");
		IClasspathEntry srcClasspath = JavaCore.newSourceEntry(root.getPath(), null, null, null, new IClasspathAttribute[] { jbangScopeAttr });
		classpaths.add(srcClasspath);
		javaProject.setRawClasspath(classpaths.toArray(new IClasspathEntry[0]), monitor);

		javaProject.setOutputLocation(bin.getFullPath(), monitor);

		IFile mainFile = null;

		if (configuration.getLinkedSourceFolder() == null && sourceDir == null) {

			if (JBangFileUtils.isJBangBuildFile(script)) {
				mainFile = link(script.toAbsolutePath().toString(), project, configuration, monitor);
			} else {
				mainFile = link(info.getBackingResource(), project, configuration, monitor);
			}

			if (info.getSources() != null && !info.getSources().isEmpty()) {
				for (JBangFile s : info.getSources()) {
					link(s.originalResource, project, configuration, monitor);
				}
			}
			if (info.getFiles() != null && !info.getFiles().isEmpty()) {
				for (JBangFile file : info.getFiles()) {
					String target = file.target;
					if (target == null) {
						link(file.originalResource, project, configuration, monitor);
					} else {
						link(file.originalResource, file.target, project, configuration, monitor);
					}
				}
			}
		} else {
			var scriptUri = script.toAbsolutePath().toUri();
			IFile[] mainCandidates = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(scriptUri);
			if (mainCandidates.length == 0) {
				// FIXME Move that fugly hack to some other utility class
				// FFS Eclipse on case-insensitive Macs!
				var sUri = scriptUri.toString();
				if (sUri.startsWith("file:///Users/")) {
					try {
						scriptUri = new URI(sUri.replaceFirst("file:///Users", "file:/USERS"));
						mainCandidates = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(scriptUri);
					} catch (URISyntaxException nooope) {
						// can't happen
					}
				}
			}

			for (IFile candidate : mainCandidates) {
				if (project.equals(candidate.getProject())) {
					mainFile = candidate;
					break;
				}
			}
			if (mainFile == null) {
				throw newException("Couldn't file main script O_o");
			}
		}

		var jbp = new JBangProject(project);
		jbp.setMainSourceFile(mainFile);

		configure(jbp, info, monitor);

		return jbp;
	}

	private IFile link(String resource, IProject project, JBangProjectConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		java.nio.file.Path p = Paths.get(resource);
		String fileName = p.getFileName().toString();
		return link(resource, fileName, project, configuration, monitor);
	}

	private IFile link(String resource, String link, IProject project, JBangProjectConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		IPath filePath = new Path(configuration.getSourceFolder()).append(link);
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

	public void synchronize(IFile file, IProgressMonitor monitor) throws CoreException {
		// System.err.println(file + " configuration changed, checking jbang info");
		IProject project = file.getProject();
		JBangProject jbp = getJBangProject(project, monitor);
		if (jbp == null
				|| isJavaProject(project) && JBangClasspathUtils.isOnOutputLocation(JavaCore.create(project), file)) {
			return;
		}
		monitor.setTaskName("Updating JBang configuration from " + file.getName());

		jbp.setMainSourceFile(file);
		var jbang = jbp.getRuntime();

		var execution = new JBangInfoExecution(jbang, file.getLocation().toFile(), null);
		JBangInfoResult info = execution.getInfo(monitor);
		clearMarkers(file);
		if (info != null) {
			if (info.getResolutionErrors() == null || info.getResolutionErrors().isEmpty()) {
				configure(jbp, info, monitor);
			} else {
				String source = getSource(file);
				info.getResolutionErrors().forEach(e -> {
					try {
						//TODO set marker on source file actually causing the error
						addErrorMarker(file, source, e);
					} catch (CoreException e1) {
						log(e1);
					}
				});
			}
		}
	}

	private String getSource(IFile file) throws CoreException {
		if (JBangFileUtils.isJBangBuildFile(file)) {
			return ResourceUtil.getContent(file);
		}
		ICompilationUnit typeRoot = JavaCore.createCompilationUnitFrom(file);
		return typeRoot.getBuffer().getContents();
	}

	private void clearMarkers(IFile file) throws CoreException {
		file.deleteMarkers(JBangConstants.MARKER_ID, true, IResource.DEPTH_ZERO);
	}

	private void addErrorMarker(IFile file, String source, JBangError e) throws CoreException {
		IMarker marker = file.createMarker(JBangConstants.MARKER_RESOLUTION_ID);
		marker.setAttribute(IMarker.MESSAGE, e.getMessage());
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.TRANSIENT, true);
		TextPosition pos = null;
		if (e instanceof JBangDependencyError dependencyError) {
			String dependency = dependencyError.getDependency();
			pos = findPosition(dependency, source);
		} else if (e instanceof JBangMissingResource missingResourceError) {
			String missingResource = missingResourceError.getResource();
			marker.setAttribute("JBANG_MISSING_RESOURCE", missingResource);
			pos = switch(e.getKind()) {
				case UnresolvedFile -> findMissingResourcePosition(source, "//FILES ", missingResource);				
				case UnresolvedSource -> findMissingResourcePosition(source, "//SOURCES ", missingResource);
				default -> null;
			};
		} else {
			switch(e.getKind()) {
				case JavaError -> {
					pos = findJavaPosition(source);
				}
				case ModuleError -> {
					pos = findModulePosition(source);
				}
				default -> {
				}
			}
		}
		if (pos == null) {
			marker.setAttribute(IMarker.LINE_NUMBER, 1);
		} else {
			marker.setAttribute(IMarker.LINE_NUMBER, pos.line);
			if (pos.start > 0) {
				marker.setAttribute(IMarker.CHAR_START, pos.start);
				marker.setAttribute(IMarker.CHAR_END, pos.end);
			}	
		}
	}

	private TextPosition findPosition(String dependency, String source) {
		TextPosition pos = new TextPosition();
		int line[] = new int[1];
		int lineOffset[] = new int[1];
		// FIXME only apply to //DEPS or @Grab lines
		// FIXME also match @Grab annotations using the verbose @Grab(group =
		// "ch.qos.reload4j", module = "reload4j", version = "1.2.19") notation
		source.lines().filter(l -> {
			line[0]++;
			int i = l.indexOf(dependency);
			if (i > -1) {
				pos.line = line[0];
				pos.start = lineOffset[0] + i;
				pos.end = pos.start + dependency.length();
			}
			lineOffset[0] += 1 + l.length();
			return i > 0;
		}).findFirst();
		return pos;
	}

	private TextPosition findJavaPosition(String source) {
		TextPosition pos = new TextPosition();
		int line[] = new int[1];
		int lineOffset[] = new int[1];
		source.lines().filter(l -> {
			line[0]++;
			var m = JBangFileUtils.JAVA_INSTRUCTION.matcher(l);
			int i = -1;
			if (m.matches()) {
				var badVersion = m.group(1);
				i = l.indexOf(badVersion);
				pos.line = line[0];
				pos.start = lineOffset[0] + i;
				pos.end = lineOffset[0] + l.length();
			}
			lineOffset[0] += 1 + l.length();
			return i > 0;
		}).findFirst();
		return pos;
	}
	
	private TextPosition findModulePosition(String source) {
		TextPosition pos = new TextPosition();
		int line[] = new int[1];
		int lineOffset[] = new int[1];
		source.lines().filter(l -> {
			line[0]++;
			var m = JBangFileUtils.MODULE_INSTRUCTION.matcher(l);
			int i = -1;
			if (m.matches()) {
				var badModule = m.group(1);
				i = l.indexOf(badModule);
				pos.line = line[0];
				pos.start = lineOffset[0] + i;
				pos.end = lineOffset[0] + l.length();
			}
			lineOffset[0] += 1 + l.length();
			return i > 0;
		}).findFirst();
		return pos;
	}
	
	private TextPosition findMissingResourcePosition(String source, String prefix, String missingSource) {
		TextPosition pos = new TextPosition();
		int line[] = new int[1];
		int lineOffset[] = new int[1];
		source.lines().filter(l -> {
			line[0]++;
			int i = -1;
			if (l.startsWith(prefix)) {
				i = l.indexOf(missingSource);
				pos.line = line[0];
				pos.start = lineOffset[0] + i;
				pos.end = pos.start + missingSource.length();
			}
			lineOffset[0] += 1 + l.length();
			return i > 0;
		}).findFirst();
		return pos;
	}

}
