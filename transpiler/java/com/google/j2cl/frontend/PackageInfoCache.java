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
package com.google.j2cl.frontend;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.j2cl.errors.Errors;

import jsinterop.annotations.JsPackage;

import org.eclipse.jdt.core.dom.IAnnotationBinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 *
 * <p>
 * The cached information comes from two sources. It can either be extracted from previously
 * compiled package-info.class files in the classpath or from package-info.java sources that are
 * part of the current compile.
 *
 * <p>
 * Requests are answered against the package-info file in the same package and same class path entry
 * as the queried about type. To do anything else would lead to inconsistent answers when multiple
 * class path entries contain the same packages but with different package info files, depending on
 * what arbitrary set of class path entries do or do not happen to be included in the current
 * compile.
 */
public class PackageInfoCache {

  /**
   * Encapsulates all that is known about a particular package in a particular class path entry.
   */
  private static class PackageReport {
    private String jsNamespace = null;

    // TODO: turn into a nullability enum.
    private boolean nullabilityEnabled = false;
  }

  public static final String SOURCE_CLASS_PATH_ENTRY = "[source]";

  /**
   * When nothing is known about a particular package in a particular class path entry the answers
   * to questions about package properties are taken from this instance.
   */
  private static final PackageReport defaultPackageReport = new PackageReport();

  /**
   * Allows for the initialization/retrieval of one shared PackageInfoCache instance per thread.
   */
  private static final ThreadLocal<PackageInfoCache> packageInfoCacheStorage = new ThreadLocal<>();

  @VisibleForTesting
  public static void clear() {
    packageInfoCacheStorage.remove();
  }

  public static PackageInfoCache get() {
    return Preconditions.checkNotNull(packageInfoCacheStorage.get());
  }

  public static void init(List<String> classPathEntries, Errors errors) {
    Preconditions.checkState(
        packageInfoCacheStorage.get() == null,
        "PackageInfoCache should only be initialized once per thread.");

    // Constructs a URLClassLoader to do the dirty work of finding package-info.class files in the
    // classpath of the compile.
    List<URL> classPathUrls = new ArrayList<>();
    for (String classPathEntry : classPathEntries) {
      try {
        classPathUrls.add(new URL("file:" + classPathEntry));
      } catch (MalformedURLException e) {
        errors.error(Errors.Error.ERR_CANNOT_OPEN_FILE, classPathEntry);
      }
    }
    URLClassLoader resourcesClassLoader =
        new URLClassLoader(
            Iterables.toArray(classPathUrls, URL.class), PackageInfoCache.class.getClassLoader());

    packageInfoCacheStorage.set(new PackageInfoCache(resourcesClassLoader, errors));
  }

  private static PackageReport toPackageReport(Annotation[] packageAnnotations) {
    PackageReport packageReport = new PackageReport();
    if (packageAnnotations != null) {
      for (Annotation packageAnnotation : packageAnnotations) {
        if (packageAnnotation instanceof JsPackage) {
          packageReport.jsNamespace = ((JsPackage) packageAnnotation).namespace();
        }

        Class<? extends Annotation> annotationType = packageAnnotation.annotationType();
        if (annotationType.getSimpleName().equals("AnnotatedFor")) {
          try {
            Method valueMethod = annotationType.getDeclaredMethod("value");
            Object result = valueMethod.invoke(packageAnnotation);
            if (result instanceof String[]) {
              if (Arrays.asList((String[]) result).contains("nullness")) {
                packageReport.nullabilityEnabled = true;
              }
            }
          } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Not the annotation we are looking for.
          }
        }
      }
    }
    return packageReport;
  }

  private static PackageReport toPackageReport(
      Iterable<org.eclipse.jdt.core.dom.Annotation> packageAnnotations) {
    PackageReport packageReport = new PackageReport();
    if (packageAnnotations != null) {
      for (org.eclipse.jdt.core.dom.Annotation packageAnnotation : packageAnnotations) {
        IAnnotationBinding annotationBinding = packageAnnotation.resolveAnnotationBinding();
        if (JsInteropAnnotationUtils.isJsPackageAnnotation(annotationBinding)) {
          packageReport.jsNamespace = JsInteropAnnotationUtils.getJsNamespace(annotationBinding);
        }

        if (annotationBinding.getAnnotationType().getName().equals("AnnotatedFor")) {
          Object[] value =
              JdtAnnotationUtils.getAnnotationParameterArray(annotationBinding, "value");
          if (value != null && Arrays.asList(value).contains("nullness")) {
            packageReport.nullabilityEnabled = true;
          }
        }
      }
    }
    return packageReport;
  }

  private static String toSpecificPackagePath(String classPathEntry, String packagePath) {
    return classPathEntry + ":" + packagePath;
  }

  private final Errors errors;
  private final Map<String, PackageReport> packageReportBySpecificPackagePath = new HashMap<>();
  private final Map<String, PackageReport> packageReportByTypeName = new HashMap<>();
  private final ClassLoader resourcesClassLoader;

  private PackageInfoCache(ClassLoader resourcesClassLoader, Errors errors) {
    this.resourcesClassLoader = resourcesClassLoader;
    this.errors = errors;
  }

  /**
   * Returns the JsNamespace for the given type, which must be a top level type and referenced by
   * fully qualified source name.
   */
  public String getJsNamespace(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).jsNamespace;
  }

  /**
   * Returns whether nullability is enabled for the given type, which must be a top level type and
   * referenced by fully qualified source name.
   */
  public boolean isNullabilityEnabled(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).nullabilityEnabled;
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
   * Interpret the given annotations as info about a specific package (as identified by the
   * combination of class path entry and package path).
   */
  public void setInfo(
      String classPathEntry,
      String packagePath,
      List<org.eclipse.jdt.core.dom.Annotation> packageAnnotations) {
    setReportForPackage(classPathEntry, packagePath, toPackageReport(packageAnnotations));
  }

  @SuppressWarnings({"resource", "unused"})
  private Annotation[] findBytecodePackageAnnotations(String classPathEntry, String packagePath) {
    Preconditions.checkNotNull(classPathEntry);
    String packageInfoRelativeFilePath = packagePath.replace('.', '/') + "/package-info.class";
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
      errors.error(Errors.Error.ERR_PACKAGE_INFO_PARSE, packageInfoRelativeFilePath);
    } catch (MalformedURLException e) {
      errors.error(Errors.Error.ERR_CLASS_PATH_URL, classPathEntry);
    }
    return annotations;
  }

  /**
   * Returns the first classpath entry that provides class file for the given type.
   */
  private String findOriginClassPathEntry(String typeName) {
    String classFilePath = typeName.replace(".", "/") + ".class";

    URL typeResource = resourcesClassLoader.getResource(classFilePath);
    if (typeResource == null) {
      return null;
    } else {
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

  private String getPackagePath(String topLevelTypeSourceName) {
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
    String packagePath = getPackagePath(topLevelTypeSourceName);
    String specificPackagePath = toSpecificPackagePath(originClassPathEntry, packagePath);

    if (originClassPathEntry == null) {
      return defaultPackageReport;
    }

    if (!packageReportBySpecificPackagePath.containsKey(specificPackagePath)) {
      parsePackageInfo(originClassPathEntry, packagePath, topLevelTypeSourceName);
    }

    if (packageReportBySpecificPackagePath.containsKey(specificPackagePath)) {
      PackageReport packageReport = packageReportBySpecificPackagePath.get(specificPackagePath);
      packageReportByTypeName.put(topLevelTypeSourceName, packageReport);
      return packageReport;
    }

    return defaultPackageReport;
  }

  private void parsePackageInfo(
      String classPathEntry, String packagePath, String topLevelTypeSourceName) {
    Annotation[] packageAnnotations = findBytecodePackageAnnotations(classPathEntry, packagePath);
    PackageReport packageReport = toPackageReport(packageAnnotations);

    setReportForPackage(classPathEntry, packagePath, packageReport);
    propagateSpecificInfo(classPathEntry, topLevelTypeSourceName);
  }

  /**
   * Propagate cached info about a specific package path (which is a combination of a class path
   * entry and a package path) to apply to an exact type name. This saves the cost of needing to do
   * an origin class path check on the given type every time info about it is requested.
   */
  private void propagateSpecificInfo(String classPathEntry, String topLevelTypeSourceName) {
    PackageReport packageReport =
        packageReportBySpecificPackagePath.get(
            toSpecificPackagePath(classPathEntry, getPackagePath(topLevelTypeSourceName)));
    if (packageReport == null) {
      packageReport = defaultPackageReport;
    }

    packageReportByTypeName.put(topLevelTypeSourceName, packageReport);
  }

  private void setReportForPackage(
      String classPathEntry, String packagePath, PackageReport packageReport) {
    if (packageReport == null) {
      packageReport = defaultPackageReport;
    }
    String specificPackagePath = toSpecificPackagePath(classPathEntry, packagePath);
    packageReportBySpecificPackagePath.put(specificPackagePath, packageReport);
  }
}
