/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;

/**
 * Inserts type annotation for 'new' a generic type or a JsFunction type.
 */
public class InsertCastOnNewInstancesVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertCastOnNewInstancesVisitor().insertCastOnNewInstances(compilationUnit);
  }

  private void insertCastOnNewInstances(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    if (newInstance.getTypeDescriptor().isParameterizedType()
        || newInstance.getTypeDescriptor().isJsFunctionImplementation()) {
      // add type annotation to ClassInstanceCreation of generic type and JsFunction type.
      return CastExpression.createRawNonNullable(newInstance, newInstance.getTypeDescriptor());
    } else {
      return newInstance;
    }
  }
}
