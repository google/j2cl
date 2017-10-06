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
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.Message;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.TimingCollector;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The OutputGeneratorStage contains all necessary information for generating the JavaScript output
 * and source map files for the transpiler. It is responsible for pulling in native sources and then
 * generating header, implementation and sourcemap files for each Java Type.
 */
public class OutputGeneratorStage {
  private final List<String> nativeJavaScriptFileZipPaths;
  private final Problems problems;
  private final Path outputPath;
  private final boolean declareLegacyNamespace;
  private final boolean shouldGenerateReadableSourceMaps;
  private final TimingCollector timingReport = TimingCollector.get();

  public OutputGeneratorStage(
      List<String> nativeJavaScriptFileZipPaths,
      Path outputPath,
      boolean declareLegacyNamespace,
      boolean shouldGenerateReadableSourceMaps,
      Problems problems) {
    this.nativeJavaScriptFileZipPaths = nativeJavaScriptFileZipPaths;
    this.outputPath = outputPath;
    this.declareLegacyNamespace = declareLegacyNamespace;
    this.shouldGenerateReadableSourceMaps = shouldGenerateReadableSourceMaps;
    this.problems = problems;
  }

  public void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable. Actually this one can't actually destabilize output but since
    // it's being safely iterated over now it's best to guard against it being unsafely iterated
    // over in the future.
    timingReport.startSample("Native files gather");

    Map<String, NativeJavaScriptFile> nativeFilesByPath =
        NativeJavaScriptFile.getFilesByPathFromZip(nativeJavaScriptFileZipPaths, problems);

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      for (Type type : j2clCompilationUnit.getTypes()) {
        if (type.getDeclaration().isNative() || type.getDeclaration().isJsFunctionInterface()) {
          // Don't generate JS for native JsTypes.
          continue;
        }

        timingReport.startSample("Create impl generator (gather variable aliases)");
        JavaScriptImplGenerator jsImplGenerator =
            new JavaScriptImplGenerator(problems, declareLegacyNamespace, type);

        // If the java type contains any native methods, search for matching native file.
        timingReport.startSample("Native files read");

        String typeRelativePath = GeneratorUtils.getRelativePath(type);
        String typeAbsolutePath = GeneratorUtils.getAbsolutePath(j2clCompilationUnit, type);

        // Locate matching native files that either have the same relative package as their Java
        // class (useful when Java and native.js files started in different directories on disk).
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
              Message.ERR_NATIVE_JAVA_SOURCE_NO_MATCH,
              typeRelativePath + NativeJavaScriptFile.NATIVE_EXTENSION);
          return;
        }

        timingReport.startSample("Render impl");
        jsImplGenerator.setRelativeSourceMapLocation(
            type.getDeclaration().getSimpleBinaryName() + SOURCE_MAP_SUFFIX);

        Path absolutePathForImpl =
            outputPath.resolve(GeneratorUtils.getRelativePath(type) + jsImplGenerator.getSuffix());
        String javaScriptImplementationSource = jsImplGenerator.renderOutput();
        timingReport.startSample("Write impl");
        GeneratorUtils.writeToFile(absolutePathForImpl, javaScriptImplementationSource, problems);

        timingReport.startSample("Render header");
        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(problems, declareLegacyNamespace, type);
        Path absolutePathForHeader =
            outputPath.resolve(
                GeneratorUtils.getRelativePath(type) + jsHeaderGenerator.getSuffix());
        String javaScriptHeaderFile = jsHeaderGenerator.renderOutput();
        timingReport.startSample("Write header");
        GeneratorUtils.writeToFile(absolutePathForHeader, javaScriptHeaderFile, problems);

        timingReport.startSample("Render source maps");
        generateSourceMaps(
            j2clCompilationUnit,
            type,
            javaScriptImplementationSource,
            jsImplGenerator.getSourceMappings(),
            matchingNativeFile);

        if (matchingNativeFile != null) {
          copyNativeJsFileToOutput(matchingNativeFile);
        }
      }

      timingReport.startSample("Copy *.java sources");
      copyJavaSourcesToOutput(j2clCompilationUnit);
    }
    timingReport.startSample("Check unused native impl files.");
    // Error if any of the native implementation files were not used.
    for (Entry<String, NativeJavaScriptFile> fileEntry : nativeFilesByPath.entrySet()) {
      if (!fileEntry.getValue().wasUsed()) {
        problems.error(Message.ERR_NATIVE_UNUSED_NATIVE_SOURCE, fileEntry.getValue().toString());
      }
    }
  }

  private static final String SOURCE_MAP_SUFFIX = ".js.map";

  private void generateSourceMaps(
      CompilationUnit j2clUnit,
      Type type,
      String javaScriptImplementationFileContents,
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition,
      NativeJavaScriptFile nativeJavaScriptFile) {
    try {
      String output;
      if (shouldGenerateReadableSourceMaps) {
        output =
            ReadableSourceMapGenerator.generate(
                javaSourcePositionByOutputSourcePosition,
                javaScriptImplementationFileContents,
                nativeJavaScriptFile,
                j2clUnit.getFilePath());
        if (output.isEmpty()) {
          return;
        }
      } else {
        // Generate sourcemap files.
        output =
            SourceMapGeneratorStage.generateSourceMaps(
                type, javaSourcePositionByOutputSourcePosition);
      }

      Path absolutePathForSourceMap =
          outputPath.resolve(GeneratorUtils.getRelativePath(type) + SOURCE_MAP_SUFFIX);
      GeneratorUtils.writeToFile(absolutePathForSourceMap, output, problems);
    } catch (IOException e) {
      problems.error(
          "Could not generate source maps for %s: %s", j2clUnit.getName(), e.getMessage());
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
    try {
      Files.copy(
          Paths.get(j2clUnit.getFilePath()),
          absolutePath,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      Files.setLastModifiedTime(absolutePath, FileTime.fromMillis(0));
    } catch (IOException e) {
      // TODO(tdeegan): This blows up during the JRE compile. Did this ever work? The sources are
      // available for compilation so no errors should be seen here unless there is an exceptional
      // condition.
      // errors.error(Errors.Error.ERR_ERROR, "Could not copy java file: "
      // + absoluteOutputPath + ":" + e.getMessage());
    }
  }

  private void copyNativeJsFileToOutput(NativeJavaScriptFile nativeJavaScriptFile) {
    Path absolutePath = outputPath.resolve(nativeJavaScriptFile.getRelativeFilePath());
    try {
      Files.write(absolutePath, nativeJavaScriptFile.getContent().getBytes(StandardCharsets.UTF_8));
      Files.setLastModifiedTime(absolutePath, FileTime.fromMillis(0));
    } catch (IOException e) {
      problems.warning("Could not copy native js file to " + absolutePath + ":" + e.getMessage());
    }
  }
}
