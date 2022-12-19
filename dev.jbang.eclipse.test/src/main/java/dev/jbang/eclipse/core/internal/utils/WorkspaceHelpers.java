/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package dev.jbang.eclipse.core.internal.utils;

import static dev.jbang.eclipse.core.internal.ExceptionFactory.newException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import dev.jbang.eclipse.core.internal.JBangConstants;

/**
 * Copied from
 * https://raw.githubusercontent.com/eclipse-m2e/m2e-core/master/org.eclipse.m2e.tests.common/src/org/eclipse/m2e/tests/common/WorkspaceHelpers.java
 *
 */
public class WorkspaceHelpers {

  public static void cleanWorkspace() throws InterruptedException, CoreException {
    Exception cause = null;
    int i;
    for(i = 0; i < 10; i++ ) {
      try {
        System.gc();
        doCleanWorkspace();
      } catch(InterruptedException | OperationCanceledException e) {
        throw e;
      } catch(Exception e) {
        cause = e;
        e.printStackTrace();
        System.out.println(i);
        Thread.sleep(6 * 1000);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw newException ("Could not delete workspace resources (after " + i
            + " retries): " + Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()), cause);
  }

  private static void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run((IWorkspaceRunnable) monitor -> {
      IProject[] projects = workspace.getRoot().getProjects();
      for(IProject project : projects) {
        project.delete(true, true, monitor);
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete(new NullProgressMonitor());
   }

  public static String toString(IMarker... markers) {
    if(markers != null) {
      return toString(Arrays.asList(markers));
    }
    return "";
  }

  public static String toString(List<IMarker> markers) {
    return markers.stream().map(WorkspaceHelpers::toString).collect(Collectors.joining(","));
  }

  protected static String toString(IMarker marker) {
    try {
      return "Type=" + marker.getType() + ":Message=" + marker.getAttribute(IMarker.MESSAGE) + ":LineNumber="
          + marker.getAttribute(IMarker.LINE_NUMBER);
    } catch(CoreException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static List<IMarker> findMarkers(IResource resource, int targetSeverity) throws CoreException {
    return findMarkers(resource, targetSeverity, null /*withAttribute*/);
  }

  public static List<IMarker> findMarkers(IResource resource, int targetSeverity, String withAttribute)
      throws CoreException {
    Set<IMarker> errors = new TreeSet<>(Comparator.<IMarker> comparingInt(o -> o.getAttribute(IMarker.LINE_NUMBER, -1))
        .thenComparing(o -> o.getAttribute(IMarker.MESSAGE, "")));
    for(IMarker marker : resource.findMarkers(null /* all markers */, true /* subtypes */, IResource.DEPTH_INFINITE)) {
      int severity = marker.getAttribute(IMarker.SEVERITY, 0);
      if(targetSeverity >= 0 && severity != targetSeverity) {
        continue;
      }
      if(withAttribute != null) {
        String attribute = marker.getAttribute(withAttribute, null);
        if(attribute == null) {
          continue;
        }
      }
      errors.add(marker);
    }
    List<IMarker> result = new ArrayList<>();
    result.addAll(errors);
    return result;
  }

  public static List<IMarker> findWarningMarkers(IResource resource) throws CoreException {
    return findMarkers(resource, IMarker.SEVERITY_WARNING);
  }

  public static List<IMarker> findErrorMarkers(IResource resource) throws CoreException {
    return findMarkers(resource, IMarker.SEVERITY_ERROR);
  }

  public static void assertNoErrors(IResource resource) throws CoreException {
    List<IMarker> markers = findErrorMarkers(resource);
    assertEquals( 0, markers.size(), "Unexpected error markers " + toString(markers));
  }

  public static void assertNoWarnings(IResource resource) throws CoreException {
    List<IMarker> markers = findWarningMarkers(resource);
    assertEquals(0, markers.size(), "Unexpected warning markers " + toString(markers));
  }

  public static IMarker assertErrorMarker(String type, String message, Integer lineNumber, String resourceRelativePath,
      IProject project) throws Exception {
    return assertMarker(type, IMarker.SEVERITY_ERROR, message, lineNumber, resourceRelativePath, project);
  }

  public static IMarker assertWarningMarker(String type, String message, Integer lineNumber,
      String resourceRelativePath, IProject project) throws Exception {
    return assertMarker(type, IMarker.SEVERITY_WARNING, message, lineNumber, resourceRelativePath, project);
  }

  private static IMarker findMarker(String type, String message, Integer lineNumber, String resourceRelativePath,
      List<IMarker> markers) throws Exception {
    for(IMarker marker : markers) {
      if((type != null && !type.equals(marker.getType())) || (message != null && !marker.getAttribute(IMarker.MESSAGE, "").startsWith(message))) {
        continue;
      }
      if(lineNumber != null && !lineNumber.equals(marker.getAttribute(IMarker.LINE_NUMBER))) {
        continue;
      }
      if(type != null && type.startsWith(JBangConstants.MARKER_ID)) {
        //assertEquals(false, marker.getAttribute(IMarker.TRANSIENT), "Marker not persistent:" + toString(marker));
      }

      if(resourceRelativePath == null) {
        resourceRelativePath = "";
      }
      assertEquals(resourceRelativePath,
          marker.getResource().getProjectRelativePath().toString(), "Marker not on the expected resource:" + toString(marker));

      return marker;
    }
    return null;
  }

  public static IMarker assertMarker(String type, int severity, String message, Integer lineNumber,
      String resourceRelativePath, IResource resource) throws Exception {
    List<IMarker> markers = findMarkers(resource, severity);
    IMarker marker = findMarker(type, message, lineNumber, resourceRelativePath, markers);
    if(marker == null) {
      fail(
          "Expected marker not found. Found " + (markers.isEmpty() ? "no markers" : "markers :") + toString(markers));
    }
    assertTrue(marker.isSubtypeOf(IMarker.PROBLEM), "Marker type " + type + " is not a subtype of " + IMarker.PROBLEM);
    return marker;
  }

}