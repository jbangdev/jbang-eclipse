/*******************************************************************************
 * Copyright (c) 2022 JRE Lookup project contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fred Bricon - initial API and implementation
 *******************************************************************************/
package dev.jbang.eclipse.core.internal.vms;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;

/**
 * Copied from https://github.com/sidespin/jre-discovery/blob/015336958a5d234db87086ede23d8ca156478491/io.sidespin.jre.discovery.core/src/main/java/io/sidespin/eclipse/jre/discovery/core/internal/ManagedVMService.java
 * @author Fred Bricon
 */
public class JBangManagedVMService {

	private static final String RELEASE_FILE = "release"; //$NON-NLS-1$
	private static final String IMPLEMENTOR_VERSION = "IMPLEMENTOR_VERSION"; //$NON-NLS-1$
	private static final String IMPLEMENTOR = "IMPLEMENTOR"; //$NON-NLS-1$
	private static final String JVM_VERSION = "JVM_VERSION"; //$NON-NLS-1$
	private static final String JAVA_VERSION = "JAVA_VERSION";//$NON-NLS-1$
	private static final Pattern VERSION_PATTERN = Pattern.compile(".*(\\d).*"); //$NON-NLS-1$
	private static final Pattern NOT_VERSION_PATTERN = Pattern.compile(".*-(\\d{4,})"); //$NON-NLS-1$

	public IVMInstall getJVM(File availableJdkPath, IProgressMonitor monitor) throws CoreException {
		var existingVm = findVM(availableJdkPath);
		if (existingVm != null) {
			// Already exists
			return existingVm;
		}
		IVMInstallType type = identifyVMType(availableJdkPath, monitor);
		if (type == null) { // unlikely to happen but, you never know
			return null;
		}

		return createVM(availableJdkPath, type, monitor);
	}

	public IVMInstall createVM(File directory, IVMInstallType type, IProgressMonitor monitor) throws CoreException {
		var status = type.validateInstallLocation(directory);
		if (!status.isOK()) { // No longer valid, we bail
			return null;
		}
		var existingVm = findVM(directory);
		if (existingVm != null) {
			// Already exists
			return existingVm;
		}
		var vmId = generateUniqueId(type);
		var vmStandin = new VMStandin(type, vmId);
		var name = getName("[JBang]", directory);
		vmStandin.setName(name);
		vmStandin.setInstallLocation(directory);
		IVMInstall vm = vmStandin.convertToRealVM();
		JavaRuntime.saveVMConfiguration();
		return vm;
	}

	/**
	 * @param prefix
	 * @param directory
	 * @return
	 */
	private String getName(String prefix, File directory) {
		var release = readRelease(directory);
		var name = release.get(IMPLEMENTOR_VERSION);
		if (name != null && NOT_VERSION_PATTERN.matcher(name).matches()) {
			name = release.get(IMPLEMENTOR);
		}
		if (name == null) {
			name = directory.getName();
			if ("Home".equals(name) && directory.getParentFile() != null
					&& "Contents".equals(directory.getParentFile().getName())
					&& directory.getParentFile().getParentFile() != null) {
				// looking for parent of Content/Home
				name = directory.getParentFile().getParentFile().getName();
			}
		} else {
			Matcher hasVersion = VERSION_PATTERN.matcher(name);
			if (!hasVersion.matches()) {
				var version = release.get(JVM_VERSION);
				if (version == null) {
					version = release.get(JAVA_VERSION);
				}
				if (version != null) {
					name = name + "-" + version;
				}
			}
		}
		return prefix + ' ' + name;
	}

	private Map<String, String> readRelease(File directory) {
		Map<String, String> releaseProps = new LinkedHashMap<>();
		Path releasePath = Paths.get(directory.getAbsolutePath(), RELEASE_FILE);
		if (Files.notExists(releasePath)) {
			return releaseProps;
		}
		FileReader reader;
		try {
			reader = new FileReader(releasePath.toFile());
			Properties p = new Properties();
			p.load(reader);
			Enumeration<?> keys = p.propertyNames();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String value = p.getProperty(key);
				if (value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				releaseProps.put(key, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return releaseProps;
	}

	private String generateUniqueId(IVMInstallType type) {
		long unique = System.currentTimeMillis();
		while (type.findVMInstall(String.valueOf(unique)) != null) {
			unique++;
		}
		return String.valueOf(unique);
	}

	public IVMInstall findVM(File file) {
		if (file == null) {
			return null;
		}
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			IVMInstall[] installs = type.getVMInstalls();
			for (IVMInstall install : installs) {
				if (file.equals(install.getInstallLocation())) {
					return install;
				}
			}
		}
		return null;
	}


	protected IVMInstallType identifyVMType(File file, IProgressMonitor monitor) {
		if (file == null || !file.isDirectory()) {
			return null;
		}
		IVMInstallType[] vmTypes = JavaRuntime.getVMInstallTypes();

		// Take the first VM install type that claims the location as a
		// valid VM install. VM install types should be smart enough to not
		// claim another type's VM, but just in case...
		for (IVMInstallType type : vmTypes) {
			if (monitor.isCanceled()) {
				return null;
			}
			IStatus status = type.validateInstallLocation(file);
			if (status.isOK()) {
				return type;
			}
		}
		return null;
	}

}
