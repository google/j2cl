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

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
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
    TypeDescriptor enumValueType =
        AstUtils.getJsEnumValueFieldType(
            field.getDescriptor().getEnclosingTypeDescriptor().getTypeDeclaration());
    FieldDescriptor closureEnumConstantFieldDescriptor =
        FieldDescriptor.Builder.from(field.getDescriptor())
            .setTypeDescriptor(enumValueType)
            .setFinal(true)
            .setCompileTimeConstant(true)
            .setEnumConstant(true)
            .build();
    return Field.Builder.from(field)
        .setDescriptor(closureEnumConstantFieldDescriptor)
        .setInitializer(getJsEnumConstantValue(field))
        .build();
  }

  /** Returns the initialization value for a JsEnum constant. */
  private static Expression getJsEnumConstantValue(Field field) {
    checkState(field.isEnumField());

    List<Expression> arguments = ((NewInstance) field.getInitializer()).getArguments();
    if (field.getDescriptor().getEnclosingTypeDescriptor().getJsEnumInfo().hasCustomValue()) {
      checkState(arguments.size() == 3);
      Expression constantValue = arguments.get(2);
      checkState(constantValue.isCompileTimeConstant());
      return constantValue;
    } else {
      checkState(arguments.size() == 2);
      return NumberLiteral.fromInt(field.getEnumOrdinal());
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
            TypeDescriptor qualifierTypeDescriptor = methodCall.getQualifier().getTypeDescriptor();
            if (!qualifierTypeDescriptor.isJsEnum()) {
              // Not a JsEnum receiver, nothing to do.
              return methodCall;
            }

            if (methodCall.getTarget().getSignature().equals("ordinal()")) {
              return castJsEnumToValue(methodCall);
            }

            return MethodCall.Builder.from(methodCall)
                .setTarget(fixEnumMethodDescriptor(methodDescriptor))
                .build();
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            if (AstUtils.isJsEnumCustomValueField(fieldAccess.getTarget())) {
              return castJsEnumToValue(fieldAccess);
            }

            return fieldAccess;
          }
        });
  }

  private static MethodDescriptor fixEnumMethodDescriptor(MethodDescriptor methodDescriptor) {
    MethodDescriptor declarationMethodDescriptor = methodDescriptor.getDeclarationDescriptor();
    String declarationMethodSignature = declarationMethodDescriptor.getSignature();

    // Reroute overridden methods to the super method they override.
    if (declarationMethodSignature.equals("compareTo(java.lang.Enum)")) {
      return TypeDescriptors.get()
          .javaLangComparable
          .getMethodDescriptor("compareTo", TypeDescriptors.get().javaLangObject);
    }
    if (declarationMethodDescriptor.isOrOverridesJavaLangObjectMethod()) {
      return MethodDescriptor.Builder.from(declarationMethodDescriptor)
          .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
          .build();
    }

    throw new InternalCompilerError("Unexpected Enum method: %s.", methodDescriptor);
  }

  /** Rewrite references like {@code q.value} and {@code q.ordinal()} to {@code q}. */
  private static Expression castJsEnumToValue(MemberReference memberReference) {
    // In order to preserve type consistency, expressions like
    //
    //     getEnum().ordinal()  // where getEnum() returns MyJsEnum.
    //
    // will be rewritten as
    //
    //     /** @type {int} */ ((MyJsEnum) getEnum())
    //
    // The inner Java cast to MyJsEnum guarantees that any conversion due to getEnum() being the
    // qualifier of ordinal() is preserved (e.g. erasure casts if getEnum() returned T and was
    // specialized to MyJsEnum in the calling context).
    // The outer JsDoc cast guarantees that the expression is treated as of being the type of value
    // and conversions such as boxing are correctly preserved (e.g. if the expression was assigned
    // to an Integer variable).
    return JsDocCastExpression.newBuilder()
        .setCastType(memberReference.getTypeDescriptor())
        .setExpression(
            CastExpression.newBuilder()
                .setCastTypeDescriptor(memberReference.getQualifier().getTypeDescriptor())
                .setExpression(memberReference.getQualifier())
                .build())
        .build();
  }
}
