/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.common.Problems;
import com.google.j2cl.common.visitor.ProcessorError;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Type;

/** The base class for all J2cl Normalization passes. */
public abstract class NormalizationPass {
  private CompilationUnit currentCompilationUnit;
  private Problems problems;

  public final void setProblems(Problems problems) {
    this.problems = problems;
  }

  protected Problems getProblems() {
    return problems;
  }

  public final void execute(CompilationUnit compilationUnit) {
    currentCompilationUnit = compilationUnit;
    try {
      applyTo(compilationUnit);
    } catch (RuntimeException | Error e) {
      String filePath;
      if (compilationUnit.isSynthetic()) {
        filePath = "[Synthetic CompilationUnit]";
      } else {
        filePath = compilationUnit.getFilePath();
      }
      throw new ProcessorError(filePath, filePath, e);
    }
    currentCompilationUnit = null;
  }

  public CompilationUnit getCompilationUnit() {
    return currentCompilationUnit;
  }

  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.streamTypes().forEach(this::applyTo);
  }

  public void applyTo(Type type) {}
}
