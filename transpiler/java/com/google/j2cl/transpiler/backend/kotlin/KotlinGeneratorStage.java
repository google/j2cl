/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The OutputGeneratorStage contains all necessary information for generating the Kotlin output
 * for the transpiler. It is responsible for generating implementation files for each Java file.
 */
public class KotlinGeneratorStage {
  private ExecutorService fileService;
  private final Problems problems;
  private final Path outputPath;

  public KotlinGeneratorStage(
      Path outputPath,
      Problems problems) {
    this.outputPath = outputPath;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {
    fileService = Executors.newSingleThreadExecutor();
    try {
      for (CompilationUnit compilationUnit : library.getCompilationUnits()) {
        for (Type type : compilationUnit.getTypes()) {
          KotlinGenerator ktGenerator = new KotlinGenerator(problems, type);
          String typeRelativePath = getPackageRelativePath(type.getDeclaration());
          String kotlinSource = ktGenerator.renderOutput();
          String relativePath = typeRelativePath + ktGenerator.getSuffix();
          writeToFile(outputPath.resolve(relativePath), kotlinSource);
        }
      }
    } finally {
      awaitCompletionOfFileWrites();
      fileService = null;
    }
  }

  private void awaitCompletionOfFileWrites() {
    try {
      fileService.shutdown();
      fileService.awaitTermination(Long.MAX_VALUE, SECONDS);
    } catch (InterruptedException ie) {
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  private void writeToFile(Path outputPath, String content) {
    fileService.execute(() -> OutputUtils.writeToFile(outputPath, content, problems));
  }

  /** Returns the relative output path for a given type. */
  private static String getPackageRelativePath(TypeDeclaration typeDeclaration) {
    return OutputUtils.getPackageRelativePath(
        typeDeclaration.getPackageName(), typeDeclaration.getSimpleBinaryName());
  }
}
