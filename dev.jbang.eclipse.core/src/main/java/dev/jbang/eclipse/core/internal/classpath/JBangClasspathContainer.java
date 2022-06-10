package dev.jbang.eclipse.core.internal.classpath;
import java.io.Serializable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;


/**
 * JBang classpath container
 */
public class JBangClasspathContainer implements IClasspathContainer, Serializable {
  private static final long serialVersionUID = -5976726121300869771L;

  private final IClasspathEntry[] entries;

  private final IPath path;

  public JBangClasspathContainer(IPath path, IClasspathEntry[] entries) {
    this.path = path;
    this.entries = entries;
  }

  @Override
  public String getDescription() {
    return "JBang Dependencies";
  }

  @Override
  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  @Override
  public synchronized IClasspathEntry[] getClasspathEntries() {
    return entries;
  }

  @Override
  public IPath getPath() {
    return path;
  }

}