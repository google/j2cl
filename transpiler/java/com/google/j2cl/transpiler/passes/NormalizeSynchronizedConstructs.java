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
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/**
 * Normalizes synchronized constructs by inserting a monitor superclass for direct subclasses of
 * Object with synchronized methods, and rewriting synchronized methods to synchronized blocks.
 */
public final class NormalizeSynchronizedConstructs extends NormalizationPass {

  /** Perform normalization of synchronized constructs as described in the class javadoc. */
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    insertJ2ktMonitorSuperclass(compilationUnit);
    normalizeSynchronizedMethods(compilationUnit);
  }

  /**
   * Insert J2ktMonitor as super class for classes with synchronized instance methods. This enables
   * them to be used in Kotlin synhronized blocks.
   *
   * <pre>
   * class A {
   *   synchronized void foo() {}
   * }
   * </pre>
   *
   * <p>is rewritten to:
   *
   * <pre>
   * class A extends J2ktMonitor {
   *   synchronized void foo() {}
   * }
   * </pre>
   */
  private void insertJ2ktMonitorSuperclass(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteType(Type type) {
            // We can only insert monitor as a super class to the direct subclasses of j.l.Object.
            if (!type.getTypeDescriptor().isClass()
                || type.getSuperTypeDescriptor() == null
                || !TypeDescriptors.isJavaLangObject(type.getSuperTypeDescriptor())) {
              return type;
            }

            if (type.getMethods().stream().anyMatch(it -> it.getDescriptor().isSynchronized())) {
              type.setSuperTypeDescriptor(TypeDescriptors.get().javaemulLangJ2ktMonitor);
            }
            return type;
          }
        });
  }

  /**
   * Normalize synchronized methods by wrapping their body with a synchronized statement.
   * Note that we could insert a reference to implied monitor here already, but it will be done
   * at the next step anyway, so we keep this as an isolated transformation that could
   * run independently of the other transformations.
   * <pre>
   *   synchronized void foo() {}
   * </pr>
   *
   * <p>is rewritten to:
   *
   * <pre>
   *   synchronized (this) {
   *     void foo() {}
   *   }
   * </pre>
   */
  private void normalizeSynchronizedMethods(CompilationUnit compilationUnit) {
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


