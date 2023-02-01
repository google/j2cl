/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.frontend.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.stream;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2objc.annotations.ObjectiveCName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import jsinterop.annotations.JsPackage;
import org.jspecify.nullness.NullMarked;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 *
 * <p>The cached information comes from two sources. It can either be extracted from previously
 * compiled package-info.class files in the classpath or from package-info.java sources that are
 * part of the current compile.
 *
 * <p>Requests are answered against the package-info file in the same package and same class path
 * entry as the queried about type. To do anything else would lead to inconsistent answers when
 * multiple class path entries contain the same packages but with different package info files,
 * depending on what arbitrary set of class path entries do or do not happen to be included in the
 * current compile.
 */
public class PackageInfoCache {

  /** Encapsulates all that is known about a particular package in a particular class path entry. */
  @AutoValue
  public abstract static class PackageReport {
    @Nullable
    public abstract String getJsNamespace();

    @Nullable
    public abstract String getObjectiveCName();

    public abstract boolean isNullMarked();

    public static Builder newBuilder() {
      return new AutoValue_PackageInfoCache_PackageReport.Builder().setNullMarked(false);
    }

    /** A Builder for PackageReport. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setJsNamespace(String jsNamespace);

      public abstract Builder setObjectiveCName(String objectiveCName);

      public abstract Builder setNullMarked(boolean isNullMarked);

      abstract PackageReport autoBuild();

      public PackageReport build() {
        return autoBuild();
      }
    }
  }

  public static final String SOURCE_CLASS_PATH_ENTRY = "[source]";

  /**
   * When nothing is known about a particular package in a particular class path entry the answers
   * to questions about package properties are taken from this instance.
   */
  private static final PackageReport DEFAULT_PACKAGE_REPORT = PackageReport.newBuilder().build();

  /** Allows for the initialization/retrieval of one shared PackageInfoCache instance per thread. */
  private static final ThreadLocal<PackageInfoCache> packageInfoCacheStorage = new ThreadLocal<>();

  @VisibleForTesting
  public static void clear() {
    packageInfoCacheStorage.remove();
  }

  public static PackageInfoCache get() {
    return checkNotNull(packageInfoCacheStorage.get());
  }

  public static void init(List<String> classPathEntries, Problems problems) {
    checkState(
        packageInfoCacheStorage.get() == null,
        "PackageInfoCache should only be initialized once per thread.");

    // Constructs a URLClassLoader to do the dirty work of finding package-info.class files in the
    // classpath of the compile.
    List<URL> classPathUrls = new ArrayList<>();
    for (String classPathEntry : classPathEntries) {
      try {
        classPathUrls.add(new URL("file:" + classPathEntry));
      } catch (MalformedURLException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
      }
    }

    // Create a parent-less classloader to make sure it does not load anything from the classpath
    // the compiler is running with. This allows to load supersourced versions of the annotations;
    // however, the classes loaded by this classloader do not share the same class instances and
    // are considered different classes than the ones loaded by the compiler *even* if they load
    // the same classfile. Any manipulation of instances loaded by this classloader need to be done
    // reflectively.
    URLClassLoader resourcesClassLoader =
        new URLClassLoader(Iterables.toArray(classPathUrls, URL.class), null);

    packageInfoCacheStorage.set(new PackageInfoCache(resourcesClassLoader, problems));
  }

  private static String toSpecificPackagePath(String classPathEntry, String packagePath) {
    return classPathEntry + ":" + packagePath;
  }

  private final Problems problems;
  private final Map<String, PackageReport> packageReportBySpecificPackagePath = new HashMap<>();
  private final Map<String, PackageReport> packageReportByTypeName = new HashMap<>();
  private final ClassLoader resourcesClassLoader;

  private PackageInfoCache(ClassLoader resourcesClassLoader, Problems problems) {
    this.resourcesClassLoader = resourcesClassLoader;
    this.problems = problems;
  }

  /**
   * Returns the JsNamespace for the given type, which must be a top level type and referenced by
   * fully qualified source name.
   */
  public String getJsNamespace(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).getJsNamespace();
  }

  /**
   * Returns the ObjectiveCName for the given type which must be a top level type and referenced by
   * its fully qualified source name.
   */
  public String getObjectiveCName(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).getObjectiveCName();
  }

  public boolean isNullMarked(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).isNullMarked();
  }

  /**
   * Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
   * around in the class path to figure it out and it might even come up with the wrong answer. For
   * example if this class has also been globbed into some other library that is a dependency of
   * this one.
   */
  public void markAsSource(String topLevelTypeSourceName) {
    propagateSpecificInfo(SOURCE_CLASS_PATH_ENTRY, topLevelTypeSourceName);
  }

  /**
   * Specify the JavaScript namespace and whether it defines a null marked scope for a given package
   * (as identified by the combination of class path entry and package path).
   */
  public void setPackageProperties(
      String classPathEntry,
      String packagePath,
      String packageJsNamespace,
      String objectiveCName,
      boolean isNullMarked) {
    setReportForPackage(
        classPathEntry,
        packagePath,
        PackageReport.newBuilder()
            .setJsNamespace(packageJsNamespace)
            .setObjectiveCName(objectiveCName)
            .setNullMarked(isNullMarked)
            .build());
  }

  @SuppressWarnings({"resource", "unused"})
  private Annotation[] findBytecodePackageAnnotations(String classPathEntry, String packagePath) {
    checkNotNull(classPathEntry);
    String packageInfoRelativeFilePath =
        OutputUtils.getPackageRelativePath(packagePath, "package-info.class");
    String packageInfoSourceName = packagePath + ".package-info";

    Annotation[] annotations = {};
    try {
      URLClassLoader entryClassLoader =
          new URLClassLoader(new URL[] {new URL(classPathEntry)}, resourcesClassLoader);

      // This find does not recurse up the class loader parent chain. We want to know if *exactly*
      // this class path entry contains the package-info class.
      URL packageInfoResource = entryClassLoader.findResource(packageInfoRelativeFilePath);
      if (packageInfoResource == null) {
        return annotations;
      }

      // This load *does* recurse up the class loader parent chain and this is important to be able
      // to load the annotations that are referenced. They are almost certain to not be available in
      // just the particular target class path entry.
      Class<?> packageInfoClass = entryClassLoader.loadClass(packageInfoSourceName);
      annotations = packageInfoClass.getAnnotations();
    } catch (ClassNotFoundException e) {
      problems.fatal(FatalError.PACKAGE_INFO_PARSE, packageInfoRelativeFilePath);
    } catch (MalformedURLException e) {
      problems.fatal(FatalError.CLASS_PATH_URL, classPathEntry);
    }
    return annotations;
  }

  /** Returns the first classpath entry that provides class file for the given type. */
  @Nullable
  private String findOriginClassPathEntry(String typeName) {
    String classFilePath = typeName.replace('.', '/') + ".class";

    URL typeResource = resourcesClassLoader.getResource(classFilePath);
    if (typeResource == null) {
      return null;
    } else {
      if (typeResource.getProtocol().equals("jrt")) {
        // Java 9 SDK url, ignore for now.
        // TODO(rluble): revisit when compiling under -source 9.
        return null;
      }
      String resourcePath = typeResource.getFile();
      String originClassPathEntry =
          resourcePath.substring(0, resourcePath.length() - classFilePath.length());
      if (originClassPathEntry.endsWith("/")) {
        originClassPathEntry = originClassPathEntry.substring(0, originClassPathEntry.length() - 1);
      }
      if (originClassPathEntry.endsWith("!")) {
        originClassPathEntry = originClassPathEntry.substring(0, originClassPathEntry.length() - 1);
      }
      return originClassPathEntry;
    }
  }

  private String getPackage(String topLevelTypeSourceName) {
    int lastDotIndex = topLevelTypeSourceName.lastIndexOf(".");
    if (lastDotIndex == -1) {
      return topLevelTypeSourceName;
    }
    return topLevelTypeSourceName.substring(0, lastDotIndex);
  }

  private PackageReport getPackageReport(String topLevelTypeSourceName) {
    if (packageReportByTypeName.containsKey(topLevelTypeSourceName)) {
      return packageReportByTypeName.get(topLevelTypeSourceName);
    }

    String originClassPathEntry = findOriginClassPathEntry(topLevelTypeSourceName);
    String packagePath = getPackage(topLevelTypeSourceName);
    String specificPackagePath = toSpecificPackagePath(originClassPathEntry, packagePath);

    if (originClassPathEntry == null) {
      return DEFAULT_PACKAGE_REPORT;
    }

    if (!packageReportBySpecificPackagePath.containsKey(specificPackagePath)) {
      parsePackageInfo(originClassPathEntry, packagePath, topLevelTypeSourceName);
    }

    if (packageReportBySpecificPackagePath.containsKey(specificPackagePath)) {
      PackageReport packageReport = packageReportBySpecificPackagePath.get(specificPackagePath);
      packageReportByTypeName.put(topLevelTypeSourceName, packageReport);
      return packageReport;
    }

    return DEFAULT_PACKAGE_REPORT;
  }

  private void parsePackageInfo(
      String classPathEntry, String packagePath, String topLevelTypeSourceName) {
    
    Annotation[] packageAnnotations = findBytecodePackageAnnotations(classPathEntry, packagePath);

    setPackageProperties(
        classPathEntry,
        packagePath,
        getPackageJsNamespace(packageAnnotations),
        getPackageObjectiveCName(packageAnnotations),
        hasNullMarkedAnnotation(packageAnnotations));
    propagateSpecificInfo(classPathEntry, topLevelTypeSourceName);
  }

  @Nullable
  private static String getPackageJsNamespace(Annotation[] packageAnnotations) {
    return getAnnotationField(packageAnnotations, JsPackage.class, "namespace");
  }

  @Nullable
  private static String getPackageObjectiveCName(Annotation[] packageAnnotations) {
    return getAnnotationField(packageAnnotations, ObjectiveCName.class, "value");
  }

  private static boolean hasNullMarkedAnnotation(Annotation[] packageAnnotations) {
    return getAnnotation(packageAnnotations, NullMarked.class) != null;
  }

  /**
   * Finds an annotation and retrieves a field value via reflection.
   *
   * <p>Note that reflection needs to be used to access these annotations since they are loaded
   * using a different classloader that does not share state with the classloader the compiler is
   * running with.
   */
  @Nullable
  private static String getAnnotationField(
      Annotation[] annotations, Class<?> annotationClass, String field) {
    Annotation annotation = getAnnotation(annotations, annotationClass);
    if (annotation == null) {
      return null;
    }

    try {
      Method fieldAccessor = annotation.annotationType().getMethod(field);
      return (String) fieldAccessor.invoke(annotation);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  private static Annotation getAnnotation(Annotation[] annotations, Class<?> annotationClass) {
    if (annotations == null) {
      return null;
    }
    return stream(annotations)
        .filter(a -> a.annotationType().getName().equals(annotationClass.getName()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Propagate cached info about a specific package path (which is a combination of a class path
   * entry and a package path) to apply to an exact type name. This saves the cost of needing to do
   * an origin class path check on the given type every time info about it is requested.
   */
  private void propagateSpecificInfo(String classPathEntry, String topLevelTypeSourceName) {
    PackageReport packageReport =
        packageReportBySpecificPackagePath.getOrDefault(
            toSpecificPackagePath(classPathEntry, getPackage(topLevelTypeSourceName)),
            DEFAULT_PACKAGE_REPORT);

    packageReportByTypeName.put(topLevelTypeSourceName, packageReport);
  }

  private void setReportForPackage(
      String classPathEntry, String packagePath, PackageReport packageReport) {
    if (packageReport == null) {
      packageReport = DEFAULT_PACKAGE_REPORT;
    }
    String specificPackagePath = toSpecificPackagePath(classPathEntry, packagePath);
    packageReportBySpecificPackagePath.put(specificPackagePath, packageReport);
  }
}
