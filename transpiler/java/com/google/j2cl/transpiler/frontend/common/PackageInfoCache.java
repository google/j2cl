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
import com.google.common.collect.Iterables;
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
import org.jspecify.annotations.NullMarked;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
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

  /**
   * When nothing is known about a particular package in a particular class path entry the answers
   * to questions about package properties are taken from this instance.
   */
  public static final PackageReport DEFAULT_PACKAGE_REPORT = PackageReport.newBuilder().build();

  /** Allows for the initialization/retrieval of one shared PackageInfoCache instance per thread. */
  private static final ThreadLocal<PackageInfoCache> packageInfoCacheStorage = new ThreadLocal<>();

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

    packageInfoCacheStorage.set(new PackageInfoCache(resourcesClassLoader));
  }

  private final Map<String, PackageReport> packageReportByTypeName = new HashMap<>();
  private final ClassLoader resourcesClassLoader;

  private PackageInfoCache(ClassLoader resourcesClassLoader) {
    this.resourcesClassLoader = resourcesClassLoader;
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

  public void setPackageProperties(
      String packagePath, String packageJsNamespace, String objectiveCName, boolean isNullMarked) {
    packageReportByTypeName.put(
        packagePath,
        PackageReport.newBuilder()
            .setJsNamespace(packageJsNamespace)
            .setObjectiveCName(objectiveCName)
            .setNullMarked(isNullMarked)
            .build());
  }

  private PackageReport getPackageReport(String packagePath) {
    return packageReportByTypeName.computeIfAbsent(packagePath, this::parsePackageInfo);
  }

  private PackageReport parsePackageInfo(String packagePath) {
    Annotation[] packageAnnotations = findBytecodePackageAnnotations(packagePath);
    if (packageAnnotations == null) {
      return DEFAULT_PACKAGE_REPORT;
    } else {
      return PackageReport.newBuilder()
          .setJsNamespace(getPackageJsNamespace(packageAnnotations))
          .setObjectiveCName(getPackageObjectiveCName(packageAnnotations))
          .setNullMarked(hasNullMarkedAnnotation(packageAnnotations))
          .build();
    }
  }

  private Annotation[] findBytecodePackageAnnotations(String packagePath) {
    String packageInfoSourceName = packagePath + ".package-info";
    Annotation[] annotations = null;
    try {
      Class<?> packageInfoClass = resourcesClassLoader.loadClass(packageInfoSourceName);
      annotations = packageInfoClass.getAnnotations();
    } catch (ClassNotFoundException ignored) {
      // No package-info file in the classpath.
    }
    return annotations;
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
}
