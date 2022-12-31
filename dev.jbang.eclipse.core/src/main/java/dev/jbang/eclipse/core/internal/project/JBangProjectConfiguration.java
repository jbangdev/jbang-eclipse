package dev.jbang.eclipse.core.internal.project;

import org.eclipse.core.runtime.IPath;

public class JBangProjectConfiguration {

	private static final String DEFAULT_SOURCE_FOLDER = "src";

	public JBangProjectConfiguration() {
		sourceFolder = DEFAULT_SOURCE_FOLDER;
	}

	private IPath linkedSourceFolder;

	private IPath rootFolder;

	public IPath getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(IPath rootFolder) {
		this.rootFolder = rootFolder;
	}

	private String sourceFolder;

	public IPath getLinkedSourceFolder() {
		return linkedSourceFolder;
	}

	public void setLinkedSourceFolder(IPath linkedSourceFolder) {
		this.linkedSourceFolder = linkedSourceFolder;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(String sourceFolder) {
		if (sourceFolder == null || sourceFolder.isBlank()) {
			sourceFolder = DEFAULT_SOURCE_FOLDER;
		}
		this.sourceFolder = sourceFolder;
	}

}
