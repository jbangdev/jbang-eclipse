package dev.jbang.eclipse.ui.internal.commands;

import static dev.jbang.eclipse.core.internal.ProjectUtils.addJBangNature;
import static dev.jbang.eclipse.core.internal.ProjectUtils.isJBangProject;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import dev.jbang.eclipse.ui.Activator;

public class AddJBangNatureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof StructuredSelection) {
            List<?> elements = ((StructuredSelection) selection).toList();
            Set<IProject> projects = collectProjects(elements);
            if (projects.isEmpty()) {
            	return null;
            }
            new ConfigJob(projects).schedule();
        }
		return null;
	}

    private Set<IProject> collectProjects(List<?> elements) {
        Set<IProject> projects = new LinkedHashSet<>();
        for (Object element : elements) {
            IProject project = Adapters.adapt(element, IProject.class);
            if (project != null && !isJBangProject(project) && project.getLocation() != null) {
                projects.add(project);
            }
        }
        return projects;
    }  
    
    private static class ConfigJob extends Job {

		private Collection<IProject> projects;

		public ConfigJob(Collection<IProject> projects) {
			super("Enable JBang");
			this.projects = projects;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
            for (IProject project : projects) {
            	if (monitor.isCanceled()) {
            		return Status.CANCEL_STATUS;
            	}
				try {
					addJBangNature(project, monitor);
				} catch (CoreException e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
				}
			}
			return Status.OK_STATUS;
		}
    	
    }
}
