/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.backend.libraryinfo.LibraryInfoBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The OutputGeneratorStage contains all necessary information for generating the JavaScript output
 * and source map files for the transpiler. It is responsible for pulling in native sources and then
 * generating header, implementation and sourcemap files for each Java Type.
 */
public class OutputGeneratorStage {
  private final List<FileInfo> nativeJavaScriptFiles;
  private final Problems problems;
  private final Output output;
  private final Path libraryInfoOutputPath;
  private final boolean shouldGenerateReadableSourceMaps;
  private final boolean shouldGenerateReadableLibraryInfo;
  private final boolean generateKytheIndexingMetadata;

  public OutputGeneratorStage(
      List<FileInfo> nativeJavaScriptFiles,
      Output output,
      Path libraryInfoOutputPath,
      boolean shouldGenerateReadableLibraryInfo,
      boolean shouldGenerateReadableSourceMaps,
      boolean generateKytheIndexingMetadata,
      Problems problems) {
    this.nativeJavaScriptFiles = nativeJavaScriptFiles;
    this.output = output;
    this.libraryInfoOutputPath = libraryInfoOutputPath;
    this.shouldGenerateReadableLibraryInfo = shouldGenerateReadableLibraryInfo;
    this.shouldGenerateReadableSourceMaps = shouldGenerateReadableSourceMaps;
    this.generateKytheIndexingMetadata = generateKytheIndexingMetadata;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {

    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable. Actually this one can't actually destabilize output but since
    // it's being safely iterated over now it's best to guard against it being unsafely iterated
    // over in the future.
    Map<String, NativeJavaScriptFile> nativeFilesByPath =
        NativeJavaScriptFile.getMap(nativeJavaScriptFiles, problems);
    LibraryInfoBuilder libraryInfoBuilder = new LibraryInfoBuilder();

    for (CompilationUnit compilationUnit : library.getCompilationUnits()) {
      for (Type type : compilationUnit.getTypes()) {
        List<Import> imports = ImportGatherer.gatherImports(type);
        JavaScriptImplGenerator jsImplGenerator =
            new JavaScriptImplGenerator(problems, type, imports);

        String typeRelativePath = getPackageRelativePath(type.getDeclaration());

        NativeJavaScriptFile matchingNativeFile =
            getMatchingNativeFile(nativeFilesByPath, compilationUnit, type);

        if (matchingNativeFile != null) {
          jsImplGenerator.setNativeSource(matchingNativeFile);
          matchingNativeFile.setUsed();

          // Native JsTypes are mere references to external JavaScript types, adding native code
          // through native.js files does not make sense. Non-native JsEnums on the other hand are
          // emitted by J2CL but are not JavaScript classes, "native.js" files are not allowed in
          // this case to avoid surprises.
          TypeDeclaration typeDeclaration = type.getUnderlyingTypeDeclaration();
          if (typeDeclaration.isNative() || typeDeclaration.isJsEnum()) {
            problems.error(
                "%s '%s' does not support having a '.native.js' file.",
                typeDeclaration.isJsEnum() ? "JsEnum" : "Native JsType",
                typeDeclaration.getReadableDescription());
            continue;
          }

        } else if (type.containsNonJsNativeMethods()) {
          problems.error(
              "Cannot find matching native file '%s'.",
              typeRelativePath + NativeJavaScriptFile.NATIVE_EXTENSION);
          continue;
        }

        String javaScriptImplementationSource = jsImplGenerator.renderOutput();

        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(problems, type, imports);
        String javaScriptHeaderSource = jsHeaderGenerator.renderOutput();

        if (generateKytheIndexingMetadata) {
          // Inline metadata so that Kythe can create edges between these files and the Java source
          // file.
          javaScriptHeaderSource +=
              renderKytheIndexingMetadata(jsHeaderGenerator.getSourceMappings());
          javaScriptImplementationSource +=
              renderKytheIndexingMetadata(jsImplGenerator.getSourceMappings());
        } else {
          String sourceMap = renderSourceMap(type, jsImplGenerator.getSourceMappings());

          if (sourceMap != null) {
            javaScriptImplementationSource +=
                String.format(
                    "%n//# sourceMappingURL=%s",
                    type.getDeclaration().getSimpleBinaryName() + SOURCE_MAP_SUFFIX);
            output.write(typeRelativePath + SOURCE_MAP_SUFFIX, sourceMap);
          }
        }

        if (shouldGenerateReadableSourceMaps) {
          outputReadableSourceMap(
              compilationUnit,
              type,
              javaScriptImplementationSource,
              jsImplGenerator.getSourceMappings(),
              matchingNativeFile);
        }

        String implRelativePath = typeRelativePath + jsImplGenerator.getSuffix();
        output.write(implRelativePath, javaScriptImplementationSource);

        String headerRelativePath = typeRelativePath + jsHeaderGenerator.getSuffix();
        output.write(headerRelativePath, javaScriptHeaderSource);

        if (libraryInfoOutputPath != null || shouldGenerateReadableLibraryInfo) {
          libraryInfoBuilder.addType(
              type,
              headerRelativePath,
              implRelativePath,
              jsImplGenerator.getOutputSourceInfoByMember());
        }

        if (matchingNativeFile != null) {
          // Copy native js file to output.
          output.write(matchingNativeFile.getRelativeFilePath(), matchingNativeFile.getContent());
        }
      }

      if (!generateKytheIndexingMetadata) {
        // Copy java sources to output.
        output.copyFile(compilationUnit.getFilePath(), compilationUnit.getPackageRelativePath());
      }
    }

    if (shouldGenerateReadableLibraryInfo) {
      output.write("library_info_debug.json", libraryInfoBuilder.toJson(problems));
    }

    if (libraryInfoOutputPath != null) {
      OutputUtils.writeToFile(libraryInfoOutputPath, libraryInfoBuilder.toByteArray(), problems);
    }

    // Error if any of the native implementation files were not used.
    for (NativeJavaScriptFile file : nativeFilesByPath.values()) {
      if (!file.wasUsed()) {
        problems.error("Unused native file '%s'.", file);
      }
    }
  }

  private static final String SOURCE_MAP_SUFFIX = ".js.map";

  private static final String READABLE_MAPPINGS_SUFFIX = ".js.mappings";

  private String renderKytheIndexingMetadata(
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    KytheIndexingMetadata metadata = new KytheIndexingMetadata();

    for (Map.Entry<SourcePosition, SourcePosition> entry :
        javaSourcePositionByOutputSourcePosition.entrySet()) {

      SourcePosition javaSourcePosition = entry.getValue();
      SourcePosition javaScriptSourcePosition = entry.getKey();

      metadata.addAnchorAnchor(
          javaSourcePosition.getStartFilePosition().getByteOffset(),
          javaSourcePosition.getEndFilePosition().getByteOffset(),
          javaScriptSourcePosition.getStartFilePosition().getByteOffset(),
          javaScriptSourcePosition.getEndFilePosition().getByteOffset(),
          null, // sourceCorpus
          javaSourcePosition.getFilePath(),
          null // sourceRoot
          );
    }

    return String.format(
        // TODO(b/77961191): remove leading newline once the bug is fixed.
        "%n// Kythe Indexing Metadata:%n// %s", metadata.toJson());
  }

  private String renderSourceMap(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    try {
      return SourceMapGeneratorStage.generateSourceMaps(
          type, javaSourcePositionByOutputSourcePosition);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
      return null;
    }
  }

  private void outputReadableSourceMap(
      CompilationUnit j2clUnit,
      Type type,
      String javaScriptImplementationFileContents,
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition,
      NativeJavaScriptFile nativeJavaScriptFile) {
    String readableOutput =
        ReadableSourceMapGenerator.generate(
            javaSourcePositionByOutputSourcePosition,
            javaScriptImplementationFileContents,
            nativeJavaScriptFile,
            j2clUnit.getFilePath(),
            problems);
    if (!readableOutput.isEmpty()) {
      String readableSourceMapRelativePath =
          getPackageRelativePath(type.getDeclaration()) + READABLE_MAPPINGS_SUFFIX;
      output.write(readableSourceMapRelativePath, readableOutput);
    }
  }

  /** Returns the relative output path for a given type. */
  private static String getPackageRelativePath(TypeDeclaration typeDeclaration) {
    return OutputUtils.getPackageRelativePath(
        typeDeclaration.getPackageName(), typeDeclaration.getSimpleBinaryName());
  }

  private static NativeJavaScriptFile getMatchingNativeFile(
      Map<String, NativeJavaScriptFile> nativeFilesByPath,
      CompilationUnit j2clCompilationUnit,
      Type type) {

    // Locate the files that have a package path relative to the root according to Java convention.
    // This is useful when the files are in different directories on disk.
    TypeDeclaration typeDeclaration = type.getUnderlyingTypeDeclaration();
    String typeRelativePath = getPackageRelativePath(typeDeclaration);

    NativeJavaScriptFile matchingNativeFile = nativeFilesByPath.get(typeRelativePath);
    if (matchingNativeFile != null) {
      return matchingNativeFile;
    }

    String typeJavaRootRelativePath =
        SourceUtils.getJavaPath(getAbsolutePath(j2clCompilationUnit, typeDeclaration));
    return nativeFilesByPath.get(typeJavaRootRelativePath);
  }

  /** Returns the absolute binary path for a given type. */
  private static String getAbsolutePath(
      CompilationUnit compilationUnit, TypeDeclaration typeDeclaration) {
    return compilationUnit.getDirectoryPath() + '/' + typeDeclaration.getSimpleBinaryName();
  }
}
