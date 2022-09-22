package dev.jbang.eclipse.core.internal;

import static dev.jbang.eclipse.core.internal.ExceptionFactory.newException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ResourceUtil {

	private ResourceUtil() {
	}


	public static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent = folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder) parent,  monitor);
			}
			folder.create(true, true, monitor);
		}
	}
	
	/**
	 * Writes content to file, within the workspace. A change event is emitted.
	 */
	public static void setContent(IFile file, String content) throws CoreException {
		Assert.isNotNull(file, "file can not be null");
		if (content == null) {
			content = "";
		}
		try (InputStream newContent = new ByteArrayInputStream(content.getBytes())) {
			file.setContents(newContent, IResource.FORCE, null);
		} catch (IOException e) {
			throw newException("Can not write to " + file.getRawLocation(), e);
		}
	}

	public static String getContent(IFile file) throws CoreException {
		if (file == null) {
			return null;
		}
		try (final Reader reader = new InputStreamReader(file.getContents(), file.getCharset())) {
			int bufferSize = 1024;
			char[] buffer = new char[bufferSize];
			StringBuilder out = new StringBuilder();
			for (int numRead; (numRead = reader.read(buffer, 0, buffer.length)) > 0; ) {
				out.append(buffer, 0, numRead);
			}
			return out.toString();
		} catch (IOException e) {
			throw newException("Can not get " + file.getRawLocation() + " content", e);
		}
	}
}