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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Type;

/** The base class for all J2cl Normalization passes. */
public abstract class NormalizationPass {
  private AbstractVisitor processor;

  public final void execute(CompilationUnit compilationUnit) {
    processor =
        new AbstractVisitor() {
          @Override
          public boolean enterCompilationUnit(CompilationUnit node) {
            applyTo(node);
            return false;
          }

          @Override
          public boolean enterType(Type node) {
            applyTo(node);
            return false;
          }
        };
    compilationUnit.accept(processor);
  }

  public CompilationUnit getCompilationUnit() {
    return processor.getCurrentCompilationUnit();
  }

  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.getTypes().forEach(t -> t.accept(processor));
  }

  public void applyTo(Type type) {}
}
