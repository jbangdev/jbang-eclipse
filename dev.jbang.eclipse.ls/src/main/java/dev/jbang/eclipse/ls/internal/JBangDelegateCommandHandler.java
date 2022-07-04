package dev.jbang.eclipse.ls.internal;

import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.project.SynchronizeJBangJob;

public class JBangDelegateCommandHandler implements IDelegateCommandHandler {

	private static final String JDTLS_JBANG_SYNCHRONIZE_COMMAND = "jbang/synchronize";

	@Override
	public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor monitor) throws Exception {
		switch (commandId) {
		case JDTLS_JBANG_SYNCHRONIZE_COMMAND:
			if (arguments != null && !arguments.isEmpty() && arguments.get(0) instanceof Collection) {
				return synchronize((Collection<Object>)arguments.get(0), monitor);
			}
		}
		return null;
	}

	private Object synchronize(Collection<Object> uris, IProgressMonitor monitor) {
		Set<IFile> jbangFiles = collectFiles(uris);
		if (!jbangFiles.isEmpty()) {
			JBangCorePlugin.logInfo("Found "+jbangFiles.size()+" files to synchronize");
			var configManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
			new SynchronizeJBangJob(configManager, jbangFiles).schedule();
		}
		return null;
	}

	private Set<IFile> collectFiles(Collection<Object> params) {
		Set<IFile> jbangFiles = new LinkedHashSet<>();
		for (Object unknownUri : params) {
			URI uri = null;
			if (unknownUri instanceof URI) {
				uri = (URI) unknownUri;
			} else if (unknownUri instanceof String) {
				try {
					uri = new URI(unknownUri.toString());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
			if (uri == null) {
				continue;
			}
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
			if (files.length > 0) {
				Arrays.sort(files, (f1, f2) -> {
					return f1.getProjectRelativePath().segmentCount() - f2.getProjectRelativePath().segmentCount();
				});
				IFile file = files[0];
				if (file != null && isJBangFile(file)) {
					jbangFiles.add(file);
				}
			}
		}
		return jbangFiles;
	}
}
