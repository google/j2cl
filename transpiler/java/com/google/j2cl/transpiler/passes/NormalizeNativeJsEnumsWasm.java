/*
 * Copyright 2026 Google Inc.
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

import static com.google.common.collect.MoreCollectors.onlyElement;

import com.google.common.collect.Iterables;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Normalizes Native JsEnums for Wasm. */
public class NormalizeNativeJsEnumsWasm extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    if (AstUtils.isNativeJsEnum(type.getTypeDescriptor())) {
      // Remove the custom value field, which doesn't exist in JS, to prevent generating native
      // accessors for it.
      type.getMembers().removeIf(m -> AstUtils.isJsEnumCustomValueField(m.getDescriptor()));
    }

    fixEnumMemberAccesses(type);
  }

  /** Rewrite method calls to Enum methods on JsEnum instances to keep the AST consistent. */
  private static void fixEnumMemberAccesses(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();

            if (!methodDescriptor.isInstanceMember()) {
              // Only rewrite instance methods.
              return methodCall;
            }
            if (!TypeDescriptors.isJavaLangEnum(methodDescriptor.getEnclosingTypeDescriptor())) {
              // Not a java.lang.Enum method, nothing to do.
              return methodCall;
            }
            Expression qualifier = methodCall.getQualifier();
            if (!AstUtils.isNativeJsEnum(qualifier.getTypeDescriptor().toRawTypeDescriptor())) {
              // Not a native JsEnum receiver, nothing to do.
              return methodCall;
            }

            String targetSignature = methodDescriptor.getDeclarationDescriptor().getSignature();
            if (targetSignature.equals("equals(java.lang.Object)")) {
              Expression argument = Iterables.getOnlyElement(methodCall.getArguments());
              // Use == for comparison instead of equals() to avoid crossing the boundary.
              return qualifier.infixEquals(argument);
            }

            if (targetSignature.equals("compareTo(java.lang.Enum)")) {
              Expression argument = Iterables.getOnlyElement(methodCall.getArguments());
              // Redirect the call to Comparable.compareTo(Object) since JsEnums don't actually
              // extend Enum.
              return methodCall.toBuilder()
                  .setQualifier(
                      CastExpression.builder()
                          .setExpression(qualifier)
                          .setCastTypeDescriptor(TypeDescriptors.get().javaLangComparable)
                          .build())
                  .setArguments(argument)
                  .setTarget(
                      TypeDescriptors.get()
                          .javaLangComparable
                          .getMethodDescriptor("compareTo", TypeDescriptors.get().javaLangObject))
                  .build();
            }

            return methodCall.toBuilder()
                .setTarget(fixEnumMethodDescriptor(methodDescriptor))
                .build();
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            if (AstUtils.isJsEnumCustomValueField(fieldAccess.getTarget())
                && AstUtils.isNativeJsEnum(fieldAccess.getTarget().getEnclosingTypeDescriptor())) {
              return extractJsEnumValue(
                  fieldAccess.getQualifier(), fieldAccess.getTypeDescriptor());
            }

            return fieldAccess;
          }
        });
  }

  private static Expression extractJsEnumValue(
      Expression expression, TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor primitiveTypeDescriptor) {
      return MethodCall.builderFrom(
              TypeDescriptors.get()
                  .javaLangNumber
                  .getMethodDescriptor(primitiveTypeDescriptor.getSimpleSourceName() + "Value"))
          .setQualifier(
              CastExpression.builder()
                  .setExpression(expression)
                  .setCastTypeDescriptor(TypeDescriptors.get().javaLangDouble)
                  .build())
          .build();
    }

    return CastExpression.builder()
        .setExpression(expression)
        .setCastTypeDescriptor(typeDescriptor)
        .build();
  }

  private static MethodDescriptor fixEnumMethodDescriptor(MethodDescriptor methodDescriptor) {
    MethodDescriptor declarationMethodDescriptor = methodDescriptor.getDeclarationDescriptor();

    // Reroute overridden methods to the super method they override on java.lang.Object. We cannot
    // simply change the enclosing type of the Enum method descriptor because Enum methods like
    // hashCode() are final, which would result in a direct non-polymorphic call to Object.
    if (declarationMethodDescriptor.isOrOverridesJavaLangObjectMethod()) {
      return declarationMethodDescriptor.getJavaOverriddenMethodDescriptors().stream()
          .filter(m -> TypeDescriptors.isJavaLangObject(m.getEnclosingTypeDescriptor()))
          .collect(onlyElement());
    }

    throw new InternalCompilerError("Unexpected Enum method: %s.", methodDescriptor);
  }
}
