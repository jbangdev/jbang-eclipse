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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

/**
 * 
 * Copied from https://raw.githubusercontent.com/eclipse-m2e/m2e-core/master/org.eclipse.m2e.tests.common/src/org/eclipse/m2e/tests/common/JobHelpers.java
 */
@SuppressWarnings("restriction")
public class JobHelpers {

  private static final int POLLING_DELAY = 10;

  public static void waitForJobsToComplete() {
    try {
      waitForJobsToComplete(new NullProgressMonitor());
    } catch(Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void waitForJobsToComplete(IProgressMonitor monitor) throws InterruptedException, CoreException {
    waitForJobsToComplete(60_000, monitor);
  }

  public static void waitForJobsToComplete(int buildTimeoutMilliseconds, IProgressMonitor monitor)
      throws InterruptedException, CoreException {
    waitForBuildJobs(buildTimeoutMilliseconds);

    /*
     * First, make sure refresh job gets all resource change events
     *
     * Resource change events are delivered after WorkspaceJob#runInWorkspace returns
     * and during IWorkspace#run. Each change notification is delivered by
     * only one thread/job, so we make sure no other workspaceJob is running then
     * call IWorkspace#run from this thread.
     *
     * Unfortunately, this does not catch other jobs and threads that call IWorkspace#run
     * so we have to hard-code workarounds
     *
     * See http://www.eclipse.org/articles/Article-Resource-deltas/resource-deltas.html
     */
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IJobManager jobManager = Job.getJobManager();
    jobManager.suspend();
    try {
      Job[] jobs = jobManager.find(null);
      for(Job job : jobs) {
        if(job instanceof WorkspaceJob || job.getClass().getName().endsWith("JREUpdateJob")) {
          job.join();
        }
      }
      workspace.run((IWorkspaceRunnable) monitor1 -> {
      }, workspace.getRoot(), 0, monitor);

      
    } finally {
      jobManager.resume();
    }

  }

  private static void waitForBuildJobs(int timeOutMilliseconds) {
    waitForJobs(BuildJobMatcher.INSTANCE, timeOutMilliseconds);
  }

  public static void waitForJobs(IJobMatcher matcher, int maxWaitMillis) {
    final long limit = System.currentTimeMillis() + maxWaitMillis;
    while(true) {
      Job job = getJob(matcher);
      if(job == null) {
        return;
      }
      boolean timeout = System.currentTimeMillis() > limit;
      assertFalse(timeout, "Timeout while waiting for completion of job: " + job);
      job.wakeUp();
      try {
        Thread.sleep(POLLING_DELAY);
      } catch(InterruptedException e) {
        // ignore and keep waiting
      }
    }
  }

  private static Job getJob(IJobMatcher matcher) {
    Job[] jobs = Job.getJobManager().find(null);
    for(Job job : jobs) {
      if(matcher.matches(job)) {
        return job;
      }
    }
    return null;
  }

//  public static void waitForLaunchesToComplete(int maxWaitMillis) {
//    // wait for any jobs that actually start the launch
//    waitForJobs(LaunchJobMatcher.INSTANCE, maxWaitMillis);
//
//    // wait for the launches themselves
//    final long limit = System.currentTimeMillis() + maxWaitMillis;
//    while(true) {
//      ILaunch launch = getActiveLaunch();
//      if(launch == null) {
//        return;
//      }
//      boolean timeout = System.currentTimeMillis() > limit;
//      Assert.assertFalse("Timeout while waiting for completion of launch: " + launch.getLaunchConfiguration(), timeout);
//      try {
//        Thread.sleep(POLLING_DELAY);
//      } catch(InterruptedException e) {
//        // ignore and keep waiting
//      }
//    }
//  }

//  private static ILaunch getActiveLaunch() {
//    ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
//    if(launches != null) {
//      for(ILaunch launch : launches) {
//        if(!launch.isTerminated()) {
//          return launch;
//        }
//      }
//    }
//    return null;
//  }

  public interface IJobMatcher {

    boolean matches(Job job);

  }

  static class LaunchJobMatcher implements IJobMatcher {

    public static final IJobMatcher INSTANCE = new LaunchJobMatcher();

    public boolean matches(Job job) {
      return job.getClass().getName().matches("(.*\\.DebugUIPlugin.*)");
    }

  }

  static class BuildJobMatcher implements IJobMatcher {

    public static final IJobMatcher INSTANCE = new BuildJobMatcher();

    public boolean matches(Job job) {
      return (job instanceof WorkspaceJob) || job.getClass().getName().matches("(.*\\.AutoBuild.*)")
          || job.getClass().getName().endsWith("JREUpdateJob");
    }

  }

}