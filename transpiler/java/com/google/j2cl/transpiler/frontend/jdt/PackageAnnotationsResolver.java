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
import static com.google.j2cl.transpiler.frontend.common.PackageInfoCache.DEFAULT_PACKAGE_REPORT;
import static com.google.j2cl.transpiler.frontend.jdt.JsInteropAnnotationUtils.getJsNamespace;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtObjectiveCName;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache.PackageReport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** A utility class to resolve and cache package annotations. */
public final class PackageAnnotationsResolver {

  private final Map<String, PackageReport> packageReportByPackageName = new HashMap<>();
  private final JdtParser parser;

  /** Create a PackageAnnotationResolver with the source package-info CompilationUnits. */
  public static PackageAnnotationsResolver create(
      Stream<CompilationUnit> packageInfoCompilationUnits, JdtParser parser) {
    var packageAnnotationResolver = new PackageAnnotationsResolver(parser);
    packageAnnotationResolver.populateFromCompilationUnits(packageInfoCompilationUnits);
    return packageAnnotationResolver;
  }

  /** Create a PackageAnnotationResolver with package infos in sources. */
  public static PackageAnnotationsResolver create(List<FileInfo> sources, JdtParser parser) {
    return create(parsePackageInfoFiles(sources, parser), parser);
  }

  public String getJsNameSpace(String packageName) {
    return getPackageReport(packageName).getJsNamespace();
  }

  public String getObjectiveCNamePrefix(String packageName) {
    return getPackageReport(packageName).getObjectiveCName();
  }

  public boolean isNullMarked(String packageName) {
    return getPackageReport(packageName).isNullMarked();
  }

  /** Parser package-info files from sources. */
  private static Stream<CompilationUnit> parsePackageInfoFiles(
      List<FileInfo> sources, JdtParser parser) {

    if (sources.isEmpty()) {
      return Stream.of();
    }

    var parsingResult = parser.parseFiles(sources, false, ImmutableList.of());
    return parsingResult.getCompilationUnitsByFilePath().values().stream();
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
            setPackageProperties(
                packageDeclaration.getName().getFullyQualifiedName(),
                getJsNamespace(packageDeclaration),
                getKtObjectiveCName(packageDeclaration),
                JdtAnnotationUtils.isNullMarked(packageDeclaration));
          }
        });
  }

  public void setPackageProperties(
      String packageName, String jsNamespace, String objectiveCName, boolean isNullMarked) {
    packageReportByPackageName.put(
        packageName,
        PackageReport.newBuilder()
            .setJsNamespace(jsNamespace)
            .setObjectiveCName(objectiveCName)
            .setNullMarked(isNullMarked)
            .build());
  }

  private PackageReport getPackageReport(String packageName) {
    return packageReportByPackageName.computeIfAbsent(packageName, this::createPackageReport);
  }

  private PackageReport createPackageReport(String packageName) {
    ITypeBinding packageInfoBinding = parser.resolveBinding(packageName + ".package-info");
    if (packageInfoBinding == null) {
      return DEFAULT_PACKAGE_REPORT;
    }

    boolean isNullMarked =
        stream(packageInfoBinding.getAnnotations())
            .anyMatch(
                a -> Nullability.isNullMarkedAnnotation(a.getAnnotationType().getQualifiedName()));

    String objectiveCName =
        getKtObjectiveCName(
            KtInteropAnnotationUtils.getKtObjectiveCNameAnnotation(
                packageInfoBinding.getAnnotations()));

    String jsNamespace =
        getJsNamespace(JsInteropAnnotationUtils.getJsPackageAnnotation(packageInfoBinding));
    return PackageReport.newBuilder()
        .setJsNamespace(jsNamespace)
        .setObjectiveCName(objectiveCName)
        .setNullMarked(isNullMarked)
        .build();
  }

  private PackageAnnotationsResolver(JdtParser parser) {
    this.parser = parser;
  }
}
