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
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.InitializerBlock;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Normalizes JsEnum classes into a native JsEnum with overlays and a JsEnum that represents the
 * closure enum directly.
 */
public class NormalizeJsEnums extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    normalizeNonNativeJsEnums(compilationUnit);
    normalizeJsEnumMemberReferencesToJsOverlay(compilationUnit);
    fixEnumMethodCalls(compilationUnit);
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

    Expression initializer = getJsEnumConstantValue(field);
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

  /** Returns the initialization value for a JsEnum constant. */
  private static Expression getJsEnumConstantValue(Field field) {
    checkState(field.isEnumField());
    NewInstance initializer = (NewInstance) field.getInitializer();
    List<Expression> arguments = initializer.getArguments();
    if (arguments.size() != 1) {
      // Not a valid initialization. The code will be rejected.
      return null;
    }
    return arguments.get(0);
  }

  /** Convert the JsEnum to a native JsEnum so that they are handled uniformly. */
  private static void convertJsEnumToNative(Type type) {

    type.setMembers(
        type.getMembers().stream()
            .filter(NormalizeJsEnums::shouldMoveToNativeType)
            .map(NormalizeJsEnums::createJsOverlayMember)
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
    } else if (member.isMethod()) {
      Method method = (Method) member;
      return Method.Builder.from(method)
          .setMethodDescriptor(toJsOverlay(method.getDescriptor()))
          .build();
    } else {
      checkArgument(member.isStatic());
      InitializerBlock initializerBlock = (InitializerBlock) member;
      return InitializerBlock.Builder.from(initializerBlock)
          .setDescriptor(toJsOverlay(initializerBlock.getDescriptor()))
          .build();
    }
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

  /**
   * Rewrite method calls to Enum methods on JsEnum instances to preserve the AST consistent.
   *
   * <p>Many passes rely on the enclosing type of the method to make decisions such as
   * devirtualizing, conversion, etc. But since JsEnums do not really extend Enum this prevents
   * those passes from taking decisions based on incorrect information.
   */
  private void fixEnumMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();

            if (!methodDescriptor.isPolymorphic()) {
              // Only rewrite polymorphic methods.
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

            if (methodCall.getTarget().getMethodSignature().equals("ordinal()")) {
              return castJsEnumToValue(methodCall);
            }

            return MethodCall.Builder.from(methodCall)
                .setMethodDescriptor(fixEnumMethodDescriptor(methodDescriptor))
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

  private MethodDescriptor fixEnumMethodDescriptor(MethodDescriptor methodDescriptor) {
    MethodDescriptor declarationMethodDescriptor = methodDescriptor.getDeclarationDescriptor();
    String declarationMethodSignature = declarationMethodDescriptor.getMethodSignature();

    // Reroute overridden methods to the super method they override.
    if (declarationMethodSignature.equals("compareTo(java.lang.Enum)")) {
      return TypeDescriptors.get()
          .javaLangComparable
          .getMethodDescriptorByName("compareTo", TypeDescriptors.get().javaLangObject);
    }
    if (declarationMethodDescriptor.isOrOverridesJavaLangObjectMethod()) {
      return MethodDescriptor.Builder.from(declarationMethodDescriptor)
          .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
          .build();
    }

    throw new AssertionError();
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
