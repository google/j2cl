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

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.TimingCollector;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatherer;
import com.google.j2cl.generator.visitors.ImportGatherer.ImportCategory;
import com.google.j2cl.problems.Problems;
import com.google.j2cl.problems.Problems.Message;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The OutputGeneratorStage contains all necessary information for generating the JavaScript output,
 * source maps and depinfo files for the transpiler. It is responsible for pulling in native sources
 * and then generating header, implementation and sourcemap files for each Java Type.
 */
public class OutputGeneratorStage {
  private final Charset charset;
  private final List<String> nativeJavaScriptFileZipPaths;
  private final Problems problems;
  private final FileSystem outputFileSystem;
  private final String outputLocationPath;
  private final boolean declareLegacyNamespace;
  private final String depinfoPath;
  private final boolean shouldGenerateReadableSourceMaps;
  private final TimingCollector timingReport = TimingCollector.get();

  public OutputGeneratorStage(
      Charset charset,
      List<String> nativeJavaScriptFileZipPaths,
      FileSystem outputFileSystem,
      String outputLocationPath,
      boolean declareLegacyNamespace,
      String depinfoPath,
      boolean shouldGenerateReadableSourceMaps,
      Problems problems) {
    this.charset = charset;
    this.nativeJavaScriptFileZipPaths = nativeJavaScriptFileZipPaths;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.declareLegacyNamespace = declareLegacyNamespace;
    this.depinfoPath = depinfoPath;
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
        NativeJavaScriptFile.getFilesByPathFromZip(
            nativeJavaScriptFileZipPaths, charset.name(), problems);

    SortedSet<String> importModulePaths = new TreeSet<>();
    SortedSet<String> exportModulePaths = new TreeSet<>();

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      for (Type type : j2clCompilationUnit.getTypes()) {
        if (type.getDeclaration().isNative()) {
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
          jsImplGenerator.setNativeSource(matchingNativeFile.getContent());
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
            type.getDeclaration().getSimpleBinaryName()
                + SourceMapGeneratorStage.SOURCE_MAP_SUFFIX);

        Path absolutePathForImpl =
            GeneratorUtils.getAbsolutePath(
                outputFileSystem,
                outputLocationPath,
                GeneratorUtils.getRelativePath(type),
                jsImplGenerator.getSuffix());
        String javaScriptImplementationSource = jsImplGenerator.renderOutput();
        timingReport.startSample("Write impl");
        GeneratorUtils.writeToFile(
            absolutePathForImpl, javaScriptImplementationSource, charset, problems);

        timingReport.startSample("Render header");
        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(problems, declareLegacyNamespace, type);
        Path absolutePathForHeader =
            GeneratorUtils.getAbsolutePath(
                outputFileSystem,
                outputLocationPath,
                GeneratorUtils.getRelativePath(type),
                jsHeaderGenerator.getSuffix());
        String javaScriptHeaderFile = jsHeaderGenerator.renderOutput();
        timingReport.startSample("Write header");
        GeneratorUtils.writeToFile(absolutePathForHeader, javaScriptHeaderFile, charset, problems);

        timingReport.startSample("Render source maps");
        generateSourceMaps(
            j2clCompilationUnit,
            type,
            javaScriptImplementationSource,
            jsImplGenerator.getSourceMappings());

        timingReport.startSample("Gather depinfo");
        if (depinfoPath != null) {
          gatherDepinfo(type, importModulePaths, exportModulePaths);
        }
      }

      timingReport.startSample("Copy *.java sources");
      copyJavaSourcesToOutput(j2clCompilationUnit);
    }

    timingReport.startSample("Write depinfo");
    if (depinfoPath != null) {
      writeDepinfo(importModulePaths, exportModulePaths);
    }

    timingReport.startSample("Check unused native impl files.");
    // Error if any of the native implementation files were not used.
    for (Entry<String, NativeJavaScriptFile> fileEntry : nativeFilesByPath.entrySet()) {
      if (!fileEntry.getValue().wasUsed()) {
        problems.error(Message.ERR_NATIVE_UNUSED_NATIVE_SOURCE, fileEntry.getValue().toString());
      }
    }
  }

  private void gatherDepinfo(
      Type type, Set<String> importModulePaths, Set<String> exportModulePaths) {
    // Gather imports.
    Multimap<ImportCategory, Import> importsByCategory = ImportGatherer.gatherImports(type);
    for (ImportCategory importCategory : ImportCategory.values()) {
      // Don't record use of the environment, it is not considered a dependency.
      if (importCategory == ImportCategory.EXTERN) {
        continue;
      }

      for (Import anImport : importsByCategory.get(importCategory)) {
        importModulePaths.add(anImport.getHeaderModulePath());
        importModulePaths.add(anImport.getImplModulePath());
      }
    }

    // Gather exports.
    TypeDeclaration selfTypeDeclaration = type.getDeclaration();
    exportModulePaths.add(selfTypeDeclaration.getModuleName());
    exportModulePaths.add(selfTypeDeclaration.getImplModuleName());
  }

  private void writeDepinfo(
      SortedSet<String> importModulePaths, SortedSet<String> exportModulePaths) {
    // Don't report self-dependencies (from things within this compile onto things within this
    // compile).
    importModulePaths.removeAll(exportModulePaths);

    // If there are no imports or exports, include at least a space on the line, to make life easier
    // if parsing the depinfo file in Bash.
    if (importModulePaths.isEmpty()) {
      importModulePaths.add(" ");
    }
    if (exportModulePaths.isEmpty()) {
      exportModulePaths.add(" ");
    }

    String depinfoContent =
        Joiner.on(",").join(importModulePaths) + "\n" + Joiner.on(",").join(exportModulePaths);
    // The format here is:
    // line 1: comma separated list of names of imported (goog.require()d) modules
    // line 2: comma separated list of names of exported (goog.module() declared) modules

    try {
      com.google.common.io.Files.write(depinfoContent, new File(depinfoPath), charset);
    } catch (IOException e) {
      problems.error(Message.ERR_CANNOT_GENERATE_OUTPUT, depinfoPath, e.getMessage());
    }
  }

  private void generateSourceMaps(
      CompilationUnit j2clUnit,
      Type type,
      String javaScriptImplementationFileContents,
      Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    String compilationUnitFileName = j2clUnit.getFileName();
    String compilationUnitFilePath = j2clUnit.getFilePath();
    // Generate sourcemap files.
    new SourceMapGeneratorStage(
            charset,
            outputFileSystem,
            compilationUnitFileName,
            outputLocationPath,
            compilationUnitFilePath,
            javaScriptImplementationFileContents,
            problems,
            shouldGenerateReadableSourceMaps)
        .generateSourceMaps(type, javaSourcePositionByOutputSourcePosition);
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
    Path outputPath =
        GeneratorUtils.getAbsolutePath(outputFileSystem, outputLocationPath, relativePath, ".java");
    try {
      Files.copy(
          Paths.get(j2clUnit.getFilePath()),
          outputPath,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
      // Wipe entries modification time so that input->output mapping is stable
      // regardless of the time of day.
      Files.setLastModifiedTime(outputPath, FileTime.fromMillis(0));
    } catch (IOException e) {
      // TODO(tdeegan): This blows up during the JRE compile. Did this ever work? The sources are
      // available for compilation so no errors should be seen here unless there is an exceptional
      // condition.
      // errors.error(Errors.Error.ERR_ERROR, "Could not copy java file: "
      // + outputPath + ":" + e.getMessage());
    }
  }
}
