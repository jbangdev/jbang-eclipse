package dev.jbang.eclipse.core.internal.project;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import dev.jbang.eclipse.core.internal.ExceptionFactory;
import dev.jbang.eclipse.core.internal.JBangFileUtils;

/**
 * Searches recursively for all the directories containing a given filename.
 *
 * @author Fred Bricon
 */
public class JBangFileDetector {

	private static final String METADATA_FOLDER = "**/.metadata";
	private static final Set<FileVisitOption> FOLLOW_LINKS_OPTION = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
	private List<Path> scripts;
	private List<Path> mains;
	private List<Path> builds;

	private Path rootDir;
	private int maxDepth = 3;
	private Set<String> exclusions = new LinkedHashSet<>(1);

	/**
	 * Constructs a new JBangFileDetector for the given root directory, searching
	 * for fileNames. By default, the search depth is limited to 3. Sub-directories
	 * of a found directory will be walked through. The ".metadata" folder is
	 * excluded.
	 *
	 * @param rootDir
	 *            the root directory to search for files
	 * @param fileNames
	 *            the names of the file to search
	 */
	public JBangFileDetector(Path rootDir) {
		this(rootDir, null);
	}

	public JBangFileDetector(Path rootDir, List<String> exclusions) {
		this.rootDir = rootDir;
		scripts = new ArrayList<>();
		builds = new ArrayList<>();
		mains = new ArrayList<>();
		addExclusions(METADATA_FOLDER);
		if (exclusions != null) {
			for (String pattern : exclusions) {
				addExclusions(pattern);
			}
		}
	}

	/**
	 * Adds the names of directories to exclude from the search. All its sub-directories will be skipped.
	 *
	 * @param excludes directory name(s) to exclude from the search
	 * @return a reference to this object.
	 */
	public JBangFileDetector addExclusions(String...excludes) {
		if (excludes != null) {
			exclusions.addAll(Arrays.asList(excludes));
		}
		return this;
	}

	/**
	 * Sets the maximum depth of the search
	 * @param maxDepth the maximum depth of the search. Must be > 0.
	 * @return a reference to this object.
	 */
	public JBangFileDetector maxDepth(int maxDepth) {
		Assert.isTrue(maxDepth > 0, "maxDepth must be > 0");
		this.maxDepth = maxDepth;
		return this;
	}

	/**
	 * Returns the scripts found.
	 * @return an unmodifiable collection of {@link Path}s.
	 */
	public Collection<Path> getScripts() {
		return Collections.unmodifiableList(scripts);
	}

	/**
	 * Returns the "main.java" scripts found.
	 * @return an unmodifiable collection of {@link Path}s.
	 */
	public Collection<Path> getMains() {
		return Collections.unmodifiableList(mains);
	}

	/**
	 * Returns the build.jbang files found.
	 * @return an unmodifiable collection of {@link Path}s.
	 */
	public Collection<Path> getBuildFiles() {
		return Collections.unmodifiableList(builds);
	}

	/**
	 * Scan the  the directories found to be containing the sought-after file.
	 * @param monitor the {@link IProgressMonitor} used to handle scan interruption.
	 * @return an unmodifiable collection of {@link Path}s.
	 * @throws CoreException if an error is encountered during the scan
	 */
	public Collection<Path> scan(IProgressMonitor monitor) throws CoreException {
		try {
			scanDir(rootDir, monitor == null? new NullProgressMonitor(): monitor);
		} catch (IOException e) {
			throw  ExceptionFactory.newException("Failed to scan "+rootDir, e);
		}
		if (!builds.isEmpty()) {
			return getBuildFiles();
		}
		if (!mains.isEmpty()) {
			return getMains();
		}

		return getScripts();
	}

	private void scanDir(Path dir, final IProgressMonitor monitor) throws IOException {
		FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (monitor.isCanceled()) {
					return TERMINATE;
				}
				Objects.requireNonNull(dir);
				if (isExcluded(dir)) {
					return SKIP_SUBTREE;
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (JBangFileUtils.isJBangBuildFile(file)) {
					builds.add(file);
				} else if (JBangFileUtils.isMainFile(file)) {
					mains.add(file);
				} else if (JBangFileUtils.isJBangFile(file)) {
					scripts.add(file);
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				Objects.requireNonNull(file);
    			if (exc instanceof FileSystemLoopException) {
        			return CONTINUE;
    			}
				throw exc;
			}

		};
		Files.walkFileTree(dir, FOLLOW_LINKS_OPTION, maxDepth, visitor);
	}

	private boolean isExcluded(Path dir) {
		if (dir.getFileName() == null) {
			return true;
		}
		boolean excluded = false;
		for (String pattern : exclusions) {
			boolean includePattern = false;
			if (pattern.startsWith("!")) {
				includePattern = true;
				pattern = pattern.substring(1);
			}
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
			if (matcher.matches(dir)) {
				excluded = includePattern ? false : true;
			}
		}
		return excluded;
	}

}
