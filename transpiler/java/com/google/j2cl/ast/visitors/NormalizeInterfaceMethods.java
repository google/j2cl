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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Devirtualizes default and private instance interface methods and rewrites the corresponding
 * calls.
 */
public class NormalizeInterfaceMethods extends NormalizationPass {

  private static final String DEFAULT_POSTFIX = "__$default";
  private static final String PRIVATE_POSTFIX = "__$private";

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    devirtualizeInterfaceMethods(compilationUnit);
    devirtualizeMethodCalls(compilationUnit);
  }

  private void devirtualizeInterfaceMethods(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      if (!type.isInterface()) {
        continue;
      }
      List<Method> devirtualizedMethods = new ArrayList<>();
      for (Method method : type.getMethods()) {
        MethodDescriptor methodDescriptor = method.getDescriptor();
        checkState(!methodDescriptor.isJsOverlay());

        if (methodDescriptor.isDefaultMethod()) {
          devirtualizedMethods.add(AstUtils.devirtualizeMethod(method, DEFAULT_POSTFIX));
          AstUtils.stubMethod(method);
        } else if (isInterfacePrivateInstanceMethod(methodDescriptor)) {
          devirtualizedMethods.add(AstUtils.devirtualizeMethod(method, PRIVATE_POSTFIX));
        }
      }
      type.getMembers().removeIf(NormalizeInterfaceMethods::isInterfacePrivateInstanceMethod);
      type.addMethods(devirtualizedMethods);
    }
  }

  private static boolean isInterfacePrivateInstanceMethod(Member member) {
    return isInterfacePrivateInstanceMethod(member.getDescriptor());
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

  private void devirtualizeMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
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
