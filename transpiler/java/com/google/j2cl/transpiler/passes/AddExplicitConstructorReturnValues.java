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
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.ThisReference;

/**
 * Constructors in WASM are treated as methods that receive and return the instance; this pass adds
 * the explicit {@code return this;} at the end of the constructor as well as replacing the existing
 * {@code return;} with {@code return this;}.
 *
 * <p>Rewrites code like
 *
 * <p>
 *
 * <pre>@{code
 *    MyClass(int i) {
 *      if (i == 0) {
 *        return;
 *      }
 *      this.a = 3;
 *    }
 * }</pre>
 *
 * <p>into
 *
 * <p>
 *
 * <pre>@{code
 *    MyClass(int i) {
 *      if (i == 0) {
 *        return this;
 *      }
 *      this.a = 3;
 *      return this;
 *    }
 * }</pre>
 */
public class AddExplicitConstructorReturnValues extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public boolean shouldProcessMethod(Method method) {
            return method.isConstructor();
          }

          @Override
          public ReturnStatement rewriteReturnStatement(ReturnStatement returnStatement) {
            // Explicit return statements in the original constructor now need to return the
            // instance.
            return ReturnStatement.Builder.from(returnStatement)
                .setExpression(
                    new ThisReference(
                        getCurrentMember().getDescriptor().getEnclosingTypeDescriptor()))
                .build();
          }

          @Override
          public Method rewriteMethod(Method method) {
            // Add "return this" to the end of the constructor to return the instance.
            if (method.isConstructor()) {
              DeclaredTypeDescriptor enclosingTypeDescriptor =
                  method.getDescriptor().getEnclosingTypeDescriptor();
              method
                  .getBody()
                  .getStatements()
                  .add(
                      ReturnStatement.newBuilder()
                          .setSourcePosition(method.getSourcePosition())
                          .setTypeDescriptor(enclosingTypeDescriptor)
                          .setExpression(new ThisReference(enclosingTypeDescriptor))
                          .build());
            }
            return method;
          }
        });
  }
}
