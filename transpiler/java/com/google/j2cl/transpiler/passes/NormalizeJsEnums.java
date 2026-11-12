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

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import javax.annotation.Nullable;

/**
 * Normalizes JsEnums for Closure, where they are represented by their values, i.e. non-native
 * JsEnums become Closure enums.
 */
public class NormalizeJsEnums extends NormalizeJsEnumsBase {
  @Nullable
  @Override
  DeclaredTypeDescriptor getImplementationType(TypeDescriptor jsEnum) {
    // JsEnums are represented by their values.
    return null;
  }

  /** Inserts type-consistency casts for JsEnum values when used as their value type. */
  @Override
  Expression extractValue(Expression jsEnum, TypeDescriptor valueTypeDescriptor) {
    checkArgument(jsEnum.getTypeDescriptor().isJsEnum());
    // In order to preserve type consistency, expressions like
    //
    //     getEnum()  // where getEnum() returns MyJsEnum.
    //
    // will be rewritten as
    //
    //     /** @type {int} */ ((MyJsEnum) getEnum())
    //
    // The inner Java cast to MyJsEnum guarantees that any conversion for getEnum() is preserved
    // (e.g. erasure casts if getEnum() returned T and was specialized to MyJsEnum in the calling
    // context; this allows the expression to be unboxed if needed).
    // The outer JsDoc cast guarantees that the expression is treated as of being the type of value
    // and conversions such as boxing are correctly preserved (e.g. if the expression was assigned
    // to an Integer variable).
    return JsDocCastExpression.builder()
        .setCastTypeDescriptor(valueTypeDescriptor)
        .setExpression(
            CastExpression.builder()
                .setCastTypeDescriptor(jsEnum.getTypeDescriptor())
                .setExpression(jsEnum)
                .build())
        .build();
  }

  @Override
  Expression rewriteCompareTo(MethodCall methodCall) {
    Expression qualifier = methodCall.getQualifier();
    Expression argument = methodCall.getArguments().getFirst();
    if (canAvoidBoxing(qualifier.getTypeDescriptor(), argument)) {
      return RuntimeMethods.createEnumsMethodCall("compareTo", qualifier, argument);
    }
    return super.rewriteCompareTo(methodCall);
  }

  private static boolean canAvoidBoxing(
      TypeDescriptor qualifierTypeDescriptor, Expression argument) {
    // Use a non boxing version of compareTo when comparing two non-native JsEnums of the same type,
    // or a non-native JsEnum with null.
    // We don't do this for native JsEnums because they are generally not boxed here and this allows
    // us to specialize the code for non-native JsEnums to avoid a trampoline.
    return AstUtils.isNonNativeJsEnum(qualifierTypeDescriptor)
        && (argument instanceof NullLiteral
            || argument.getTypeDescriptor().hasSameRawType(qualifierTypeDescriptor));
  }
}
