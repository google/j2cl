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
import static com.google.j2cl.transpiler.frontend.jdt.JsInteropAnnotationUtils.getJsNamespace;
import static com.google.j2cl.transpiler.frontend.jdt.KtInteropAnnotationUtils.getKtObjectiveCName;

import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

  /** Create a PackageAnnotationResolver with package infos in sources. */
  public static PackageAnnotationsResolver create(
      List<FileInfo> sources, PackageInfoCache packageInfoCache, Problems problems) {
    var packageAnnotationResolver = new PackageAnnotationsResolver(packageInfoCache);
    packageAnnotationResolver.populateFromSources(sources, problems);
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
                getKtObjectiveCName(packageDeclaration),
                JdtAnnotationUtils.isNullMarked(packageDeclaration));
          }
        });
  }

  /** Populates the cache for the annotations in package-info compilation units. */
  private void populateFromSources(List<FileInfo> packageInfoFiles, Problems problems) {
    for (var packageInfoFile : packageInfoFiles) {
      var content = readSource(packageInfoFile, problems);
      var jsNamespace = extractJsNamespace(content);
      if (jsNamespace != null) {
        var packageName = extractPackageName(content);
        packageInfoCache.setPackageProperties(packageName, jsNamespace, null, false);
      }
    }
  }

  @Nullable
  private static String readSource(FileInfo file, Problems problems) {
    try {
      return Files.readString(Path.of(file.sourcePath()));
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
      return null;
    }
  }

  private static final Pattern JS_PACKAGE_NAMESPACE_PATTERN =
      Pattern.compile(
          "^\\s*@((?:jsinterop\\.annotations\\.)?JsPackage)\\s*\\(\\s*namespace\\s*=\\s*\"([^\"]*)\"\\s*\\)",
          Pattern.MULTILINE);

  @Nullable
  private static String extractJsNamespace(String content) {
    Matcher matcher = JS_PACKAGE_NAMESPACE_PATTERN.matcher(content);
    if (!matcher.find()) {
      return null;
    }
    // If short name is used, make sure that it is properly imported.
    // Note that there could be also wildcard import but that's against to the style guide.
    if (matcher.group(1).equals("JsPackage")
        && !content.contains("import jsinterop.annotations.JsPackage;")) {
      return null;
    }

    return matcher.group(2);
  }

  private static final Pattern PACKAGE_PATTERN =
      Pattern.compile("^package ([\\w.]+);", Pattern.MULTILINE);

  private static String extractPackageName(String content) {
    Matcher matcher = PACKAGE_PATTERN.matcher(content);
    checkState(matcher.find());
    return matcher.group(1);
  }

  private PackageAnnotationsResolver(PackageInfoCache packageInfoCache) {
    this.packageInfoCache = packageInfoCache;
  }
}
