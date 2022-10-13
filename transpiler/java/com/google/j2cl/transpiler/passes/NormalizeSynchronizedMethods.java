/*
 * Copyright 2022 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/** Normalizes synchronized methods by wrapping its body with synchronized statement. */
public class NormalizeSynchronizedMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (!methodDescriptor.isSynchronized()) {
              return method;
            }
            methodDescriptor =
                MethodDescriptor.Builder.from(methodDescriptor).setSynchronized(false).build();
            SourcePosition sourcePosition = method.getBody().getSourcePosition();
            return Method.Builder.from(method)
                .setMethodDescriptor(methodDescriptor)
                .setStatements(
                    SynchronizedStatement.newBuilder()
                        .setExpression(
                            methodDescriptor.isStatic()
                                ? new TypeLiteral(
                                    sourcePosition, methodDescriptor.getEnclosingTypeDescriptor())
                                : new ThisReference(
                                    method.getDescriptor().getEnclosingTypeDescriptor()))
                        .setSourcePosition(sourcePosition)
                        .setBody(method.getBody())
                        .build())
                .build();
          }
        });
  }
}
