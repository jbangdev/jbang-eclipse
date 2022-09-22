/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package dev.jbang.eclipse.core.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public final class StatusFactory {

	private StatusFactory() {}

	public static IStatus newErrorStatus(String message) {
		return newErrorStatus(message, null);
	}

	public static IStatus newErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, JBangConstants.PLUGIN_ID, message, exception);
	}
}
