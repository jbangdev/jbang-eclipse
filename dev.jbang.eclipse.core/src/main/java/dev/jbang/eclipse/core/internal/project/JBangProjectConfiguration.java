package dev.jbang.eclipse.core.internal.project;

import java.net.URI;

public class JBangProjectConfiguration {
	
	private static final String DEFAULT_SOURCE_FOLDER = "src";

	public JBangProjectConfiguration() {
		sourceFolder = DEFAULT_SOURCE_FOLDER;
	}
	
	private URI linkedSourceFolder;
	
	private String sourceFolder;

	public URI getLinkedSourceFolder() {
		return linkedSourceFolder;
	}

	public void setLinkedSourceFolder(URI linkedSourceFolder) {
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
