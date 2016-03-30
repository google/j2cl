/*
 * Copyright 2015 Google Inc.
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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeReference;

/**
 * Normalizes the static native js method calls with the real native method calls.
 *
 * <p>For example,
 * class A {
 *   @JsMethod(namespace="Math") static native double abs(double x);
 * }
 * A.abs() really refers to Javascript built-in Math.abs().
 *
 * <p>This pass replaces all method calls to A.abs() with Math.abs().
 */
public class NormalizeNativeMethodCalls extends AbstractRewriter {

  private void normalizeNativeMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeNativeMethodCalls().normalizeNativeMethodCalls(compilationUnit);
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    MethodDescriptor methodDescriptor = methodCall.getTarget();
    if (!methodDescriptor.isStatic()
        || !methodDescriptor.isNative()
        || !methodDescriptor.hasJsNamespace()) {
      return methodCall;
    }

    // A.abs() -> Math.abs().
    MethodDescriptor newMethodDescriptor =
        MethodDescriptor.Builder.from(methodDescriptor)
            .enclosingClassTypeDescriptor(
                TypeDescriptor.createNative(methodDescriptor.getJsNamespace()))
            .build();
    Preconditions.checkArgument(methodCall.getQualifier() instanceof TypeReference);
    return MethodCall.createRegularMethodCall(null, newMethodDescriptor, methodCall.getArguments());
  }
}
