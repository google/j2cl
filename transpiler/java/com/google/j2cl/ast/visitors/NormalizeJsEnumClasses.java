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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.stream.Collectors;

/**
 * Normalizes JsEnum classes into a native JsEnum with overlays and a JsEnum that represents the
 * closure enum directly.
 */
public class NormalizeJsEnumClasses extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    normalizeNonNativeJsEnums(compilationUnit);
    normalizeJsEnumMemberReferencesToJsOverlay(compilationUnit);
  }

  /**
   * Normalizes non native JsEnums into a Native JsEnum with JsOverlay members and an non native
   * JsEnum with only the Enum constants.
   */
  private void normalizeNonNativeJsEnums(CompilationUnit compilationUnit) {
    for (Type type : ImmutableList.copyOf(compilationUnit.getTypes())) {
      if (type.getDeclaration().isJsEnum() && !type.isNative()) {
        compilationUnit.addType(createClosureJsEnum(type));
        convertJsEnumToNative(type);
      }
    }
  }

  /**
   * Extracts a type that represents the closure enum containing only fields derived from the enum
   * fields in {@code type}.
   */
  private static Type createClosureJsEnum(Type type) {
    Type jsEnumType =
        new Type(type.getSourcePosition(), type.getVisibility(), type.getDeclaration());

    int nextOrdinal = 0;
    // rewrite enum fields to be initialized directly the corresponding literal
    for (Field enumField : type.getEnumFields()) {
      jsEnumType.addField(createClosureEnumConstant(enumField, nextOrdinal++));
    }
    return jsEnumType;
  }

  /**
   * Rewrites the initializer enum constants by extracting the literal for the constructor
   * invocation.
   */
  private static Field createClosureEnumConstant(Field field, int ordinal) {

    Expression initializer = AstUtils.getJsEnumConstantValue(field);
    if (initializer == null) {
      initializer = NumberLiteral.fromInt(ordinal);
    }

    FieldDescriptor fieldDescriptor = field.getDescriptor();
    TypeDescriptor closureEnumUnderlyingType =
        AstUtils.getJsEnumValueFieldType(
            fieldDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());

    FieldDescriptor closureEnumConstantFieldDescriptor =
        FieldDescriptor.Builder.from(fieldDescriptor)
            .setTypeDescriptor(closureEnumUnderlyingType)
            .setFinal(true)
            .setCompileTimeConstant(true)
            .setEnumConstant(false)
            .build();
    return Field.Builder.from(field)
        .setDescriptor(closureEnumConstantFieldDescriptor)
        .setInitializer(initializer)
        .build();
  }

  /** Convert the JsEnum to a native JsEnum so that they are handled uniformly. */
  private static void convertJsEnumToNative(Type type) {

    type.setMembers(
        type.getMembers().stream()
            .filter(NormalizeJsEnumClasses::shouldMoveToNativeType)
            .map(NormalizeJsEnumClasses::createJsOverlayMember)
            .collect(Collectors.toList()));

    // Change to a native type.
    type.setTypeDeclaration(createNativeEnumTypeDeclaration(type.getDeclaration()));
  }

  /** Returns the type declaration corresponding to the native enum. */
  private static TypeDeclaration createNativeEnumTypeDeclaration(TypeDeclaration typeDeclaration) {
    return TypeDeclaration.Builder.from(typeDeclaration)
        .setClassComponents(
            ImmutableList.<String>builder()
                .addAll(typeDeclaration.getClassComponents())
                .add("$ClosureEnum")
                .build())
        .setCustomizedJsNamespace(typeDeclaration.getJsNamespace())
        .setSimpleJsName(typeDeclaration.getSimpleJsName())
        .setJsType(true)
        .setNative(true)
        .build();
  }

  private static boolean shouldMoveToNativeType(Member member) {
    if (member.isEnumField()) {
      return false;
    }
    if (AstUtils.isJsEnumCustomValueField(member.getDescriptor())) {
      // remove value field which are only needed for definition.
      return false;
    }
    if (member.isConstructor()) {
      // remove constructor since it is only needed for definition.
      return false;
    }
    return true;
  }

  /**
   * Converts member to {@code @JsOverlay} so that it is handled by {@link
   * NormalizeJsOverlayMembers}.
   */
  private static Member createJsOverlayMember(Member member) {
    if (member.isField()) {
      checkArgument(member.isStatic());
      Field field = (Field) member;
      return Field.Builder.from(field).setDescriptor(toJsOverlay(field.getDescriptor())).build();
    }
    if (member.isMethod()) {
      Method method = (Method) member;
      return Method.Builder.from(method)
          .setMethodDescriptor(toJsOverlay(method.getDescriptor()))
          .build();
    }

    checkArgument(member.isStatic());
    return member;
  }

  /** Rewrites member references to JsOverlay references. */
  private void normalizeJsEnumMemberReferencesToJsOverlay(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!fieldDescriptor.getEnclosingTypeDescriptor().isJsEnum()
                || fieldDescriptor.isEnumConstant()
                || AstUtils.isJsEnumCustomValueField(fieldDescriptor)) {
              return fieldAccess;
            }

            // References to static fields now need to be rewritten as references to the
            // JsOverlay field.
            return FieldAccess.Builder.from(fieldAccess)
                .setTargetFieldDescriptor(toJsOverlay(fieldAccess.getTarget()))
                .build();
          }

          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            if (!methodDescriptor.getEnclosingTypeDescriptor().isJsEnum()) {
              return methodCall;
            }

            // JsEnum method calls now need to be rewritten as calls to the overlay method.
            return MethodCall.Builder.from(methodCall)
                .setMethodDescriptor(toJsOverlay(methodDescriptor))
                .build();
          }
        });
  }

  private static FieldDescriptor toJsOverlay(FieldDescriptor fieldDescriptor) {
    checkState(fieldDescriptor.isStatic());
    return FieldDescriptor.Builder.from(fieldDescriptor)
        .setJsInfo(JsInfo.Builder.from(fieldDescriptor.getJsInfo()).setJsOverlay(true).build())
        .build();
  }

  private static MethodDescriptor toJsOverlay(MethodDescriptor methodDescriptor) {
    return MethodDescriptor.Builder.from(methodDescriptor)
        .setJsInfo(JsInfo.Builder.from(methodDescriptor.getJsInfo()).setJsOverlay(true).build())
        .build();
  }
}
