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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 *
 * <p>JsPackage annotations in package-info files that are processed as source in the current
 * compile are directly inserted and annotations of package-info files provided as bytecode on the
 * classpath are dynamically cached on first access.
 */
public class PackageInfoCache {
  private static final ThreadLocal<PackageInfoCache> packageInfoCacheStorage = new ThreadLocal<>();
  private final Map<String, String> jsInteropNamespaceByPackagePath = new HashMap<>();
  private final ClassLoader packageInfoClassLoader;

  private PackageInfoCache(ClassLoader packageInfoClassLoader) {
    this.packageInfoClassLoader = packageInfoClassLoader;
  }

  public static PackageInfoCache get() {
    return Preconditions.checkNotNull(packageInfoCacheStorage.get());
  }

  @VisibleForTesting
  public static void clear() {
    packageInfoCacheStorage.remove();
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

    packageInfoCacheStorage.set(new PackageInfoCache(resourcesClassLoader));
  }

  /**
   * Returns the namespace specified by a {@link JsPackage} annotation, or null if no namespace is
   * specified (for example because there is no package-info file).
   */
  public String getJsNamespace(String packagePath) {
    if (!jsInteropNamespaceByPackagePath.containsKey(packagePath)) {
      parsePackageInfo(packagePath);
    }
    return jsInteropNamespaceByPackagePath.get(packagePath);
  }

  /**
   * Sets the given annotations for the given package. Annotations are normally parsed automatically
   * by loading package-info files with reflection. However this method allows to set them directly,
   * useful when package-info is in the files that are transpiled.
   */
  public void setPackageAnnotations(
      String packagePath, List<org.eclipse.jdt.core.dom.Annotation> annotations) {
    Preconditions.checkNotNull(annotations);

    String namespace = null;
    for (org.eclipse.jdt.core.dom.Annotation annotation : annotations) {
      IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
      if (JsInteropAnnotationUtils.isJsPackageAnnotation(annotationBinding)) {
        namespace = JsInteropAnnotationUtils.getJsNamespace(annotationBinding);
      }
    }

    jsInteropNamespaceByPackagePath.put(packagePath, namespace);
  }

  private void parsePackageInfo(String packagePath) {
    // Attempts to use the classloader to load the package-info.class file for the given package and
    // then use reflection to query it's annotations.
    Annotation[] annotations = {};
    try {
      Class<?> packageInfoClass = packageInfoClassLoader.loadClass(packagePath + ".package-info");
      annotations = packageInfoClass.getAnnotations();
    } catch (ClassNotFoundException e) {
      // Guess there isn't a package-info file there.
    }
    setPackageAnnotations(packagePath, annotations);
  }

  private void setPackageAnnotations(String packagePath, Annotation[] annotations) {
    String namespace = null;

    for (Annotation annotation : annotations) {
      if (annotation instanceof JsPackage) {
        namespace = ((JsPackage) annotation).namespace();
      }
    }
    jsInteropNamespaceByPackagePath.put(packagePath, namespace);
  }
}
