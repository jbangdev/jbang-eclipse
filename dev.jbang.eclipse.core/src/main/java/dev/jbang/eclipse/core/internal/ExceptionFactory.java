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

import org.eclipse.core.runtime.CoreException;

/**
 *
 * @author snjeza
 *
 */
public class ExceptionFactory {

	private ExceptionFactory() {
	}

	public static CoreException newException(String message) {
		return new CoreException(StatusFactory.newErrorStatus(message));
	}

	public static CoreException newException(Throwable e) {
		return newException(e.getMessage(), e);
	}

	public static CoreException newException(String message, Throwable e) {
		return new CoreException(StatusFactory.newErrorStatus(message, e));
	}

}

