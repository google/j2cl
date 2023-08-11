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
package com.google.j2cl.transpiler.backend.closure;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.MoreFiles;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class NativeJavaScriptFileResolver {

  public static NativeJavaScriptFileResolver create(List<FileInfo> files, Problems problems) {
    ImmutableMap.Builder<String, NativeJavaScriptFile> byRelativePath = ImmutableMap.builder();
    ImmutableMap.Builder<String, NativeJavaScriptFile> byFullyQualifiedName =
        ImmutableMap.builder();

    for (FileInfo file : files) {
      checkArgument(file.sourcePath().endsWith(NativeJavaScriptFile.NATIVE_EXTENSION));
      try {
        String content = MoreFiles.asCharSource(Paths.get(file.sourcePath()), UTF_8).read();
        NativeJavaScriptFile nativeFile = new NativeJavaScriptFile(file.targetPath(), content);
        byRelativePath.put(nativeFile.getRelativePathWithoutExtension(), nativeFile);

        // Only resolve files by qualified name if it has at least one package segment. Otherwise
        // we are not able to differentiate resolving a fully qualified name vs resolving a relative
        // path (ex. imagine if we had foo/bar/Platform.native.js and baz/qux/Platform.native.js in
        // same compilation unit).
        String qualifiedName = nativeFile.getFullyQualifiedName();
        if (qualifiedName.indexOf('.') > 0) {
          byFullyQualifiedName.put(nativeFile.getFullyQualifiedName(), nativeFile);
        }
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
      }
    }

    return new NativeJavaScriptFileResolver(
        byRelativePath.buildOrThrow(), byFullyQualifiedName.buildOrThrow(), problems);
  }

  private final Map<String, NativeJavaScriptFile> byRelativePath;
  private final Map<String, NativeJavaScriptFile> byFullyQualifiedName;
  private final Problems problems;
  private final Set<NativeJavaScriptFile> usedFiles = new HashSet<>();

  private NativeJavaScriptFileResolver(
      Map<String, NativeJavaScriptFile> byRelativePath,
      Map<String, NativeJavaScriptFile> byFullyQualifiedName,
      Problems problems) {
    this.byRelativePath = byRelativePath;
    this.byFullyQualifiedName = byFullyQualifiedName;
    this.problems = problems;
  }

  public NativeJavaScriptFile getMatchingNativeFile(
      CompilationUnit j2clCompilationUnit, Type type) {
    checkArgument(
        !j2clCompilationUnit.isSynthetic(),
        "Synthetic CompilationUnit cannot have a corresponding .native.js file.");

    TypeDeclaration typeDeclaration = type.getUnderlyingTypeDeclaration();

    // Sources can be packaged in a various way so we have several approaches to match a symbol with
    // its corresponding "native.js" file. Generally we'll be looking in various paths relative to a
    // source root, which is the first "java/" or "javatests/" folder in the absolute path.
    // See: com.google.j2cl.common.SourceUtils.SourceUtils#getJavaPath
    NativeJavaScriptFile matchedFile =
        // First look for a ".native.js" file whose filename is the fully qualified identifier.
        // Ex: for the symbol "foo.bar.Buzz" we will look for a file name "foo.bar.Buzz.native.js"
        // anywhere in the inputs.
        resolveByFullyQualifiedName(typeDeclaration.getQualifiedBinaryName())
            // Next we'll attempt to look for ".native.js" relative to a folder path constructed
            // from the package statement.
            // For example: for "foo.bar.Buzz" we'll look the file "foo/bar/Buzz.native.js" relative
            // to a source root.
            .or(
                () ->
                    resolveByRelativePath(
                        OutputUtils.getPackageRelativePath(
                            typeDeclaration.getPackageName(),
                            typeDeclaration.getSimpleBinaryName())))
            // Finally we'll look for a ".native.js" file adjacent to the absolute path of the
            // source file this symbol is defined in.
            // For example, if we had the file "/foo/bar/super/Buzz.java" with a class "Bazz" in it,
            // we'd be looking for the path "/foo/bar/super/Bazz.native.js" relative to a source
            // root.
            .or(
                () ->
                    resolveByRelativePath(
                        SourceUtils.getJavaPath(
                            getAbsolutePath(j2clCompilationUnit, typeDeclaration))))
            .orElse(null);

    if (matchedFile != null) {
      usedFiles.add(matchedFile);
    }

    return matchedFile;
  }

  public void checkAllFilesUsed() {
    // We're relying on the fact that we populate all files in the byRelativePath map here. If this
    // no longer holds true we should use the union of all the map values.
    Set<NativeJavaScriptFile> allPotentialFiles = new HashSet<>(byRelativePath.values());
    Set<NativeJavaScriptFile> unusedFiles = Sets.difference(allPotentialFiles, usedFiles);
    unusedFiles.forEach(file -> problems.error("Unused native file '%s'.", file));
  }

  private Optional<NativeJavaScriptFile> resolveByRelativePath(String relativePath) {
    return Optional.ofNullable(byRelativePath.get(relativePath));
  }

  private Optional<NativeJavaScriptFile> resolveByFullyQualifiedName(String fullyQualifiedName) {
    return Optional.ofNullable(byFullyQualifiedName.get(fullyQualifiedName));
  }

  /** Returns the absolute binary path for a given type. */
  private static String getAbsolutePath(
      CompilationUnit compilationUnit, TypeDeclaration typeDeclaration) {
    return compilationUnit.getDirectoryPath() + '/' + typeDeclaration.getSimpleBinaryName();
  }
}
