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
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.errors.Errors;

import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The JavaScriptGeneratorStage contains all necessary information for generating the JavaScript
 * output for the transpiler.  It is responsible for pulling in native sources and omitted sources
 * and then generating header and implementation files for each Java Type.
 */
public class JavaScriptGeneratorStage {
  private Charset charset;
  private Set<String> omitSourceFiles;
  private Set<String> nativeJavaScriptFileZipPaths;
  private Errors errors;
  private FileSystem outputFileSystem;
  private String outputLocationPath;

  public JavaScriptGeneratorStage(
      Charset charset,
      Set<String> omitSourceFiles,
      Set<String> nativeJavaScriptFileZipPaths,
      FileSystem outputFileSystem,
      String outputLocationPath,
      Errors errors) {
    this.charset = charset;
    this.omitSourceFiles = omitSourceFiles;
    this.nativeJavaScriptFileZipPaths = nativeJavaScriptFileZipPaths;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.errors = errors;
  }

  public void generateJavaScriptSources(List<CompilationUnit> j2clCompilationUnits) {
    Map<String, NativeJavaScriptFile> nativeFilesByPath = new HashMap<>();
    for (String nativeJavascriptFileZipPath : nativeJavaScriptFileZipPaths) {
      nativeFilesByPath.putAll(
          NativeJavaScriptFile.getFilesByPathFromZip(
              nativeJavascriptFileZipPath, charset.name(), errors));
    }

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      if (omitSourceFiles.contains(j2clCompilationUnit.getFilePath())) {
        continue;
      }

      for (JavaType javaType : j2clCompilationUnit.getTypes()) {
        if (javaType.getDescriptor().isNative()) {
          // Don't generate JS for native JsType.
          continue;
        }

        JavaScriptImplGenerator jsImplGenerator = new JavaScriptImplGenerator(errors, javaType);

        // If the java type contains any native methods, search for matching native file.
        if (javaType.containsNativeMethods()) {
          String typeRelativePath = GeneratorUtils.getRelativePath(javaType);
          String typeAbsolutePath = GeneratorUtils.getAbsolutePath(j2clCompilationUnit, javaType);

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
          if (matchingNativeFile == null && javaType.containsNonJsNativeMethods()) {
            errors.error(
                Errors.Error.ERR_NATIVE_JAVA_SOURCE_NO_MATCH,
                typeRelativePath + NativeJavaScriptFile.NATIVE_EXTENSION);
            return;
          }
        }

        jsImplGenerator.setRelativeSourceMapLocation(
            javaType.getDescriptor().getClassName() + SourceMapGeneratorStage.SOURCE_MAP_SUFFIX);

        Path absolutePathForImpl =
            GeneratorUtils.getAbsolutePath(
                outputFileSystem,
                outputLocationPath,
                GeneratorUtils.getRelativePath(javaType),
                jsImplGenerator.getSuffix());
        jsImplGenerator.writeToFile(absolutePathForImpl, charset);

        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(errors, javaType);
        Path absolutePathForHeader =
            GeneratorUtils.getAbsolutePath(
                outputFileSystem,
                outputLocationPath,
                GeneratorUtils.getRelativePath(javaType),
                jsHeaderGenerator.getSuffix());
        jsHeaderGenerator.writeToFile(absolutePathForHeader, charset);
      }
    }

    // Error if any of the native implementation files were not used.
    for (Entry<String, NativeJavaScriptFile> fileEntry : nativeFilesByPath.entrySet()) {
      if (!fileEntry.getValue().wasUsed()) {
        errors.error(
            Errors.Error.ERR_NATIVE_UNUSED_NATIVE_SOURCE,
            fileEntry.getValue().getZipPath() + "!/" + fileEntry.getKey());
      }
    }
  }
}
