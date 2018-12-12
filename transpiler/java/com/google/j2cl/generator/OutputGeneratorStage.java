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
package com.google.j2cl.generator;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.frontend.FrontendUtils;
import com.google.j2cl.frontend.FrontendUtils.FileInfo;
import com.google.j2cl.libraryinfo.LibraryInfo;
import com.google.j2cl.libraryinfo.LibraryInfoBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * The OutputGeneratorStage contains all necessary information for generating the JavaScript output
 * and source map files for the transpiler. It is responsible for pulling in native sources and then
 * generating header, implementation and sourcemap files for each Java Type.
 */
public class OutputGeneratorStage {
  private final List<FileInfo> nativeJavaScriptFiles;
  private final Problems problems;
  private final Path outputPath;
  private final Optional<Path> libraryInfoOutputPath;
  private final boolean declareLegacyNamespace;
  private final boolean shouldGenerateReadableSourceMaps;
  private final boolean generateKytheIndexingMetadata;

  public OutputGeneratorStage(
      List<FileInfo> nativeJavaScriptFiles,
      Path outputPath,
      Optional<Path> libraryInfoOutputPath,
      boolean declareLegacyNamespace,
      boolean shouldGenerateReadableSourceMaps,
      boolean generateKytheIndexingMetadata,
      Problems problems) {
    this.nativeJavaScriptFiles = nativeJavaScriptFiles;
    this.outputPath = outputPath;
    this.libraryInfoOutputPath = libraryInfoOutputPath;
    this.declareLegacyNamespace = declareLegacyNamespace;
    this.shouldGenerateReadableSourceMaps = shouldGenerateReadableSourceMaps;
    this.generateKytheIndexingMetadata = generateKytheIndexingMetadata;
    this.problems = problems;
  }

  public void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable. Actually this one can't actually destabilize output but since
    // it's being safely iterated over now it's best to guard against it being unsafely iterated
    // over in the future.
    Map<String, NativeJavaScriptFile> nativeFilesByPath =
        NativeJavaScriptFile.getMap(nativeJavaScriptFiles, problems);
    LibraryInfo.Builder libraryInfo = LibraryInfo.newBuilder();

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      for (Type type : j2clCompilationUnit.getTypes()) {
        JavaScriptImplGenerator jsImplGenerator =
            new JavaScriptImplGenerator(problems, declareLegacyNamespace, type);

        // If the java type contains any native methods, search for matching native file.
        String typeRelativePath = getRelativePath(type);
        String typeAbsolutePath =
            FrontendUtils.getJavaPath(getAbsolutePath(j2clCompilationUnit, type));

        // Locate matching native files that either have the same relative package as their Java
        // class (useful when Java and native.js files started in different directories on disk).
        // TODO(goktug): reconsider matching with relative name.
        NativeJavaScriptFile matchingNativeFile = nativeFilesByPath.get(typeRelativePath);
        // or that are in the same absolute path folder on disk as their Java class.
        if (matchingNativeFile == null) {
          matchingNativeFile = nativeFilesByPath.get(typeAbsolutePath);
        }

        if (matchingNativeFile != null) {
          jsImplGenerator.setNativeSource(matchingNativeFile);
          matchingNativeFile.setUsed();
        }

        // If not matching native file is found, and the java type contains non-JsMethod native
        // method, reports an error.
        if (matchingNativeFile == null && type.containsNonJsNativeMethods()) {
          problems.error(
              "Cannot find matching native file '%s'.",
              typeRelativePath + NativeJavaScriptFile.NATIVE_EXTENSION);
          return;
        }

        String javaScriptImplementationSource = jsImplGenerator.renderOutput();

        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(problems, declareLegacyNamespace, type);
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
            Path absolutePathForSourceMap =
                outputPath.resolve(getRelativePath(type) + SOURCE_MAP_SUFFIX);
            J2clUtils.writeToFile(absolutePathForSourceMap, sourceMap, problems);
          }
        }

        if (shouldGenerateReadableSourceMaps) {
          outputReadableSourceMap(
              j2clCompilationUnit,
              type,
              javaScriptImplementationSource,
              jsImplGenerator.getSourceMappings(),
              matchingNativeFile);
        }

        String implRelativePath = typeRelativePath + jsImplGenerator.getSuffix();
        J2clUtils.writeToFile(
            outputPath.resolve(implRelativePath), javaScriptImplementationSource, problems);

        String headerRelativePath = typeRelativePath + jsHeaderGenerator.getSuffix();
        J2clUtils.writeToFile(
            outputPath.resolve(headerRelativePath), javaScriptHeaderSource, problems);

        if (libraryInfoOutputPath.isPresent()) {
          libraryInfo.addType(
              LibraryInfoBuilder.build(
                  type,
                  headerRelativePath,
                  implRelativePath,
                  jsImplGenerator.getOutputSourceInfoByMember()));
        }

        if (matchingNativeFile != null) {
          copyNativeJsFileToOutput(matchingNativeFile);
        }
      }

      if (!generateKytheIndexingMetadata) {
        copyJavaSourcesToOutput(j2clCompilationUnit);
      }
    }

    if (libraryInfoOutputPath.isPresent()) {
      J2clUtils.writeToFile(
          libraryInfoOutputPath.get(), LibraryInfoBuilder.serialize(libraryInfo), problems);
    }

    // Error if any of the native implementation files were not used.
    for (Entry<String, NativeJavaScriptFile> fileEntry : nativeFilesByPath.entrySet()) {
      if (!fileEntry.getValue().wasUsed()) {
        problems.error("Unused native file '%s'.", fileEntry.getValue());
      }
    }
  }

  private static final String SOURCE_MAP_SUFFIX = ".js.map";

  private static final String READABLE_MAPPINGS_SUFFIX = ".js.mappings";

  private String renderKytheIndexingMetadata(
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    KytheIndexingMetadata metadata = new KytheIndexingMetadata();

    for (Entry<SourcePosition, SourcePosition> entry :
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
      Type type,
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
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
      Path absolutePathForReadableSourceMap =
          outputPath.resolve(getRelativePath(type) + READABLE_MAPPINGS_SUFFIX);
      J2clUtils.writeToFile(absolutePathForReadableSourceMap, readableOutput, problems);
    }
  }

  /**
   * Copy Java source files to the output. Sourcemaps reference locations in the Java source file,
   * and having it available as output simplifies the process of source debugging in the browser.
   */
  private void copyJavaSourcesToOutput(CompilationUnit j2clUnit) {
    String relativePath =
        j2clUnit.getPackageName().replace(".", File.separator)
            + File.separator
            + j2clUnit.getName();
    Path absolutePath = outputPath.resolve(relativePath + ".java");
    J2clUtils.copyFile(Paths.get(j2clUnit.getFilePath()), absolutePath, problems);
  }

  private void copyNativeJsFileToOutput(NativeJavaScriptFile nativeJavaScriptFile) {
    Path absolutePath = outputPath.resolve(nativeJavaScriptFile.getRelativeFilePath());
    J2clUtils.writeToFile(absolutePath, nativeJavaScriptFile.getContent(), problems);
  }

  /** Returns the relative output path for a given type. */
  private static String getRelativePath(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String typeName = typeDeclaration.getSimpleBinaryName();
    String packageName = typeDeclaration.getPackageName();
    return packageName.replace(".", File.separator) + File.separator + typeName;
  }

  /** Returns the absolute binary path for a given type. */
  private static String getAbsolutePath(CompilationUnit compilationUnit, Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    String typeName = typeDeclaration.getSimpleBinaryName();
    return compilationUnit.getDirectoryPath() + File.separator + typeName;
  }
}
