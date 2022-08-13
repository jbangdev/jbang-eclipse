package dev.jbang.eclipse.core.internal.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class JBangRuntimesDiscoveryJob extends Job {

	private Map<String, Path> usualSuspects; 
	private JBangRuntimeManager runtimeManager;
	
	public JBangRuntimesDiscoveryJob(JBangRuntimeManager runtimeManager) {
		super("JBang Discovery");
		usualSuspects = new LinkedHashMap<>();
		usualSuspects.put("Default JBang", Path.of(System.getProperty("user.home"), ".jbang"));
		usualSuspects.put("Current SDKMan", Path.of(System.getProperty("user.home"), ".sdkman", "candidates", "jbang", "current"));
		this.runtimeManager = runtimeManager;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (runtimeManager.getDefaultRuntime().isValid()) {
			return Status.OK_STATUS;
		}
		var existingRuntimes = new ArrayList<>(runtimeManager.getJBangRuntimes(false).stream().filter(r -> !JBangRuntime.SYSTEM.equals(r.getName())).collect(Collectors.toList()));
		
		var newRuntimes = new ArrayList<JBangRuntime>(usualSuspects.size());
		
		for (var candidate : usualSuspects.entrySet()) {
			if (!Files.isDirectory(candidate.getValue())) {
				continue;
			}
			JBangRuntime runtime = new JBangRuntime(candidate.getKey(), candidate.getValue().toString());
			if (!existingRuntimes.contains(runtime) && runtime.isValid()) {
				runtime.detectVersion(monitor);
				newRuntimes.add(runtime);
			}
		}
		if (newRuntimes.size() == 0) {
			return Status.OK_STATUS;
		}
		newRuntimes.sort((j1, j2) -> {
			return Long.compare(j1.getExecutable().toFile().lastModified(), j2.getExecutable().toFile().lastModified());
		});
		existingRuntimes.addAll(newRuntimes);
		runtimeManager.setRuntimes(existingRuntimes);
		var defaultRuntime = newRuntimes.get(0);// Most recent JBang runtime
		System.out.println("Setting default runtime as "+defaultRuntime);
		
		runtimeManager.setDefaultRuntime(defaultRuntime);
		return Status.OK_STATUS;
	}

}
