/*******************************************************************************
 * Copyright (c) 2012-2019 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package dev.jbang.eclipse.core.internal.classpath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import dev.jbang.eclipse.core.internal.classpath.AnnotationServiceLocator.ServiceEntry;


/**
 * AnnotationProcessorUtils
 *
 * @author Fred Bricon
 * 
 * copied from https://github.com/eclipse-m2e/m2e-core/blob/bb14b75bfa14a7548fd59707965e3e281c7bb415/org.eclipse.m2e.apt.core/src/org/eclipse/m2e/apt/internal/utils/ProjectUtils.java
 */
public class AnnotationProcessorUtils {
  private AnnotationProcessorUtils() {
  }

  private static final Pattern OPTION_PATTERN = Pattern.compile("-A([^ \\t\"']+)");

  /**
   * Parse a string to extract Annotation Processor options
   */
  public static Map<String, String> parseProcessorOptions(String compilerArgument) {

    if((compilerArgument == null) || compilerArgument.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> ret = new HashMap<>();

    Matcher matcher = OPTION_PATTERN.matcher(compilerArgument);

    int start = 0;
    while(matcher.find(start)) {
      String argument = matcher.group(1);
      parse(argument, ret);
      start = matcher.end();
    }

    return ret;
  }

  private static void parse(String argument, Map<String, String> results) {
    if(argument == null || argument.isBlank()) {
      return;
    }
    String key;
    String value;
    int optionalEqualsIndex = argument.indexOf('=');
    switch(optionalEqualsIndex) {
      case -1: { // -Akey : ok
        key = argument;
        value = null;
        break;
      }
      case 0: { // -A=value : invalid
        return;
      }
      default: {
        key = argument.substring(0, optionalEqualsIndex);
        if(containsWhitespace(key)) { // -A key = value : invalid
          return;
        }
        value = argument.substring(optionalEqualsIndex + 1, argument.length());
      }
    }
    results.put(key, value);
  }

  public static Map<String, String> parseProcessorOptions(List<String> compilerArgs) {
    if((compilerArgs == null) || compilerArgs.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> options = new HashMap<>();

    for(String arg : compilerArgs) {
      if(arg != null && arg.startsWith("-A")) {
        parse(arg.substring(2), options);
      }
    }
    return options;
  }

  /**
   * Extract Annotation Processor options from a compiler-argument map
   */
  public static Map<String, String> extractProcessorOptions(Map<String, String> compilerArguments) {
    if((compilerArguments == null) || compilerArguments.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> ret = new HashMap<>(compilerArguments.size());

    for(Map.Entry<String, String> argument : compilerArguments.entrySet()) {
      String key = argument.getKey();

      if(key.startsWith("A")) {
        String value = argument.getValue();
        if((value != null) && (value.length() > 0)) {
          ret.put(key.substring(1), value);
        } else {
          ret.put(key.substring(1), null);
        }
      }
    }

    return ret;
  }

  /**
   * Validates that the name of a processor option conforms to the grammar defined by
   * <code>javax.annotation.processing.Processor.getSupportedOptions()</code>.
   *
   * @param optionName
   * @return <code>true</code> if the name conforms to the grammar, <code>false</code> if not.
   */
  public static boolean isValidOptionName(String optionName) {
    if(optionName == null) {
      return false;
    }

    boolean startExpected = true;
    int codePoint;

    for(int i = 0; i < optionName.length(); i += Character.charCount(codePoint)) {
      codePoint = optionName.codePointAt(i);

      if(startExpected) {
        if(!Character.isJavaIdentifierStart(codePoint)) {
          return false;
        }

        startExpected = false;

      } else {
        if(codePoint == '.') {
          startExpected = true;

        } else if(!Character.isJavaIdentifierPart(codePoint)) {
          return false;
        }
      }
    }

    return !startExpected;
  }

  /**
   * Converts the specified relative or absolute {@link File} to a {@link File} that is relative to the base directory
   * of the specified {@link IProject}.
   *
   * @param project the {@link IProject} whose base directory the returned {@link File} should be relative to
   * @param fileToConvert the relative or absolute {@link File} to convert
   * @return a {@link File} that is relative to the base directory of the specified {@link IProject}
   */
  public static File convertToProjectRelativePath(IProject project, File fileToConvert) {
    // Get an absolute version of the specified file
    File absoluteFile = fileToConvert.getAbsoluteFile();
    String absoluteFilePath = absoluteFile.getAbsolutePath();

    // Get a File for the absolute path to the project's directory
    File projectBasedirFile = project.getLocation().toFile().getAbsoluteFile();
    String projectBasedirFilePath = projectBasedirFile.getAbsolutePath();

    // Compute the relative path
    if(absoluteFile.equals(projectBasedirFile)) {
      return new File(".");
    } else if(absoluteFilePath.startsWith(projectBasedirFilePath)) {
      String projectRelativePath = absoluteFilePath.substring(projectBasedirFilePath.length() + 1);
      return new File(projectRelativePath);
    } else {
      return absoluteFile;
    }
  }


  /**
   * Returns <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   * <code>false</code> if none of them do.
   *
   * @param resolvedJarArtifacts the JAR {@link File}s to inspect for annotation processors
   * @return <code>true</code> if any of the specified JARs contain a Java 5 or Java 6 annotation processor,
   *         <code>false</code> if none of them do
   */
  public static boolean containsAptProcessors(Collection<File> resolvedJarArtifacts) {
    // Read through all JARs, checking for any APT service entries
    try {
      for(File resolvedJarArtifact : resolvedJarArtifacts) {
        Set<ServiceEntry> aptServiceEntries = AnnotationServiceLocator.getAptServiceEntries(resolvedJarArtifact);
        if(!aptServiceEntries.isEmpty()) {
          return true;
        }
      }
    } catch(IOException e) {
      Platform.getLog(AnnotationServiceLocator.class).log(Status.error("Error while reading artifact JARs.", e));
    }
    // No service entries were found
    return false;
  }

  public static boolean isJar(File file) {
  	return file.isFile() && file.getAbsolutePath().endsWith(".jar");
  }

  private static boolean containsWhitespace(String seq) {
    return seq != null && !seq.isBlank() && seq.chars().anyMatch(Character::isWhitespace);
  }
}