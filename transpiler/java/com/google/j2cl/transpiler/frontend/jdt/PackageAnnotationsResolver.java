/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.common.base.Preconditions.checkState;
import static com.google.j2cl.transpiler.frontend.jdt.J2ktInteropAnnotationUtils.getJ2ktObjectiveCName;
import static com.google.j2cl.transpiler.frontend.jdt.JsInteropAnnotationUtils.getJsNamespace;

import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.CompilationUnit;

/** A utility class to resolve and cache package annotations. */
public final class PackageAnnotationsResolver {

  /** Create a PackageAnnotationResolver with the source package-info CompilationUnits. */
  public static PackageAnnotationsResolver create(
      Stream<CompilationUnit> packageInfoCompilationUnits, PackageInfoCache packageInfoCache) {
    var packageAnnotationResolver = new PackageAnnotationsResolver(packageInfoCache);
    packageAnnotationResolver.populateFromCompilationUnits(packageInfoCompilationUnits);
    return packageAnnotationResolver;
  }

  private final PackageInfoCache packageInfoCache;

  public String getJsNameSpace(String packageName) {
    return packageInfoCache.getJsNamespace(packageName);
  }

  public String getObjectiveCNamePrefix(String packageName) {
    return packageInfoCache.getObjectiveCName(packageName);
  }

  public boolean isNullMarked(String packageName) {
    return packageInfoCache.isNullMarked(packageName);
  }

  /** Populates the cache for the annotations in package-info compilation units. */
  private void populateFromCompilationUnits(Stream<CompilationUnit> packageInfoCompilationUnits) {
    packageInfoCompilationUnits.forEach(
        cu -> {
          var packageDeclaration = cu.getPackage();

          // Sanity check, package-info.java files declare no types.
          checkState(cu.types().isEmpty());

          // Records information about package-info files supplied as source code.
          if (packageDeclaration != null) {
            packageInfoCache.setPackageProperties(
                packageDeclaration.getName().getFullyQualifiedName(),
                getJsNamespace(packageDeclaration),
                getJ2ktObjectiveCName(packageDeclaration),
                JdtAnnotationUtils.hasNullMarkedAnnotation(packageDeclaration));
          }
        });
  }

  private PackageAnnotationsResolver(PackageInfoCache packageInfoCache) {
    this.packageInfoCache = packageInfoCache;
  }
}
