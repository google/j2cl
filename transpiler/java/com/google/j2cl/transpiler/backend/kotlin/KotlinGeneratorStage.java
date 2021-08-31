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

import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;

/**
 * The OutputGeneratorStage contains all necessary information for generating the Kotlin output
 * for the transpiler. It is responsible for generating implementation files for each Java file.
 */
public class KotlinGeneratorStage {
  private final Problems problems;
  private final Output output;

  public KotlinGeneratorStage(Output output, Problems problems) {
    this.output = output;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {
      for (CompilationUnit compilationUnit : library.getCompilationUnits()) {
        for (Type type : compilationUnit.getTypes()) {
          KotlinGenerator ktGenerator = new KotlinGenerator(problems, type);
          String typeRelativePath = getPackageRelativePath(type.getDeclaration());
          String kotlinSource = ktGenerator.renderOutput();
          String relativePath = typeRelativePath + ktGenerator.getSuffix();
        output.write(relativePath, kotlinSource);
      }
    }
  }

  /** Returns the relative output path for a given type. */
  private static String getPackageRelativePath(TypeDeclaration typeDeclaration) {
    return OutputUtils.getPackageRelativePath(
        typeDeclaration.getPackageName(), typeDeclaration.getSimpleBinaryName());
  }
}
