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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import javax.annotation.Nullable;

/**
 * Devirtualizes default and private instance interface methods and rewrites the corresponding
 * calls.
 */
public class NormalizeInterfaceMethods extends NormalizationPass {

  private static final String DEFAULT_POSTFIX = "__$default";
  private static final String PRIVATE_POSTFIX = "__$private";

  @Override
  public void applyTo(Type type) {
    devirtualizeInterfaceMethods(type);
    devirtualizeMethodCalls(type);
  }

  private static void devirtualizeInterfaceMethods(Type type) {
    if (!type.isInterface()) {
      return;
    }
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor methodDescriptor = method.getDescriptor();
            checkState(!methodDescriptor.isJsOverlay());

            if (methodDescriptor.isDefaultMethod()) {
              type.addMember(AstUtils.devirtualizeMethod(method, DEFAULT_POSTFIX));
              // Retain the interface method declaration.
              return createInterfaceMethodDeclaration(method);
            } else if (isInterfacePrivateInstanceMethod(methodDescriptor)) {
              type.addMember(AstUtils.devirtualizeMethod(method, PRIVATE_POSTFIX));
              return null;
            } else {
              return method;
            }
          }
        });
  }

  private static Method createInterfaceMethodDeclaration(Method method) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    return Method.newBuilder()
        .setMethodDescriptor(
            MethodDescriptor.Builder.from(methodDescriptor)
                .setAbstract(true)
                .setFinal(false)
                .setDefaultMethod(false)
                .build())
        .setParameters(AstUtils.clone(method.getParameters()))
        .setSourcePosition(method.getSourcePosition())
        .build();
  }

  private static boolean isInterfacePrivateInstanceMethod(MemberDescriptor memberDescriptor) {
    if (!(memberDescriptor instanceof MethodDescriptor)) {
      return false;
    }
    MethodDescriptor methodDescriptor = (MethodDescriptor) memberDescriptor;
    return methodDescriptor.getEnclosingTypeDescriptor().isInterface()
        && methodDescriptor.getVisibility().isPrivate()
        && !methodDescriptor.isStatic();
  }

  private static void devirtualizeMethodCalls(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            checkState(!methodDescriptor.isJsOverlay());

            if (methodDescriptor.isDefaultMethod() && methodCall.isStaticDispatch()) {
              // Only redirect static dispatch to default methods, not instance dispatch. Instance
              // dispatch does not change if the method is declared as default.
              return AstUtils.devirtualizeMethodCall(methodCall, DEFAULT_POSTFIX);
            }
            if (isInterfacePrivateInstanceMethod(methodDescriptor)) {
              return AstUtils.devirtualizeMethodCall(methodCall, PRIVATE_POSTFIX);
            }
            return methodCall;
          }
        });
  }
}
