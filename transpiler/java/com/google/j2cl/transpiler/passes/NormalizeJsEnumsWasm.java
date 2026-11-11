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

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/**
 * Normalizes JsEnums for Wasm, where non-native JsEnums extend the light enum classes in {@code
 * javaemul.internal.Enums} instead of {@code java.lang.Enum}, and native JsEnums are represented by
 * their JS values.
 *
 * <p>This pass needs to run after all the passes that rely on {@code java.lang.Enum} members, e.g.
 * {@code NormalizeEnumClasses} and {@code NormalizeSwitchConstructs}.
 */
public class NormalizeJsEnumsWasm extends NormalizeJsEnumsBase {
  @Nullable
  @Override
  DeclaredTypeDescriptor getImplementationType(TypeDescriptor jsEnum) {
    if (jsEnum.isNative()) {
      // Native JsEnums are represented by their JS values.
      return null;
    }
    if (!jsEnum.getJsEnumInfo().hasCustomValue()) {
      return TypeDescriptors.get().javaemulInternalOrdinalLightEnum;
    }
    return TypeDescriptors.isJavaLangString(AstUtils.getJsEnumValueFieldType(jsEnum))
        ? TypeDescriptors.get().javaemulInternalStringLightEnum
        : TypeDescriptors.get().javaemulInternalIntLightEnum;
  }

  @Override
  Expression extractValue(Expression jsEnum, TypeDescriptor valueTypeDescriptor) {
    if (valueTypeDescriptor instanceof PrimitiveTypeDescriptor primitiveTypeDescriptor) {
      return MethodCall.builderFrom(
              TypeDescriptors.get()
                  .javaLangNumber
                  .getMethodDescriptor(primitiveTypeDescriptor.getSimpleSourceName() + "Value"))
          .setQualifier(
              CastExpression.builder()
                  .setExpression(jsEnum)
                  .setCastTypeDescriptor(TypeDescriptors.get().javaLangDouble)
                  .build())
          .build();
    }

    return CastExpression.builder()
        .setExpression(jsEnum)
        .setCastTypeDescriptor(valueTypeDescriptor)
        .build();
  }
}
