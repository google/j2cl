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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.common.JsUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes the static native js members accesses.
 *
 * <p>For example,
 *
 * <pre>
 * class A {
 *   {@literal @}JsMethod(namespace="Math")
 *   static native double abs(double x);
 * }
 * </pre>
 *
 * <p>A.abs() really refers to Javascript built-in Math.abs(). This pass replaces all method calls
 * to A.abs() with Math.abs().
 */
public class NormalizeStaticNativeMemberReferences extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!fieldDescriptor.isStatic()
                || !fieldDescriptor.isNative()
                || !fieldDescriptor.hasJsNamespace()) {
              return fieldAccess;
            }

            // A.abs -> Math.abs.
            FieldDescriptor newFieldescriptor =
                FieldDescriptor.Builder.from(fieldDescriptor)
                    .setEnclosingClassTypeDescriptor(getNamespaceAsTypeDescriptor(fieldDescriptor))
                    .build();
            checkArgument(fieldAccess.getQualifier() instanceof TypeReference);
            return FieldAccess.Builder.from(newFieldescriptor).build();
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
                    .setEnclosingClassTypeDescriptor(getNamespaceAsTypeDescriptor(methodDescriptor))
                    .build();
            checkArgument(methodCall.getQualifier() instanceof TypeReference);
            return MethodCall.Builder.from(newMethodDescriptor)
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  /**
   * Returns a TypeDescriptor to refer to the enclosing name that represents the member namespace;
   * this will transform a namespace=a.b.c on an JsMethod into a ficticious TypeDescriptor for
   * namespace=a.b and name=c.
   */
  private static TypeDescriptor getNamespaceAsTypeDescriptor(MemberDescriptor memberDescriptor) {
    String memberJsNamespace = memberDescriptor.getJsNamespace();
    if (JsUtils.isGlobal(memberJsNamespace)) {
      return TypeDescriptors.GLOBAL_NAMESPACE;
    }

    List<String> components = Splitter.on('.').omitEmptyStrings().splitToList(memberJsNamespace);
    List<String> namespaceComponents = components.subList(0, components.size() - 1);
    boolean isExtern = namespaceComponents.size() < 1;
    String jsName = Iterables.getLast(components);
    String jsNamespace =
        isExtern ? JsUtils.JS_PACKAGE_GLOBAL : Joiner.on(".").join(namespaceComponents);

    TypeDescriptor typeDescriptor = null;
    if (isExtern) {
      typeDescriptor = TypeDescriptors.createNative(jsNamespace, jsName, Collections.emptyList());
    } else {
      TypeDescriptor enclosingClassTypeDescriptor =
          getOutermostEnclosingType(memberDescriptor.getEnclosingClassTypeDescriptor());
      String packageName =
          Joiner.on(".").join(enclosingClassTypeDescriptor.getQualifiedSourceName(), jsNamespace);

      List<String> classComponents = new ArrayList<>();
      classComponents.add(jsName);

      typeDescriptor =
          TypeDescriptors.createNative(
              packageName, classComponents, jsNamespace, jsName, Collections.emptyList());
    }

    return typeDescriptor;
  }

  private static TypeDescriptor getOutermostEnclosingType(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.getEnclosingTypeDescriptor() == null) {
      return typeDescriptor;
    }
    return getOutermostEnclosingType(typeDescriptor.getEnclosingTypeDescriptor());
  }
}
