package com.google.j2cl.generator;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.common.VelocityUtil;
import com.google.j2cl.errors.Errors;

import org.apache.velocity.app.VelocityEngine;

import java.io.IOException;
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
 * output for the transpiler.  It is responsible for pulling in native sources and super sources
 * and then generating header and implementation files for each Java Type.
 */
public class JavaScriptGeneratorStage {
  private Charset charset;
  private Set<String> superSourceFiles;
  private Set<String> nativeJavaScriptFileZipPaths;
  private Errors errors;
  private FileSystem outputFileSystem;
  private String outputLocationPath;

  private final VelocityEngine velocityEngine = VelocityUtil.createEngine();

  public JavaScriptGeneratorStage(
      Charset charset,
      Set<String> superSourceFiles,
      Set<String> nativeJavaScriptFileZipPaths,
      FileSystem outputFileSystem,
      String outputLocationPath,
      Errors errors) {
    this.charset = charset;
    this.superSourceFiles = superSourceFiles;
    this.nativeJavaScriptFileZipPaths = nativeJavaScriptFileZipPaths;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.errors = errors;
  }

  public void generateJavaScriptSources(List<CompilationUnit> j2clCompilationUnits) {
    Map<String, NativeJavaScriptFile> nativeFiles = new HashMap<>();
    for (String nativeJavascriptFileZipPath : nativeJavaScriptFileZipPaths) {
      nativeFiles.putAll(
          NativeJavaScriptFile.getFilesByPathFromZip(
              nativeJavascriptFileZipPath, charset.name(), errors));
    }

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      if (superSourceFiles.contains(j2clCompilationUnit.getFilePath())) {
        continue;
      }

      for (JavaType javaType : j2clCompilationUnit.getTypes()) {
        JavaScriptImplGenerator jsImplGenerator =
            new JavaScriptImplGenerator(errors, javaType, velocityEngine);

        // TODO: This is a temporary fix to stop the transpiler from trying to find native sources
        // when we compile the JRE. The '!nativeJavaScriptFileZipPaths.isEmpty() &&' check should
        // be removed when the JRE is ready to go.
        if (!nativeJavaScriptFileZipPaths.isEmpty() && javaType.containsNativeMethods()) {
          String javaTypePath = GeneratorUtils.getRelativePath(javaType);
          NativeJavaScriptFile matchingNativeFile = nativeFiles.get(javaTypePath);
          if (matchingNativeFile == null) {
            errors.error(Errors.Error.ERR_NATIVE_JAVA_SOURCE_NO_MATCH, javaTypePath);
            return;
          }
          jsImplGenerator.setNativeSource(matchingNativeFile.getContent());
          matchingNativeFile.setUsed();
        }

        Path absolutePathForImpl =
            GeneratorUtils.getAbsolutePath(
                outputFileSystem,
                outputLocationPath,
                GeneratorUtils.getRelativePath(javaType),
                jsImplGenerator.getSuffix());
        jsImplGenerator.writeToFile(absolutePathForImpl, charset);

        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(errors, javaType, velocityEngine);
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
    for (Entry<String, NativeJavaScriptFile> fileEntry : nativeFiles.entrySet()) {
      if (!fileEntry.getValue().wasUsed()) {
        errors.error(
            Errors.Error.ERR_NATIVE_UNUSED_NATIVE_SOURCE,
            fileEntry.getValue().getZipPath() + "!/" + fileEntry.getKey());
      }
    }

    maybeCloseFileSystem();
  }

  private void maybeCloseFileSystem() {
    if (outputFileSystem instanceof com.sun.nio.zipfs.ZipFileSystem) {
      try {
        outputFileSystem.close();
      } catch (IOException e) {
        errors.error(Errors.Error.ERR_CANNOT_CLOSE_ZIP);
      }
    }
  }
}
