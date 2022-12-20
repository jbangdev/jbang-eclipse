package dev.jbang.eclipse.ui.internal.commands;

import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangBuildFile;
import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangFile;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import dev.jbang.eclipse.core.JBangCorePlugin;
import dev.jbang.eclipse.core.internal.project.SynchronizeJBangJob;

public class SynchronizeJBangHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Object[] elements = null;
		if (selection instanceof IStructuredSelection structuredSelection) {
			elements = structuredSelection.toArray();
		} else {
			elements = getFileInActiveEditor(event);
		}
		if (elements == null || elements.length == 0) {
			return null;
		}
		Set<IFile> jbangFiles = collectFiles(elements);
		if (!jbangFiles.isEmpty()) {
			var configManager = JBangCorePlugin.getJBangManager().getProjectConfigurationManager();
			new SynchronizeJBangJob(configManager, jbangFiles).schedule();
		}
		return null;
	}

	private IFile[] getFileInActiveEditor(ExecutionEvent event) {
		IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof IEditorPart editorPart
				&& editorPart.getEditorInput() instanceof IFileEditorInput fileEditorInput) {
			IFile file = fileEditorInput.getFile();
			IProject project = file.getProject();
			if (project != null && project.isAccessible()) {
				return new IFile[] { file };
			}
		}
		return null;
	}

	private Set<IFile> collectFiles(Object[] elements) {
		Set<IFile> files = new LinkedHashSet<>();
		for (Object element : elements) {
			IResource file = Adapters.adapt(element, IResource.class);
			if (file != null && (isJBangFile(file) || isJBangBuildFile(file)) && file.getLocation() != null) {
				files.add((IFile) file);
			}
		}
		return files;
	}
}
