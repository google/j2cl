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
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;

/**
 * Normalizes synchronized constructs by inserting implied lock fields for direct subclasses of
 * Object with synchronized methods, and rewriting synchronized methods and blocks to use these
 * fields.
 */
public final class NormalizeSynchronizedConstructs extends NormalizationPass {

  /** Perform normalization of synchronized constructs as described in the class javadoc. */
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    insertImpliedMonitorFields(compilationUnit);
    normalizeSynchronizedMethods(compilationUnit);
    normalizeSynchronizedStatements(compilationUnit);
  }

  /**
   *
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
   * class A {
   *   final J2ktMonitor impliedJ2ktMonitor = new J2ktMonitor();
   *   synchronized void foo() {}
   * }
   * </pre>
   */
  private void insertImpliedMonitorFields(CompilationUnit compilationUnit) {
    // First, insert the implied monitor for classes that are direct subclasses of Object.
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Node rewriteType(Type type) {
            DeclaredTypeDescriptor j2ktMonitor = TypeDescriptors.get().javaemulLangJ2ktMonitor;
            DeclaredTypeDescriptor superTypeDescriptor =
                type.getTypeDescriptor().getSuperTypeDescriptor();
            // Insert the implied monitor for classes that are direct subclasses of Object.
            if (type.getTypeDescriptor().isClass()
                && superTypeDescriptor != null
                && TypeDescriptors.isJavaLangObject(superTypeDescriptor)
                && type.getMethods().stream().anyMatch(it -> it.getDescriptor().isSynchronized())) {
              type.getMembers()
                  .add(
                      Field.Builder.from(createImpliedLockFieldDescriptor(type.getTypeDescriptor()))
                          .setInitializer(
                              NewInstance.newBuilder()
                                  .setTarget(j2ktMonitor.getSingleConstructor())
                                  .build())
                          .setSourcePosition(SourcePosition.NONE)
                          .build());
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

  /**
   * Rewrite the monitor expression for synchronized blocks to ".impliedJ2ktMonitor" if it's not
   * already a J2kt(compatible) monitor and not a class.
   *
   * <pre>
   * synchronized (obj) {
   *   ...
   * }
   * </pre>
   *
   * <p>is rewritten to:
   *
   * <pre>
   * synchronized (obj.impliedJ2ktMonitor) {
   *   ...
   * }
   * </pre>
   */
  private void normalizeSynchronizedStatements(CompilationUnit compilationUnit) {

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteSynchronizedStatement(SynchronizedStatement statement) {
            TypeDescriptor type = statement.getExpression().getTypeDescriptor();
            return !type.isSameBaseType(
                        TypeDescriptors.get().comGoogleCommonBaseJ2ktCompatibleMonitor)
                    && !type.isAssignableTo(TypeDescriptors.get().javaemulLangJ2ktMonitor)
                    && !type.isSameBaseType(TypeDescriptors.get().javaLangClass)
                ? SynchronizedStatement.Builder.from(statement)
                    .setExpression(
                        FieldAccess.Builder.from(
                                createImpliedLockFieldDescriptor(
                                    getCurrentType().getTypeDescriptor()))
                            .setQualifier(statement.getExpression())
                            .build())
                    .build()
                : statement;
          }
        });
  }

  /** Creates a field descriptor for the "impliedJ2kMonitor" typed "java.emul.lang.J2ktMonitor" */
  private static FieldDescriptor createImpliedLockFieldDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor) {
    // Note that this can't be private as synchronization on the containing object from anywhere
    // will be transformed to use this field.
    return FieldDescriptor.newBuilder()
        .setName("impliedJ2ktMonitor")
        .setFinal(true)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setSynthetic(true)
        .setTypeDescriptor(TypeDescriptors.get().javaemulLangJ2ktMonitor.toNonNullable())
        .build();
  }
}


