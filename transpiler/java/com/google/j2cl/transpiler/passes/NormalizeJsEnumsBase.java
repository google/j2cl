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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MoreCollectors.toOptional;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
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
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Normalizes JsEnums, which are not represented at runtime as {@code java.lang.Enum}.
 *
 * <p>In the type model JsEnums extend {@code java.lang.Enum}, whereas when represented by their
 * values at runtime they have no superclass, their constants are their values, {@code ordinal()}
 * and the custom value field are the value, and the other members are redirected to the {@code
 * java.lang.Object} and {@code java.lang.Comparable} methods that {@code java.lang.Enum} overrides
 * (with {@link #rewriteEquals} and {@link #rewriteCompareTo} customizing equality and comparison).
 *
 * <p>JsEnums that are not represented by their values extend an implementation class instead of
 * {@code java.lang.Enum} (see {@link #getImplementationType}), to which the references to {@code
 * java.lang.Enum} members are redirected.
 */
abstract class NormalizeJsEnumsBase extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    if (type.isJsEnum()) {
      normalizeJsEnum(type);
    }
    rewriteEnumMemberReferences(type);
  }

  /**
   * Returns the class that {@code jsEnum} extends instead of {@code java.lang.Enum}, or {@code
   * null} if {@code jsEnum} is represented at runtime by its value.
   *
   * <p>The class declares or inherits the supported {@code java.lang.Enum} members, including the
   * constructor, with matching signatures (or overrides the same supertype methods).
   */
  @Nullable
  abstract DeclaredTypeDescriptor getImplementationType(TypeDescriptor jsEnum);

  /**
   * Returns an expression of type {@code valueTypeDescriptor} that evaluates to the value of the
   * JsEnum expression {@code jsEnum}.
   */
  abstract Expression extractValue(Expression jsEnum, TypeDescriptor valueTypeDescriptor);

  /**
   * Rewrites {@code jsEnum.equals(other)}, which by default compares by identity while checking the
   * receiver for {@code null}.
   */
  Expression rewriteEquals(MethodCall methodCall) {
    Expression argument = methodCall.getArguments().getFirst();
    if (argument.getTypeDescriptor().isPrimitive()) {
      argument =
          CastExpression.builder()
              .setExpression(argument)
              .setCastTypeDescriptor(TypeDescriptors.get().javaLangObject)
              .build();
    }
    // Use == for comparison instead of equals() to avoid Object.equals() dispatch or crossing the
    // JS boundary in Wasm, while preserving the NullPointerException on a null receiver.
    return methodCall.getQualifier().postfixNotNullAssertion().infixEquals(argument);
  }

  /**
   * Rewrites {@code jsEnum.compareTo(other)}, which by default calls {@code Comparable.compareTo()}
   * on the value.
   */
  Expression rewriteCompareTo(MethodCall methodCall) {
    return redirect(methodCall, TypeDescriptors.get().javaLangComparable);
  }

  /** Normalizes the declaration of a JsEnum. */
  private void normalizeJsEnum(Type type) {
    // TODO(b/116751296): Once the type model is refactored it should reflect the correct
    // superclass for JsEnums.
    DeclaredTypeDescriptor implementationType = getImplementationType(type.getTypeDescriptor());
    type.setSuperTypeDescriptor(implementationType);
    if (isJavaLangEnumWorkaround(implementationType)) {
      // The custom value field stays in the JsEnum.
    } else {
      // Remove the custom value field, which doesn't exist at runtime since the value is either
      // the JsEnum itself or a field of the implementation class.
      type.getMembers().removeIf(m -> AstUtils.isJsEnumCustomValueField(m.getDescriptor()));
    }
    if (implementationType != null) {
      redirectSuperConstructorCalls(type, implementationType);
    } else if (!type.isNative()) {
      initializeConstantsWithValues(type);
    }
  }

  /** Redirects the super constructor calls in {@code jsEnum} to {@code implementationType}. */
  private static void redirectSuperConstructorCalls(
      Type jsEnum, DeclaredTypeDescriptor implementationType) {
    jsEnum.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            return methodDescriptor.isConstructor()
                    && TypeDescriptors.isJavaLangEnum(methodDescriptor.getEnclosingTypeDescriptor())
                ? redirect(methodCall, implementationType)
                : methodCall;
          }
        });
  }

  /**
   * Initializes the constants of {@code jsEnum} with their values, removing the constructors and
   * the ordinal constants which are only needed for the instantiations.
   */
  private static void initializeConstantsWithValues(Type jsEnum) {
    jsEnum.accept(
        new AbstractRewriter() {
          @Nullable
          @Override
          public Node rewriteField(Field field) {
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
    return field.toBuilder()
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
   * Rewrites the references to {@code java.lang.Enum} members on JsEnums.
   *
   * <p>Many passes rely on the enclosing type of the method to make decisions such as
   * devirtualizing, conversion, etc. But since JsEnums do not really extend Enum this prevents
   * those passes from taking decisions based on incorrect information.
   */
  private void rewriteEnumMemberReferences(Type type) {
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
            // JsEnums can't be cast to java.lang.Enum, and erasure casts are inserted after this pass,
            // so casts to java.lang.Enum don't hide JsEnum receivers.
            checkState(
                !(qualifier instanceof CastExpression castExpression
                    && TypeDescriptors.isJavaLangEnum(castExpression.getCastTypeDescriptor())
                    && castExpression.getExpression().getTypeDescriptor().isJsEnum()));
            TypeDescriptor qualifierTypeDescriptor = qualifier.getTypeDescriptor();
            if (!qualifierTypeDescriptor.isJsEnum()) {
              // Not a JsEnum receiver, nothing to do.
              return methodCall;
            }

            DeclaredTypeDescriptor implementationType =
                getImplementationType(qualifierTypeDescriptor);
            if (implementationType != null) {
              // The implementation class provides all the supported java.lang.Enum methods.
              return redirect(methodCall, implementationType);
            }

            // The JsEnum is represented by its value.
            return switch (methodDescriptor.getDeclarationDescriptor().getSignature()) {
              // Only JsEnums without custom values support ordinal(), and their values are the
              // ordinals.
              case "ordinal()" -> extractValue(qualifier, PrimitiveTypes.INT);
              case "equals(java.lang.Object)" -> rewriteEquals(methodCall);
              case "compareTo(java.lang.Enum)" -> rewriteCompareTo(methodCall);
              // The remaining methods, i.e. hashCode() and toString(), are dispatched on the value
              // through the java.lang.Object methods they override.
              default -> redirect(methodCall, TypeDescriptors.get().javaLangObject);
            };
          }

          @Override
          public Expression rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!AstUtils.isJsEnumCustomValueField(fieldDescriptor)) {
              return fieldAccess;
            }
            DeclaredTypeDescriptor implementationType =
                getImplementationType(fieldDescriptor.getEnclosingTypeDescriptor());
            if (isJavaLangEnumWorkaround(implementationType)) {
              // The value is a field of the JsEnum itself.
              return fieldAccess;
            } else if (implementationType != null) {
              // The value is a field of the implementation class.
              return fieldAccess.toBuilder()
                  .setTarget(implementationType.getFieldDescriptor(fieldDescriptor.getName()))
                  .build();
            }
            // The value is the JsEnum itself.
            return extractValue(fieldAccess.getQualifier(), fieldAccess.getTypeDescriptor());
          }
        });
  }

  /**
   * Redirects a call to a {@code java.lang.Enum} member to the corresponding member in {@code
   * targetTypeDescriptor}.
   */
  private static MethodCall redirect(
      MethodCall methodCall, DeclaredTypeDescriptor targetTypeDescriptor) {
    MethodDescriptor declarationMethodDescriptor =
        methodCall.getTarget().getDeclarationDescriptor();
    // Methods like `Enum.compareTo(E)` narrow a supertype method's parameter to `Enum`; resolve the
    // signature from the overridden supertype method, as seen from `targetTypeDescriptor`.
    MethodDescriptor prototype =
        declarationMethodDescriptor.getJavaOverriddenMethodDescriptors().stream()
            .map(MethodDescriptor::getDeclarationDescriptor)
            .collect(toOptional())
            .orElse(declarationMethodDescriptor)
            .specializeTypeVariables(targetTypeDescriptor.getParameterization());
    MethodDescriptor redirectedMethodDescriptor =
        targetTypeDescriptor.getMethodDescriptor(
            prototype.getName(),
            prototype.getParameterTypeDescriptors().toArray(TypeDescriptor[]::new));
    if (redirectedMethodDescriptor == null) {
      throw new InternalCompilerError("Unexpected Enum method: %s.", methodCall.getTarget());
    }
    return methodCall.toBuilder().setTarget(redirectedMethodDescriptor).build();
  }

  /**
   * Returns true if {@code implementationType} is {@code java.lang.Enum}, which unlike the other
   * implementation classes does not hold the custom value of the JsEnum.
   */
  // TODO(goktug): Remove once non-native JsEnums in Wasm no longer extend java.lang.Enum.
  private static boolean isJavaLangEnumWorkaround(
      @Nullable DeclaredTypeDescriptor implementationType) {
    return implementationType != null && TypeDescriptors.isJavaLangEnum(implementationType);
  }
}
