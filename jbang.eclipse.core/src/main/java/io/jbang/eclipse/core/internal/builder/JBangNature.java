package io.jbang.eclipse.core.internal.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.jbang.eclipse.core.internal.JBangConstants;

public class JBangNature implements IProjectNature {

	private IProject project;

	@Override
	public void configure() throws CoreException {
		addJBangBuilder(project, new NullProgressMonitor());

	}

	@Override
	public void deconfigure() throws CoreException {
		removeJBangBuilder(project, new NullProgressMonitor());

	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	public boolean addJBangBuilder(IProject project, IProgressMonitor monitor) throws CoreException {
		if (!project.isOpen()) {
			return false;
		}
		IProjectDescription description = project.getDescription();
		List<ICommand> builders = new ArrayList<>(Arrays.asList(description.getBuildSpec()));
		if (builders.stream().anyMatch(JBangNature::isJBangBuilder)) {
			// JBangBuilder already present
			return false;
		}

		ICommand jbangBuilder = description.newCommand();
		jbangBuilder.setBuilderName(JBangConstants.BUILDER_ID);
		builders.add(0, jbangBuilder);
		description.setBuildSpec(builders.toArray(new ICommand[builders.size()]));
		project.setDescription(description, monitor);
		return true;
	}

	public boolean removeJBangBuilder(IProject project, IProgressMonitor monitor) throws CoreException {
		if (project.isOpen()) {
			return false;
		}
		IProjectDescription description = project.getDescription();

		ICommand[] builders = description.getBuildSpec();
		ICommand[] newBuilders = Stream.of(builders).filter(b -> !isJBangBuilder(b)).toArray(s -> new ICommand[s]);
		if (builders.length == newBuilders.length) {
			// Nothing to remove
			return false;
		}

		description.setBuildSpec(newBuilders);
		project.setDescription(description, monitor);
		return true;
	}

	private static boolean isJBangBuilder(ICommand command) {
		return command != null && JBangConstants.BUILDER_ID.equals(command.getBuilderName());
	}
}
