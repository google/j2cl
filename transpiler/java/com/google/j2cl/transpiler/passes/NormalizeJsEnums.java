/*
 * Copyright 2018 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Iterables;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Normalizes JsEnum classes into a native JsEnum with overlays and a JsEnum that represents the
 * closure enum directly.
 */
public class NormalizeJsEnums extends NormalizationPass {
  @Override
  public void applyTo(Type type) {
    if (type.isJsEnum()) {
      // TODO(b/116751296): Once the type model is refactored it should reflect the correct
      // superclass for JsEnums.
      type.setSuperTypeDescriptor(null);
    }
    normalizeNonNativeJsEnums(type);
    fixEnumMethodCalls(type);
  }

  /**
   * Normalizes non native JsEnums into a Native JsEnum with JsOverlay members and an non native
   * JsEnum with only the Enum constants.
   */
  private static void normalizeNonNativeJsEnums(Type type) {
    if (!AstUtils.isNonNativeJsEnum(type.getTypeDescriptor())) {
      return;
    }
    type.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Node rewriteField(Field field) {
            if (AstUtils.isJsEnumCustomValueField(field.getDescriptor())) {
              // remove value field which are only needed for definition.
              return null;
            }

            if (field.getDescriptor().getOrigin() == FieldOrigin.SYNTHETIC_ORDINAL_FIELD) {
              // remove synthesized ordinal constants since there are not needed for JsEnum.
              return null;
            }

            if (field.isEnumField()) {
              return createJsEnumConstant(field);
            }

            checkArgument(field.isStatic());
            return field;
          }

          @Nullable
          @Override
          public Node rewriteMethod(Method method) {
            if (method.isConstructor()) {
              // remove constructor since it is only needed for definition.
              return null;
            }

            return method;
          }
        });
  }

  /**
   * Rewrites the initializer enum constants by extracting the literal for the constructor
   * invocation.
   */
  private static Field createJsEnumConstant(Field field) {
    return Field.Builder.from(field)
        .setDescriptor(AstUtils.createJsEnumConstantFieldDescriptor(field.getDescriptor()))
        .setInitializer(getJsEnumConstantValue(field))
        .build();
  }

  /** Returns the initialization value for a JsEnum constant. */
  private static Expression getJsEnumConstantValue(Field field) {
    checkState(field.isEnumField());

    List<Expression> arguments = ((NewInstance) field.getInitializer()).getArguments();
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    if (fieldDescriptor.getEnclosingTypeDescriptor().getJsEnumInfo().hasCustomValue()) {
      checkState(arguments.size() == 3);
      Expression constantValue = arguments.get(2);
      checkState(constantValue.isCompileTimeConstant());
      return constantValue;
    } else {
      checkState(arguments.size() == 2);
      return fieldDescriptor.getEnumOrdinalValue();
    }
  }

  /**
   * Rewrite method calls to Enum methods on JsEnum instances to preserve the AST consistent.
   *
   * <p>Many passes rely on the enclosing type of the method to make decisions such as
   * devirtualizing, conversion, etc. But since JsEnums do not really extend Enum this prevents
   * those passes from taking decisions based on incorrect information.
   */
  private static void fixEnumMethodCalls(Type type) {
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
            TypeDescriptor qualifierTypeDescriptor = qualifier.getTypeDescriptor();
            if (!qualifierTypeDescriptor.isJsEnum()) {
              // Not a JsEnum receiver, nothing to do.
              return methodCall;
            }

            String targetSignature = methodDescriptor.getDeclarationDescriptor().getSignature();
            if (targetSignature.equals("equals(java.lang.Object)")) {
              Expression argument = Iterables.getOnlyElement(methodCall.getArguments());
              if (canAvoidBoxing(qualifierTypeDescriptor, argument.getTypeDescriptor())) {
                return RuntimeMethods.createEnumsEqualsMethodCall(qualifier, argument);
              }
            }

            if (targetSignature.equals("compareTo(java.lang.Enum)")) {
              Expression argument = Iterables.getOnlyElement(methodCall.getArguments());
              if (canAvoidBoxing(qualifierTypeDescriptor, argument.getTypeDescriptor())) {
                return RuntimeMethods.createEnumsCompareToMethodCall(qualifier, argument);
              }

              // Redirect the call to Comparable.compareTo(Object) since JsEnums don't actually
              // extend Enum.
              return MethodCall.Builder.from(methodCall)
                  .setTarget(
                      TypeDescriptors.get()
                          .javaLangComparable
                          .getMethodDescriptor("compareTo", TypeDescriptors.get().javaLangObject))
                  .build();
            }

            if (targetSignature.equals("ordinal()")) {
              return AstUtils.castJsEnumToValue(
                  methodCall.getQualifier(), methodCall.getTypeDescriptor());
            }

            return MethodCall.Builder.from(methodCall)
                .setTarget(fixEnumMethodDescriptor(methodDescriptor))
                .build();
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            if (AstUtils.isJsEnumCustomValueField(fieldAccess.getTarget())) {
              return AstUtils.castJsEnumToValue(
                  fieldAccess.getQualifier(), fieldAccess.getTypeDescriptor());
            }

            return fieldAccess;
          }
        });
  }

  private static boolean canAvoidBoxing(
      TypeDescriptor qualifierTypeDescriptor, TypeDescriptor argumentTypeDescriptor) {
    // Use a non boxing version of equals, compareTo when comparing two non-native JsEnums of the
    // same type.
    // We don't do this for native JsEnums because they are generally not boxed here and this allows
    // us to specialize the code for non-native JsEnums to avoid a trampoline.
    return AstUtils.isNonNativeJsEnum(qualifierTypeDescriptor)
        && argumentTypeDescriptor.hasSameRawType(qualifierTypeDescriptor);
  }

  private static MethodDescriptor fixEnumMethodDescriptor(MethodDescriptor methodDescriptor) {
    MethodDescriptor declarationMethodDescriptor = methodDescriptor.getDeclarationDescriptor();

    // Reroute overridden methods to the super method they override.
    if (declarationMethodDescriptor.isOrOverridesJavaLangObjectMethod()) {
      return MethodDescriptor.Builder.from(declarationMethodDescriptor)
          .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
          .build();
    }

    throw new InternalCompilerError("Unexpected Enum method: %s.", methodDescriptor);
  }
}
