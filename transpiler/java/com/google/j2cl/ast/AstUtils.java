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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.common.Cloneable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Utility functions to manipulate J2CL AST. */
public class AstUtils {
  public static final String OVERLAY_IMPLEMENTATION_CLASS_SUFFIX = "$$Overlay";
  public static final String CAPTURES_PREFIX = "$c_";
  public static final String ENCLOSING_INSTANCE_NAME = "$outer_this";
  public static final String TYPE_VARIABLE_IN_METHOD_PREFIX = "M_";
  public static final String TYPE_VARIABLE_IN_TYPE_PREFIX = "C_";
  public static final FieldDescriptor ARRAY_LENGTH_FIELD_DESCRIPTION =
      FieldDescriptor.newBuilder()
          .setEnclosingClassTypeDescriptor(TypeDescriptors.get().primitiveVoid)
          .setFieldName("length")
          .setTypeDescriptor(TypeDescriptors.get().primitiveInt)
          .setIsStatic(false)
          .setJsInfo(JsInfo.RAW_FIELD)
          .build();

  /** Return the String with first letter capitalized. */
  public static String toProperCase(String string) {
    if (string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1, string.length());
  }

  /** Create "$init" MethodDescriptor. */
  public static MethodDescriptor createInitMethodDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setVisibility(Visibility.PRIVATE)
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setName(MethodDescriptor.INIT_METHOD_NAME)
        .build();
  }

  /** Create "Equality.$same()" MethodDescriptor. */
  public static MethodDescriptor createUtilSameMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setIsStatic(true)
        .setJsInfo(JsInfo.RAW)
        .setEnclosingClassTypeDescriptor(BootstrapType.NATIVE_EQUALITY.getDescriptor())
        .setName(MethodDescriptor.SAME_METHOD_NAME)
        .setReturnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
        .setParameterTypeDescriptors(
            Lists.newArrayList(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject))
        .build();
  }

  /** Create default constructor MethodDescriptor. */
  public static MethodDescriptor createDefaultConstructorDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor,
      Visibility visibility,
      TypeDescriptor... parameterTypeDescriptors) {
    JsInfo jsInfo =
        enclosingClassTypeDescriptor.isJsType() && visibility.isPublic()
            ? JsInfo.create(JsMemberType.CONSTRUCTOR, null, null, false)
            : JsInfo.NONE;
    return MethodDescriptor.newBuilder()
        .setVisibility(visibility)
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setIsConstructor(true)
        .setParameterTypeDescriptors(parameterTypeDescriptors)
        .setJsInfo(jsInfo)
        .build();
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor, or
   * returns null if it does not have one.
   */
  public static ExpressionStatement getConstructorInvocationStatement(Method method) {
    checkArgument(method.isConstructor());
    for (Statement statement : method.getBody().getStatements()) {
      if (!(statement instanceof ExpressionStatement)) {
        continue;
      }
      Expression expression = ((ExpressionStatement) statement).getExpression();
      if (!(expression instanceof MethodCall)) {
        continue;
      }
      MethodCall methodCall = (MethodCall) expression;
      if (methodCall.getTarget().isConstructor()) {
        return (ExpressionStatement) statement;
      }
    }
    return null;
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor, or
   * returns null if it does not have one.
   */
  public static MethodCall getConstructorInvocation(Method method) {
    ExpressionStatement statement = getConstructorInvocationStatement(method);
    if (statement == null) {
      return null;
    }
    checkArgument(statement.getExpression() instanceof MethodCall);
    MethodCall methodCall = (MethodCall) statement.getExpression();
    checkArgument(methodCall.getTarget().isConstructor());
    return methodCall;
  }

  /** Returns whether the specified constructor has a this() call. */
  public static boolean hasThisCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /** Returns whether the specified constructor has a super() call. */
  public static boolean hasSuperCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && !constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /** Returns whether the specified constructor has a this() or a super() call. */
  public static boolean hasConstructorInvocation(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null;
  }

  /** Returns whether other is a subtype of one. */
  public static boolean isSubType(TypeDescriptor one, TypeDescriptor other) {
    return one != null
        && (one.equalsIgnoreNullability(other) || isSubType(one.getSuperTypeDescriptor(), other));
  }

  /**
   * The following is the cast table between primitive types. The cell marked as 'X' indicates that
   * no cast is needed.
   *
   * <p>For other cases, cast from A to B is translated to method call $castAToB.
   *
   * <p>The general pattern is that you need casts that shrink, all casts involving 'long' (because
   * it has a custom boxed implementation) and the byte->char and char->short casts because char is
   * unsigned.
   *
   * <p><code>
   * from\to       byte |  char | short | int   | long | float | double|
   * -------------------------------------------------------------------
   * byte        |  X   |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * char        |      |   X   |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * short       |      |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * int         |      |       |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * long        |      |       |       |       |   X  |       |       |
   * -------------------------------------------------------------------
   * float       |      |       |       |       |      |   X   |   X   |
   * -------------------------------------------------------------------
   * double      |      |       |       |       |      |   X   |   X   |
   * </code>
   */
  public static boolean canRemoveCast(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    if (fromTypeDescriptor.equalsIgnoreNullability(toTypeDescriptor)) {
      return true;
    }
    if (fromTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)
        || toTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveLong)) {
      return false;
    }
    return toTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveFloat)
        || toTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveDouble)
        || (toTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveInt)
            && (fromTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveByte)
                || fromTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveChar)
                || fromTypeDescriptor.equalsIgnoreNullability(
                    TypeDescriptors.get().primitiveShort)))
        || (toTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveShort)
            && fromTypeDescriptor.equalsIgnoreNullability(TypeDescriptors.get().primitiveByte));
  }

  /** Returns the added field descriptor corresponding to the captured variable. */
  public static FieldDescriptor getFieldDescriptorForCapture(
      TypeDescriptor enclosingClassTypeDescriptor, Variable capturedVariable) {
    return FieldDescriptor.newBuilder()
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setFieldName(CAPTURES_PREFIX + capturedVariable.getName())
        .setTypeDescriptor(capturedVariable.getTypeDescriptor())
        .setIsStatic(false)
        .setJsInfo(JsInfo.RAW_FIELD)
        .build();
  }

  /** Returns the added field corresponding to the enclosing instance. */
  public static FieldDescriptor getFieldDescriptorForEnclosingInstance(
      TypeDescriptor enclosingClassDescriptor, TypeDescriptor fieldTypeDescriptor) {
    return FieldDescriptor.newBuilder()
        .setEnclosingClassTypeDescriptor(enclosingClassDescriptor)
        .setFieldName(ENCLOSING_INSTANCE_NAME)
        .setTypeDescriptor(fieldTypeDescriptor)
        .build();
  }

  /** Returns the added outer parameter in constructor corresponding to the added field. */
  public static Variable createOuterParamByField(Field field) {
    return Variable.newBuilder()
        .setName(field.getDescriptor().getName())
        .setTypeDescriptor(field.getDescriptor().getTypeDescriptor())
        .setIsParameter(true)
        .setIsFinal(true)
        .build();
  }

  /**
   * Creates static forwarding method that has the same signature of {@code targetMethodDescriptor}
   * in type {@code fromTypeDescriptor}, and delegates to {@code targetMethodDescriptor}, e.g.
   *
   * <p>fromTypeDescriptor.method (args) { return Target.prototype.method.call(this,args); } }
   */
  public static Method createStaticForwardingMethod(
      MethodDescriptor targetMethodDescriptor,
      TypeDescriptor fromTypeDescriptor,
      String jsDocDescription) {
    return createForwardingMethod(
        null,
        MethodDescriptor.Builder.from(targetMethodDescriptor)
            .setEnclosingClassTypeDescriptor(fromTypeDescriptor)
            .build(),
        targetMethodDescriptor,
        jsDocDescription,
        true,
        true);
  }

  /**
   * Creates forwarding method {@code fromMethodDescriptor} that delegates to {@code
   * toMethodDescriptor}, e.g.
   *
   * <p>fromMethodDescriptor (args) { return this.toMethodDescriptor(args); }
   */
  public static Method createForwardingMethod(
      Expression qualifier,
      MethodDescriptor fromMethodDescriptor,
      MethodDescriptor toMethodDescriptor,
      String jsDocDescription,
      boolean isOverride) {
    return createForwardingMethod(
        qualifier, fromMethodDescriptor, toMethodDescriptor, jsDocDescription, false, isOverride);
  }

  private static Method createForwardingMethod(
      Expression qualifier,
      MethodDescriptor fromMethodDescriptor,
      MethodDescriptor toMethodDescriptor,
      String jsDocDescription,
      boolean isStaticDispatch,
      boolean isOverride) {
    checkArgument(!fromMethodDescriptor.getEnclosingClassTypeDescriptor().isInterface());
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    List<TypeDescriptor> parameterTypes = fromMethodDescriptor.getParameterTypeDescriptors();
    for (int i = 0; i < parameterTypes.size(); i++) {
      Variable parameter =
          Variable.newBuilder()
              .setName("arg" + i)
              .setTypeDescriptor(parameterTypes.get(i))
              .setIsParameter(true)
              .build();
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }

    // TODO: Casts are probably needed on arguments if the types differ between the
    // targetMethodDescriptor and its declarationMethodDescriptor.
    Expression forwardingMethodCall =
        MethodCall.Builder.from(toMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(arguments)
            .setIsStaticDispatch(isStaticDispatch)
            .build();

    Statement statement =
        fromMethodDescriptor
                .getReturnTypeDescriptor()
                .equalsIgnoreNullability(TypeDescriptors.get().primitiveVoid)
            ? forwardingMethodCall.makeStatement()
            : new ReturnStatement(
                forwardingMethodCall, fromMethodDescriptor.getReturnTypeDescriptor());
    return Method.newBuilder()
        .setMethodDescriptor(fromMethodDescriptor)
        .setParameters(parameters)
        .addStatements(statement)
        .setIsOverride(isOverride)
        .setIsSynthetic(true)
        .setJsDocDescription(jsDocDescription)
        .build();
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method in
   * {@code enclosingClassTypeDescriptor} with the {@code instanceTypeDescriptor} as the first
   * parameter type.
   *
   * <p>instance.instanceMethod(a, b) => staticMethod(instance, a, b)
   */
  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall,
      TypeDescriptor targetTypeDescriptor,
      TypeDescriptor sourceTypeDescriptor) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    checkArgument(!targetMethodDescriptor.isConstructor());
    checkArgument(!targetMethodDescriptor.isStatic());

    Iterable<TypeDescriptor> parameterTypes =
        Iterables.concat(
            Collections.singletonList(sourceTypeDescriptor), // add the first parameter type.
            targetMethodDescriptor.getParameterTypeDescriptors());
    Iterable<TypeDescriptor> methodDeclarationParameterTypes =
        Iterables.concat(
            Collections.singletonList(sourceTypeDescriptor), // add the first parameter type.
            targetMethodDescriptor.getDeclarationMethodDescriptor().getParameterTypeDescriptors());

    MethodDescriptor declarationMethodDescriptor =
        MethodDescriptor.Builder.from(targetMethodDescriptor.getDeclarationMethodDescriptor())
            .setEnclosingClassTypeDescriptor(targetTypeDescriptor)
            .setParameterTypeDescriptors(methodDeclarationParameterTypes)
            .setIsStatic(true)
            .setJsInfo(JsInfo.NONE)
            .build();

    MethodDescriptor methodDescriptor =
        MethodDescriptor.Builder.from(targetMethodDescriptor)
            .setDeclarationMethodDescriptor(declarationMethodDescriptor)
            .setEnclosingClassTypeDescriptor(targetTypeDescriptor)
            .setParameterTypeDescriptors(parameterTypes)
            .setIsStatic(true)
            .setJsInfo(JsInfo.NONE)
            .build();

    List<Expression> arguments = methodCall.getArguments();
    // Turn the instance into now a first parameter to the devirtualized method.
    Expression instance = checkNotNull(methodCall.getQualifier());
    arguments.add(0, instance);
    // Call the method like Objects.foo(instance, ...)
    return MethodCall.Builder.from(methodDescriptor).setArguments(arguments).build();
  }

  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall, TypeDescriptor targetTypeDescriptor) {
    return createDevirtualizedMethodCall(
        methodCall, targetTypeDescriptor, methodCall.getTarget().getEnclosingClassTypeDescriptor());
  }

  /**
   * Boxes {@code expression} using the valueOf() method of the corresponding boxed type. e.g.
   * expression => Integer.valueOf(expression).
   */
  public static Expression box(Expression expression) {
    TypeDescriptor primitiveType = expression.getTypeDescriptor();
    checkArgument(TypeDescriptors.isNonVoidPrimitiveType(primitiveType));
    checkArgument(!TypeDescriptors.isPrimitiveBooleanOrDouble(primitiveType));
    TypeDescriptor boxType = TypeDescriptors.getBoxTypeFromPrimitiveType(primitiveType);

    MethodDescriptor valueOfMethodDescriptor =
        boxType.getMethodDescriptorByName(MethodDescriptor.VALUE_OF_METHOD_NAME, primitiveType);
    return MethodCall.Builder.from(valueOfMethodDescriptor).setArguments(expression).build();
  }

  /**
   * Unboxes {expression} using the ***Value() method of the corresponding boxed type. e.g
   * expression => expression.intValue().
   */
  public static Expression unbox(Expression expression) {
    TypeDescriptor boxType = expression.getTypeDescriptor();
    checkArgument(TypeDescriptors.isBoxedType(boxType));
    TypeDescriptor primitiveType = TypeDescriptors.getPrimitiveTypeFromBoxType(boxType);

    MethodDescriptor valueMethodDescriptor =
        boxType.getMethodDescriptorByName(
            primitiveType.getSimpleName() + MethodDescriptor.VALUE_METHOD_SUFFIX);

    // We want "(a ? b : c).intValue()", not "a ? b : c.intValue()".
    expression =
        isValidMethodCallQualifier(expression) ? expression : new MultiExpression(expression);

    MethodCall methodCall =
        MethodCall.Builder.from(valueMethodDescriptor).setQualifier(expression).build();
    if (TypeDescriptors.isBoxedBooleanOrDouble(boxType)) {
      methodCall = createDevirtualizedMethodCall(methodCall, boxType);
    }
    return methodCall;
  }

  /** Returns whether the given expression is a syntactically valid qualifier for a MethodCall. */
  private static boolean isValidMethodCallQualifier(Expression expression) {
    return !(expression instanceof ConditionalExpression
        || expression instanceof BinaryExpression
        || expression instanceof PrefixExpression
        || expression instanceof PostfixExpression);
  }

  public static boolean isDelegatedConstructorCall(
      MethodCall methodCall, TypeDescriptor targetTypeDescriptor) {
    if (methodCall == null || !methodCall.getTarget().isConstructor()) {
      return false;
    }
    return methodCall
        .getTarget()
        .getEnclosingClassTypeDescriptor()
        .getRawTypeDescriptor()
        .equals(targetTypeDescriptor.getRawTypeDescriptor());
  }

  /**
   * See JLS 5.2.
   *
   * <p>Would normally also verify that the right operand type is being changed, but we're leaving
   * that check up to our conversion implementation(s)
   */
  public static boolean matchesAssignmentContext(BinaryOperator binaryOperator) {
    return binaryOperator.hasSideEffect();
  }

  /** See JLS 5.4. */
  public static boolean matchesStringContext(BinaryExpression binaryExpression) {
    BinaryOperator operator = binaryExpression.getOperator();
    return (operator == BinaryOperator.PLUS_ASSIGN || operator == BinaryOperator.PLUS)
        && binaryExpression
            .getTypeDescriptor()
            .equalsIgnoreNullability(TypeDescriptors.get().javaLangString);
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(PrefixExpression prefixExpression) {
    PrefixOperator operator = prefixExpression.getOperator();
    return TypeDescriptors.isBoxedOrPrimitiveType(prefixExpression.getTypeDescriptor())
        && (operator == PrefixOperator.PLUS
            || operator == PrefixOperator.MINUS
            || operator == PrefixOperator.COMPLEMENT);
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(TypeDescriptor returnTypeDescriptor) {
    return TypeDescriptors.isBoxedOrPrimitiveType(returnTypeDescriptor);
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(BinaryExpression binaryExpression) {
    return binaryExpression.getOperator().isShiftOperator();
  }

  /** See JLS 5.6.2. */
  public static boolean matchesBinaryNumericPromotionContext(BinaryExpression binaryExpression) {
    // Both operands must be boxed or primitive.
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    BinaryOperator operator = binaryExpression.getOperator();

    return matchesBinaryNumericPromotionContext(leftOperand, operator, rightOperand);
  }

  /** See JLS 5.6.2. */
  public static boolean matchesBinaryNumericPromotionContext(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    if (!TypeDescriptors.isBoxedOrPrimitiveType(leftOperand.getTypeDescriptor())
        || !TypeDescriptors.isBoxedOrPrimitiveType(rightOperand.getTypeDescriptor())) {
      return false;
    }

    boolean leftIsPrimitive = leftOperand.getTypeDescriptor().isPrimitive();
    boolean rightIsPrimitive = rightOperand.getTypeDescriptor().isPrimitive();

    switch (operator) {
      case TIMES:
      case DIVIDE:
      case REMAINDER:
      case PLUS:
      case MINUS:
      case LESS:
      case GREATER:
      case LESS_EQUALS:
      case GREATER_EQUALS:
      case BIT_XOR:
      case BIT_AND:
      case BIT_OR:
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        return true; // Both numerics and booleans get these operators.
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        return true; // Only booleans get these operators (though not mentioned in the JLS).
      case EQUALS:
      case NOT_EQUALS:
        return leftIsPrimitive || rightIsPrimitive; // Equality is sometimes instance comparison.
      default:
        return false;
    }
  }

  /**
   * See JLS 5.6.2. <code>
   * X and double -> double
   * X and float -> float
   * X and long -> long
   * otherwise -> int
   * </code>
   */
  public static TypeDescriptor chooseWidenedTypeDescriptor(
      TypeDescriptor leftTypeDescriptor, TypeDescriptor rightTypeDescriptor) {
    checkArgument(leftTypeDescriptor.isPrimitive());
    checkArgument(rightTypeDescriptor.isPrimitive());

    // Pick the wider of the two provided type descriptors.
    TypeDescriptor widenedTypeDescriptor =
        TypeDescriptors.getWidth(leftTypeDescriptor) > TypeDescriptors.getWidth(rightTypeDescriptor)
            ? leftTypeDescriptor
            : rightTypeDescriptor;

    // If it's smaller than int then use int.
    if (TypeDescriptors.getWidth(widenedTypeDescriptor)
        < TypeDescriptors.getWidth(TypeDescriptors.get().primitiveInt)) {
      widenedTypeDescriptor = TypeDescriptors.get().primitiveInt;
    }

    return widenedTypeDescriptor;
  }

  public static MethodDescriptor getStringValueOfMethodDescriptor() {
    return TypeDescriptors.get()
        .javaLangString
        .getMethodDescriptorByName(
            MethodDescriptor.VALUE_OF_METHOD_NAME, TypeDescriptors.get().javaLangObject);
  }

  /**
   * Returns true if {@code expression} is a string literal or if it is a BinaryExpression that
   * matches String conversion context and one of its operands is non null String.
   */
  public static boolean isNonNullString(Expression expression) {
    if (expression instanceof StringLiteral) {
      return true;
    }
    if (!(expression instanceof BinaryExpression)) {
      return false;
    }
    BinaryExpression binaryExpression = (BinaryExpression) expression;
    if (!matchesStringContext(binaryExpression)) {
      return false;
    }
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    return isNonNullString(leftOperand) || isNonNullString(rightOperand);
  }

  /**
   * Returns explicit qualifier for member reference (field access or method call).
   *
   * <p>If {@code qualifier} is null, returns EnclosingClassTypeDescriptor as the qualifier for
   * static method/field and 'this' reference as the qualifier for instance method/field.
   */
  static Expression getExplicitQualifier(Expression qualifier, MemberDescriptor memberDescriptor) {
    checkNotNull(memberDescriptor);
    if (qualifier != null) {
      return qualifier;
    }
    TypeDescriptor enclosingClassTypeDescriptor =
        memberDescriptor.getEnclosingClassTypeDescriptor();
    return memberDescriptor.isStatic()
        ? new TypeReference(enclosingClassTypeDescriptor.getRawTypeDescriptor())
        : new ThisReference(enclosingClassTypeDescriptor);
  }

  /** Returns true if the qualifier of the given member reference is 'this' reference. */
  public static boolean hasThisReferenceAsQualifier(MemberReference memberReference) {
    Expression qualifier = memberReference.getQualifier();
    return qualifier instanceof ThisReference
        && qualifier.getTypeDescriptor()
            == memberReference.getTarget().getEnclosingClassTypeDescriptor();
  }

  /** Returns true if the qualifier of the given member reference is a Type reference. */
  public static boolean hasTypeReferenceAsQualifier(MemberReference memberReference) {
    return memberReference.getQualifier() instanceof TypeReference;
  }

  public static Expression joinExpressionsWithBinaryOperator(
      TypeDescriptor outputType, BinaryOperator operator, List<Expression> expressions) {
    if (expressions.isEmpty()) {
      return null;
    }
    if (expressions.size() == 1) {
      return expressions.get(0);
    }
    Expression joinedExpressions =
        joinExpressionsWithBinaryOperator(
            outputType, operator, expressions.subList(1, expressions.size()));
    return BinaryExpression.newBuilder()
        .setTypeDescriptor(outputType)
        .setLeftOperand(expressions.get(0))
        .setOperator(operator)
        .setRightOperand(joinedExpressions)
        .build();
  }

  public static Method createDevirtualizedMethod(Method method) {
    checkArgument(
        !method.getDescriptor().isJsPropertyGetter()
            && !method.getDescriptor().isJsPropertySetter(),
        "JsPropery getter and setters should never be devirtualized " + method);
    checkArgument(method.getDescriptor().isPolymorphic());
    // TODO: remove once init() function is synthesized in AST.
    checkArgument(!method.getDescriptor().isInit(), "Do not devirtualize init().");

    final Variable thisArg =
        Variable.newBuilder()
            .setName("$thisArg")
            .setTypeDescriptor(method.getDescriptor().getEnclosingClassTypeDescriptor())
            .setIsParameter(true)
            .setIsFinal(true)
            .build();

    // Replace all 'this' references in the method with parameter references.
    method.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteThisReference(ThisReference thisReference) {
            return thisArg.getReference();
          }
        });

    // MethodDescriptor for the devirtualized static method.
    MethodDescriptor newMethodDescriptor =
        MethodDescriptors.makeStaticMethodDescriptor(method.getDescriptor());

    // Parameters for the devirtualized static method.
    List<Variable> newParameters = new ArrayList<>();
    newParameters.add(thisArg); // added extra parameter.
    newParameters.addAll(method.getParameters()); // original parameters in the instance method.

    // Add the static method to current type.
    return Method.newBuilder()
        .setMethodDescriptor(newMethodDescriptor)
        .setParameters(newParameters)
        .addStatements(method.getBody().getStatements())
        .build();
  }

  /**
   * Returns the constructor that is being delegated to (the primary constructor) in a JsConstructor
   * class or in a child class of a JsConstructor class. This constructor will be generated as the
   * real ES6 constructor.
   */
  public static Method getPrimaryConstructor(Type type) {
    for (Method method : type.getMethods()) {
      if (method.isPrimaryConstructor()) {
        return method;
      }
    }
    return null;
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   */
  public static boolean areParameterErasureEqual(MethodDescriptor left, MethodDescriptor right) {
    List<TypeDescriptor> leftParameterTypeDescriptors =
        left.getDeclarationMethodDescriptor().getParameterTypeDescriptors();
    List<TypeDescriptor> rightParameterTypeDescriptors =
        right.getDeclarationMethodDescriptor().getParameterTypeDescriptors();

    if (!left.getName().equals(right.getName())
        || leftParameterTypeDescriptors.size() != rightParameterTypeDescriptors.size()) {
      return false;
    }
    for (int i = 0; i < leftParameterTypeDescriptors.size(); i++) {
      if (!leftParameterTypeDescriptors
          .get(i)
          .getRawTypeDescriptor()
          .equalsIgnoreNullability(rightParameterTypeDescriptors.get(i).getRawTypeDescriptor())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Generates the following code:
   *
   * <p>$Util.$makeLambdaFunction( $Util.$getPrototype(Type).m_equal, $instance, Type.$copy);
   */
  public static MethodCall createLambdaInstance(TypeDescriptor lambdaType, Expression instance) {
    checkArgument(lambdaType.isJsFunctionImplementation());

    // Class.prototype.apply
    String applyMethodName =
        ManglingNameUtils.getMangledName(lambdaType.getJsFunctionMethodDescriptor());

    // Util getPrototype
    MethodDescriptor getPrototype =
        MethodDescriptor.newBuilder()
            .setEnclosingClassTypeDescriptor(
                TypeDescriptors.BootstrapType.NATIVE_UTIL.getDescriptor())
            .setName("$getPrototype")
            .setIsStatic(true)
            .setJsInfo(JsInfo.RAW)
            .setParameterTypeDescriptors(TypeDescriptors.NATIVE_FUNCTION)
            .setReturnTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .build();

    MethodCall getPrototypeCall =
        MethodCall.Builder.from(getPrototype).setArguments(new TypeReference(lambdaType)).build();

    FieldAccess applyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingClassTypeDescriptor(lambdaType)
                    .setFieldName(applyMethodName)
                    .setTypeDescriptor(TypeDescriptors.NATIVE_FUNCTION)
                    .setJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(getPrototypeCall)
            .build();

    MethodDescriptor makeLambdaCall =
        MethodDescriptor.newBuilder()
            .setEnclosingClassTypeDescriptor(
                TypeDescriptors.BootstrapType.NATIVE_UTIL.getDescriptor())
            .setName("$makeLambdaFunction")
            .setIsStatic(true)
            .setJsInfo(JsInfo.RAW)
            .setParameterTypeDescriptors(
                TypeDescriptors.NATIVE_FUNCTION,
                TypeDescriptors.get().javaLangObject,
                TypeDescriptors.NATIVE_FUNCTION)
            .build();

    FieldAccess copyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingClassTypeDescriptor(lambdaType)
                    .setFieldName("$copy")
                    .setTypeDescriptor(TypeDescriptors.NATIVE_FUNCTION)
                    .setJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(new TypeReference(lambdaType))
            .build();

    return MethodCall.Builder.from(makeLambdaCall)
        .setArguments(applyFunctionFieldAccess, instance, copyFunctionFieldAccess)
        .build();
  }

  private static Expression getInitialValue(Field field) {
    if (field.isCompileTimeConstant()) {
      return field.getInitializer();
    }
    return TypeDescriptors.getDefaultValue(field.getDescriptor().getTypeDescriptor());
  }

  public static List<Statement> generateFieldDeclarations(Type type) {
    List<Statement> fieldInits = new ArrayList<>();
    for (Field field : type.getFields()) {
      if (field.getDescriptor().isStatic()) {
        continue;
      }
      Statement declaration =
          JsDocAnnotatedExpression.newBuilder()
              .setExpression(
                  BinaryExpression.newBuilder()
                      .setTypeDescriptor(TypeDescriptors.get().primitiveVoid)
                      .setLeftOperand(FieldAccess.Builder.from(field).build())
                      .setOperator(BinaryOperator.ASSIGN)
                      .setRightOperand(getInitialValue(field))
                      .build())
              .setAnnotationType(field.getDescriptor().getTypeDescriptor())
              .setIsDeclaration(true)
              .build()
              .makeStatement();
      fieldInits.add(declaration);
    }
    return fieldInits;
  }

  /**
   * Create a call to an array set expression for a binary operator.
   *
   * @param array
   * @param index
   * @param operator
   * @param value
   * @return The method call.
   */
  public static Expression createArraySetExpression(
      Expression array, Expression index, BinaryOperator operator, Expression value) {

    // Get the type of the elements in the array.
    TypeDescriptor elementType = array.getTypeDescriptor().getComponentTypeDescriptor();

    // Get the name of the method that should be called
    String methodName = AstUtils.getArrayAssignmentMethodName(elementType, operator);

    // Create the parameter type descriptor list.
    TypeDescriptor[] methodParams = {
      BootstrapType.ARRAYS.getDescriptor(), // array
      TypeDescriptors.get().primitiveInt, // index
      elementType
    }; // value

    MethodDescriptor arraySetMethodDescriptor =
        createArraySetMethodDescriptor(elementType, methodName, methodParams);
    return MethodCall.Builder.from(arraySetMethodDescriptor)
        .setArguments(array, index, value)
        .build();
  }

  /**
   * Create a call to an array set expression for a postfix operator.
   *
   * @param array
   * @param index
   * @param operator
   * @return The method call.
   */
  public static Node createArraySetPostfixExpression(
      Expression array, Expression index, PostfixOperator operator) {

    // Get the type of the elements in the array.
    TypeDescriptor elementType = array.getTypeDescriptor().getComponentTypeDescriptor();

    // Get the name of the method that should be called
    String methodName = AstUtils.getArrayPostfixAssignmentMethodName(elementType, operator);

    // Create the parameter type descriptor list.
    TypeDescriptor[] methodParams = {
      BootstrapType.ARRAYS.getDescriptor(), // array
      TypeDescriptors.get().primitiveInt
    }; // index

    MethodDescriptor arraySetMethodDescriptor =
        createArraySetMethodDescriptor(elementType, methodName, methodParams);
    return MethodCall.Builder.from(arraySetMethodDescriptor).setArguments(array, index).build();
  }

  /**
   * Create a method descriptor for a call to an array set expression for a binary operator.
   *
   * @param elementType
   * @param methodName
   * @param methodParams
   * @return The method descriptor.
   */
  private static MethodDescriptor createArraySetMethodDescriptor(
      TypeDescriptor elementType, String methodName, TypeDescriptor... methodParams) {

    // Get the descriptor for the class on which the array set method should be called.
    // (LongUtils if the array is a long array, else Arrays)
    TypeDescriptor enclosingClassType;
    if (elementType.equals(TypeDescriptors.get().primitiveLong)) {
      enclosingClassType = BootstrapType.LONG_UTILS.getDescriptor();
    } else {
      enclosingClassType = BootstrapType.ARRAYS.getDescriptor();
    }

    // Create and return the method descriptor.
    return MethodDescriptor.newBuilder()
        .setJsInfo(JsInfo.RAW)
        .setIsStatic(true)
        .setEnclosingClassTypeDescriptor(enclosingClassType)
        .setName(methodName)
        .setParameterTypeDescriptors(methodParams)
        .setReturnTypeDescriptor(elementType)
        .build();
  }

  /**
   * Get the name of the method that should be called for an array set expression.
   *
   * @param elementType
   * @param operator
   * @return The string method name.
   */
  private static String getArrayAssignmentMethodName(
      TypeDescriptor elementType, BinaryOperator operator) {
    String methodName;
    switch (operator) {
      case ASSIGN:
        methodName = "$set";
        break;
      case PLUS_ASSIGN:
        methodName = "$addSet";
        break;
      case MINUS_ASSIGN:
        methodName = "$subSet";
        break;
      case TIMES_ASSIGN:
        methodName = "$mulSet";
        break;
      case DIVIDE_ASSIGN:
        methodName = "$divSet";
        break;
      case BIT_AND_ASSIGN:
        methodName = "$andSet";
        break;
      case BIT_OR_ASSIGN:
        methodName = "$orSet";
        break;
      case BIT_XOR_ASSIGN:
        methodName = "$xorSet";
        break;
      case REMAINDER_ASSIGN:
        methodName = "$modSet";
        break;
      case LEFT_SHIFT_ASSIGN:
        methodName = "$lshiftSet";
        break;
      case RIGHT_SHIFT_SIGNED_ASSIGN:
        methodName = "$rshiftSet";
        break;
      case RIGHT_SHIFT_UNSIGNED_ASSIGN:
        methodName = "$rshiftUSet";
        break;
      default:
        checkState(
            false, "Requested the array assignment function name for a non-assignment operator.");
        return null;
    }
    // Long arrays use the LongUtils set methods, which are suffixed by "Array".
    if (elementType.equals(TypeDescriptors.get().primitiveLong)) {
      methodName = methodName.concat("Array");
    }
    return methodName;
  }

  /**
   * Get the name of the method that should be called for an array set expression.
   *
   * @param elementType
   * @param operator
   * @return The string method name.
   */
  private static String getArrayPostfixAssignmentMethodName(
      TypeDescriptor elementType, PostfixOperator operator) {
    String methodName;
    switch (operator) {
      case INCREMENT:
        methodName = "$postfixIncrement";
        break;
      case DECREMENT:
        methodName = "$postfixDecrement";
        break;
      default:
        checkState(
            false,
            "Requested the postfix array assignment function name for a non-postfix operator.");
        return null;
    }
    // Long arrays use the LongUtils set methods, which are suffixed by "Array".
    if (elementType.equals(TypeDescriptors.get().primitiveLong)) {
      methodName = methodName.concat("Array");
    }
    return methodName;
  }

  /**
   * Get the binary compound assignment operator corresponding to the given operator. + or ++ will
   * return +=. - or -- will return -=.
   *
   * @param operator
   * @return The corresponding compound assignment binary operator.
   */
  public static BinaryOperator getCorrespondingCompoundAssignmentOperator(Operator operator) {
    switch (operator.getUnderlyingBinaryOperator()) {
      case PLUS:
        return BinaryOperator.PLUS_ASSIGN;
      case MINUS:
        return BinaryOperator.MINUS_ASSIGN;
      default:
        return operator.getUnderlyingBinaryOperator();
    }
  }

  /** Returns all type variables that appear in the subtree. */
  public static Set<TypeDescriptor> getAllTypeVariables(Node node) {
    final Set<TypeDescriptor> lambdaTypeParameterTypeDescriptors = new LinkedHashSet<>();
    node.accept(
        new AbstractVisitor() {
          @Override
          public boolean enterTypeDescriptor(TypeDescriptor typeDescriptor) {
            lambdaTypeParameterTypeDescriptors.addAll(typeDescriptor.getAllTypeVariables());
            return false;
          }

          @Override
          public boolean enterFieldDescriptor(FieldDescriptor fieldDescriptor) {
            lambdaTypeParameterTypeDescriptors.addAll(
                fieldDescriptor.getTypeDescriptor().getAllTypeVariables());
            return false;
          }

          @Override
          public boolean enterMethodDescriptor(MethodDescriptor methodDescriptor) {

            lambdaTypeParameterTypeDescriptors.addAll(
                methodDescriptor.getReturnTypeDescriptor().getAllTypeVariables());
            for (TypeDescriptor parameterTypeDescriptor :
                methodDescriptor.getParameterTypeDescriptors()) {
              lambdaTypeParameterTypeDescriptors.addAll(
                  parameterTypeDescriptor.getAllTypeVariables());
            }
            return false;
          }
        });
    return lambdaTypeParameterTypeDescriptors;
  }

  /** Returns an iterable containing only the fields in {@code members}. */
  public static FluentIterable<Field> filterFields(Iterable<Member> members) {
    return FluentIterable.from(members)
        .transform(
            member -> {
              if (member instanceof Field) {
                return (Field) member;
              }
              return null;
            })
        .filter(Predicates.notNull());
  }

  /** Returns an iterable containing only the methods in {@code members}. */
  public static FluentIterable<Method> filterMethods(Iterable<Member> members) {
    return FluentIterable.from(members)
        .transform(
            member -> {
              if (member instanceof Method) {
                return (Method) member;
              }
              return null;
            })
        .filter(Predicates.notNull());
  }

  /** Returns an iterable containing only the initializer blocks in {@code members}. */
  public static FluentIterable<InitializerBlock> filterInitializerBlocks(Iterable<Member> members) {
    return FluentIterable.from(members)
        .transform(
            member -> {
              if (member instanceof InitializerBlock) {
                return (InitializerBlock) member;
              }
              return null;
            })
        .filter(Predicates.notNull());
  }

  @SuppressWarnings("unchecked")
  /** Clones a list of expressions */
  public static <T extends Cloneable<?>> List<T> clone(List<T> nodes) {
    return nodes.stream().map(node -> (T) node.clone()).collect(toImmutableList());
  }

  @SuppressWarnings("unchecked")
  /** Clones a cloneable or returns null */
  public static <T extends Cloneable<?>> T clone(T node) {
    return node != null ? (T) node.clone() : null;
  }

  @SuppressWarnings("unchecked")
  /**
   * Replaces references to variables in {@code fromVariables} to reference to variables in {@code
   * toVariables}.
   */
  public static <T extends Node> List<T> replaceVariables(
      List<Variable> fromVariables, List<Variable> toVariable, List<T> nodes) {
    List<T> result = new ArrayList<>();
    for (T node : nodes) {
      result.add(replaceVariables(fromVariables, toVariable, node));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  /**
   * Replaces references to variables in {@code fromVariables} to reference to variables in {@code
   * toVariables}.
   */
  public static <T extends Node> T replaceVariables(
      List<Variable> fromVariables, List<Variable> toVariable, T node) {
    class VariableReplacer extends AbstractRewriter {
      Map<Variable, Variable> toVariableByFromVariable = new HashMap<>();

      public VariableReplacer(List<Variable> fromVariables, List<Variable> toVariables) {
        checkArgument(fromVariables.size() == toVariables.size());
        for (int i = 0; i < fromVariables.size(); i++) {
          toVariableByFromVariable.put(fromVariables.get(i), toVariables.get(i));
        }
      }

      @Override
      public Node rewriteVariable(Variable variable) {
        Variable toVariable = toVariableByFromVariable.get(variable);
        return toVariable == null ? variable : toVariable;
      }

      @Override
      public Node rewriteVariableReference(VariableReference variableReference) {
        Variable toVariable = toVariableByFromVariable.get(variableReference.getTarget());
        if (toVariable != null) {
          return toVariable.getReference();
        }
        return variableReference;
      }
    }

    return (T) node.accept(new VariableReplacer(fromVariables, toVariable));
  }

  /** Get a list of references for {@code variables}. */
  public static List<Expression> getReferences(List<Variable> variables) {
    return variables.stream().map(Variable::getReference).collect(toImmutableList());
  }
}
