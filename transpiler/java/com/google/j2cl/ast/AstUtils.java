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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility functions to manipulate J2CL AST.
 */
public class AstUtils {
  public static final String JS_OVERLAY_METHODS_IMPL_SUFFIX = "Overlay";
  public static final String CAPTURES_PREFIX = "$c_";
  public static final String ENCLOSING_INSTANCE_NAME = "$outer_this";
  public static final String CREATE_PREFIX = "$create_";
  public static final String TYPE_VARIABLE_IN_METHOD_PREFIX = "M_";
  public static final String TYPE_VARIABLE_IN_TYPE_PREFIX = "C_";
  public static final FieldDescriptor ARRAY_LENGTH_FIELD_DESCRIPTION =
      FieldDescriptor.createRaw(
          false, TypeDescriptors.get().primitiveVoid, "length", TypeDescriptors.get().primitiveInt);

  /**
   * Return the String with first letter capitalized.
   */
  public static String toProperCase(String string) {
    if (string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1, string.length());
  }

  /**
   * Create "$init" MethodDescriptor.
   */
  public static MethodDescriptor createInitMethodDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor) {
    return MethodDescriptorBuilder.fromDefault()
        .visibility(Visibility.PRIVATE)
        .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .methodName(MethodDescriptor.INIT_METHOD_NAME)
        .build();
  }

  /**
   * Create "Equality.$same()" MethodDescriptor.
   */
  public static MethodDescriptor createUtilSameMethodDescriptor() {
    return MethodDescriptorBuilder.fromDefault()
        .isStatic(true)
        .jsInfo(JsInfo.RAW)
        .enclosingClassTypeDescriptor(BootstrapType.NATIVE_EQUALITY.getDescriptor())
        .methodName(MethodDescriptor.SAME_METHOD_NAME)
        .returnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
        .parameterTypeDescriptors(
            Lists.newArrayList(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject))
        .build();
  }

  /**
   * Create "Equality.$notSame()" MethodDescriptor.
   */
  public static MethodDescriptor createUtilNotSameMethodDescriptor() {
    return MethodDescriptorBuilder.fromDefault()
        .isStatic(true)
        .jsInfo(JsInfo.RAW)
        .enclosingClassTypeDescriptor(BootstrapType.NATIVE_EQUALITY.getDescriptor())
        .methodName(MethodDescriptor.NOT_SAME_METHOD_NAME)
        .returnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
        .parameterTypeDescriptors(
            Lists.newArrayList(
                TypeDescriptors.get().javaLangObject, TypeDescriptors.get().javaLangObject))
        .build();
  }

  /**
   * Create default constructor MethodDescriptor.
   */
  public static MethodDescriptor createDefaultConstructorDescriptor(
      TypeDescriptor typeDescriptor,
      Visibility visibility,
      TypeDescriptor... parameterTypeDescriptors) {
    JsInfo jsInfo =
        typeDescriptor.isJsType()
            ? JsInfo.create(JsMemberType.CONSTRUCTOR, null, null, false)
            : JsInfo.NONE;
    return MethodDescriptorBuilder.fromDefault()
        .visibility(visibility)
        .enclosingClassTypeDescriptor(typeDescriptor)
        .methodName(typeDescriptor.getClassName())
        .isConstructor(true)
        .parameterTypeDescriptors(Arrays.asList(parameterTypeDescriptors))
        .jsInfo(jsInfo)
        .build();
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor,
   * or returns null if it does not have one.
   */
  public static MethodCall getConstructorInvocation(Method method) {
    checkArgument(method.isConstructor());
    if (method.getBody().getStatements().isEmpty()) {
      return null;
    }
    Statement firstStatement = method.getBody().getStatements().get(0);
    if (!(firstStatement instanceof ExpressionStatement)) {
      return null;
    }
    Expression expression = ((ExpressionStatement) firstStatement).getExpression();
    if (!(expression instanceof MethodCall)) {
      return null;
    }
    MethodCall methodCall = (MethodCall) expression;
    return methodCall.getTarget().isConstructor() ? methodCall : null;
  }

  /**
   * When requested on an inner type, returns the field that references the enclosing instance,
   * otherwise null.
   */
  public static Field getEnclosingInstanceField(JavaType type) {
    for (Field field : type.getFields()) {
      if (field.getDescriptor().getFieldName().equals(ENCLOSING_INSTANCE_NAME)) {
        return field;
      }
    }
    return null;
  }

  /**
   * Returns whether the specified constructor has a this() call.
   */
  public static boolean hasThisCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /**
   * Returns whether the specified constructor has a super() call.
   */
  public static boolean hasSuperCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && !constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /**
   * Returns whether the specified constructor has a this() or a super() call.
   */
  public static boolean hasConstructorInvocation(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null;
  }

  /**
   * Returns whether other is a subtype of one.
   */
  public static boolean isSubType(TypeDescriptor one, TypeDescriptor other) {
    return one == other || one != null && isSubType(one.getSuperTypeDescriptor(), other);
  }

  /**
   * The following is the cast table between primitive types. The cell marked as 'X'
   * indicates that no cast is needed.
   * <p>
   * For other cases, cast from A to B is translated to method call $castAToB.
   * <p>
   * The general pattern is that you need casts that shrink, all casts involving
   * 'long' (because it has a custom boxed implementation) and the byte->char and
   * char->short casts because char is unsigned.
   * <p>
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
   */
  public static boolean canRemoveCast(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    if (fromTypeDescriptor == toTypeDescriptor) {
      return true;
    }
    if (fromTypeDescriptor == TypeDescriptors.get().primitiveLong
        || toTypeDescriptor == TypeDescriptors.get().primitiveLong) {
      return false;
    }
    return toTypeDescriptor == TypeDescriptors.get().primitiveFloat
        || toTypeDescriptor == TypeDescriptors.get().primitiveDouble
        || (toTypeDescriptor == TypeDescriptors.get().primitiveInt
            && (fromTypeDescriptor == TypeDescriptors.get().primitiveByte
                || fromTypeDescriptor == TypeDescriptors.get().primitiveChar
                || fromTypeDescriptor == TypeDescriptors.get().primitiveShort))
        || (toTypeDescriptor == TypeDescriptors.get().primitiveShort
            && fromTypeDescriptor == TypeDescriptors.get().primitiveByte);
  }

  /**
   * Returns the added field descriptor corresponding to the captured variable.
   */
  public static FieldDescriptor getFieldDescriptorForCapture(
      TypeDescriptor enclosingClassTypeDescriptor, Variable capturedVariable) {
    return FieldDescriptor.createRaw(
        false,
        enclosingClassTypeDescriptor,
        CAPTURES_PREFIX + capturedVariable.getName(),
        capturedVariable.getTypeDescriptor());
  }

  /**
   * Returns the added field corresponding to the enclosing instance.
   */
  public static FieldDescriptor getFieldDescriptorForEnclosingInstance(
      TypeDescriptor enclosingClassDescriptor, TypeDescriptor fieldTypeDescriptor) {
    return FieldDescriptorBuilder.fromDefault(
            enclosingClassDescriptor, ENCLOSING_INSTANCE_NAME, fieldTypeDescriptor)
        .build();
  }

  /**
   * Returns the added outer parameter in constructor corresponding to the added field.
   */
  public static Variable createOuterParamByField(Field field) {
    return new Variable(
        field.getDescriptor().getFieldName(),
        field.getDescriptor().getTypeDescriptor(),
        true,
        true);
  }

  /**
   * Returns the MethodDescriptor for the wrapper function in outer class that creates its
   * inner class by calling the corresponding inner class constructor.
   */
  public static MethodDescriptor createMethodDescriptorForInnerClassCreation(
      final TypeDescriptor outerclassTypeDescriptor,
      MethodDescriptor innerclassConstructorDescriptor) {
    String methodName = CREATE_PREFIX + innerclassConstructorDescriptor.getMethodName();
    TypeDescriptor returnTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    // if the inner class is a generic type, add its type parameters to the wrapper method.
    List<TypeDescriptor> typeParameterDescriptors = new ArrayList<>();
    TypeDescriptor innerclassTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    if (innerclassTypeDescriptor.isParameterizedType()) {
      typeParameterDescriptors.addAll(
          Lists.newArrayList(
              Iterables.filter(
                  // filters out the type parameters declared in the outer class.
                  innerclassTypeDescriptor.getTypeArgumentDescriptors(),
                  new Predicate<TypeDescriptor>() {
                    @Override
                    public boolean apply(TypeDescriptor typeParameter) {
                      return !outerclassTypeDescriptor
                          .getTypeArgumentDescriptors()
                          .contains(typeParameter);
                    }
                  })));
    }
    return MethodDescriptorBuilder.fromDefault()
        .visibility(innerclassConstructorDescriptor.getVisibility())
        .enclosingClassTypeDescriptor(outerclassTypeDescriptor)
        .methodName(methodName)
        .returnTypeDescriptor(returnTypeDescriptor)
        .parameterTypeDescriptors(innerclassConstructorDescriptor.getParameterTypeDescriptors())
        .typeParameterDescriptors(typeParameterDescriptors)
        .build();
  }

  /**
   * Returns the Method for the wrapper function in outer class that creates its inner class
   * by calling the corresponding inner class constructor.
   */
  public static Method createMethodForInnerClassCreation(
      final TypeDescriptor outerclassTypeDescriptor, Method innerclassConstructor) {
    MethodDescriptor innerclassConstructorDescriptor = innerclassConstructor.getDescriptor();
    MethodDescriptor methodDescriptor =
        createMethodDescriptorForInnerClassCreation(
            outerclassTypeDescriptor, innerclassConstructorDescriptor);

    // create arguments.
    List<Expression> arguments = new ArrayList<>();
    for (Variable parameter : innerclassConstructor.getParameters()) {
      arguments.add(new VariableReference(parameter));
    }
    // adds 'this' as the last argument.
    arguments.add(new ThisReference(outerclassTypeDescriptor));

    // adds 'this' as the last parameter.
    MethodDescriptor newInnerclassConstructorDescriptor =
        MethodDescriptors.createModifiedCopy(
            innerclassConstructorDescriptor, Lists.newArrayList(outerclassTypeDescriptor));

    Expression newInnerClass = new NewInstance(null, newInnerclassConstructorDescriptor, arguments);
    List<Statement> statements = new ArrayList<>();
    statements.add(
        new ReturnStatement(
            newInnerClass, innerclassConstructorDescriptor.getReturnTypeDescriptor()));
    // return new InnerClass();
    Block body = new Block(statements);

    return new Method(methodDescriptor, innerclassConstructor.getParameters(), body);
  }

  /**
   * Creates forwarding method that has the same signature of {@code fromMethodDescriptor} in
   * type {@code enclosingClassTypeDescriptor}, and delegates to {@code toMethodDescriptor}, e.g.
   *
   * enclosingClassTypeDescriptor.toMethodDescriptor (args) {
   *    return this.toMethodDescriptor(args);
   * }
   */
  public static Method createForwardingMethod(
      MethodDescriptor fromMethodDescriptor,
      MethodDescriptor toMethodDescriptor,
      TypeDescriptor enclosingClassTypeDescriptor,
      String jsDocDescription) {
    MethodDescriptor forwardingMethodDescriptor =
        MethodDescriptorBuilder.from(fromMethodDescriptor)
            .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
            .build();
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    for (int i = 0; i < fromMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          new Variable(
              "arg" + i, fromMethodDescriptor.getParameterTypeDescriptors().get(i), false, true);
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }
    Expression forwardingMethodCall =
        MethodCall.createRegularMethodCall(null, toMethodDescriptor, arguments);
    Statement statement =
        fromMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.get().primitiveVoid
            ? new ExpressionStatement(forwardingMethodCall)
            : new ReturnStatement(
                forwardingMethodCall, fromMethodDescriptor.getReturnTypeDescriptor());
    return new Method(
        forwardingMethodDescriptor,
        parameters,
        new Block(Arrays.asList(statement)),
        false,
        true,
        false,
        jsDocDescription);
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method
   * in {@code enclosingClassTypeDescriptor} with the {@code instanceTypeDescriptor} as the first
   * parameter type.
   *
   * <p>instance.instanceMethod(a, b) => staticMethod(instance, a, b)
   */
  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall, TypeDescriptor targetTypeDescriptor,
      TypeDescriptor sourceTypeDescriptor) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    checkArgument(!targetMethodDescriptor.isConstructor());
    checkArgument(!targetMethodDescriptor.isStatic());

    MethodDescriptor methodDescriptor =
        MethodDescriptorBuilder.from(targetMethodDescriptor)
            .enclosingClassTypeDescriptor(targetTypeDescriptor)
            .parameterTypeDescriptors(
                Iterables.concat(
                    Arrays.asList(sourceTypeDescriptor), // add the first parameter type.
                    targetMethodDescriptor.getParameterTypeDescriptors()))
            .isStatic(true)
            .jsInfo(JsInfo.NONE)
            .build();

    List<Expression> arguments = methodCall.getArguments();
    // Turn the instance into now a first parameter to the devirtualized method.
    Expression instance = checkNotNull(methodCall.getQualifier());
    arguments.add(0, instance);
    // Call the method like Objects.foo(instance, ...)
    return MethodCall.createRegularMethodCall(null, methodDescriptor, arguments);
  }

  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall, TypeDescriptor targetTypeDescriptor) {
    return createDevirtualizedMethodCall(
        methodCall, targetTypeDescriptor, methodCall.getTarget().getEnclosingClassTypeDescriptor());
  }

  /**
   * Boxes {@code expression} using the valueOf() method of the corresponding boxed type.
   * e.g. expression => Integer.valueOf(expression).
   */
  public static Expression box(Expression expression) {
    TypeDescriptor primitiveType = expression.getTypeDescriptor();
    checkArgument(TypeDescriptors.isNonVoidPrimitiveType(primitiveType));
    checkArgument(!TypeDescriptors.isPrimitiveBooleanOrDouble(primitiveType));
    TypeDescriptor boxType = TypeDescriptors.getBoxTypeFromPrimitiveType(primitiveType);

    MethodDescriptor valueOfMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .isStatic(true)
            .enclosingClassTypeDescriptor(boxType)
            .methodName(MethodDescriptor.VALUE_OF_METHOD_NAME)
            .returnTypeDescriptor(boxType)
            .parameterTypeDescriptors(Arrays.asList(primitiveType))
            .build();
    return MethodCall.createRegularMethodCall(null, valueOfMethodDescriptor, expression);
  }

  /**
   * Unboxes {expression} using the ***Value() method of the corresponding boxed type.
   * e.g expression => expression.intValue().
   */
  public static Expression unbox(Expression expression) {
    TypeDescriptor boxType = expression.getTypeDescriptor();
    checkArgument(TypeDescriptors.isBoxedType(boxType));
    TypeDescriptor primitiveType = TypeDescriptors.getPrimitiveTypeFromBoxType(boxType);

    MethodDescriptor valueMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .enclosingClassTypeDescriptor(boxType)
            .methodName(primitiveType.getSimpleName() + MethodDescriptor.VALUE_METHOD_SUFFIX)
            .returnTypeDescriptor(primitiveType)
            .build();

    // We want "(a ? b : c).intValue()", not "a ? b : c.intValue()".
    expression =
        isValidMethodCallQualifier(expression) ? expression : new MultiExpression(expression);

    MethodCall methodCall = MethodCall.createRegularMethodCall(expression, valueMethodDescriptor);
    if (TypeDescriptors.isBoxedBooleanOrDouble(boxType)) {
      methodCall = createDevirtualizedMethodCall(methodCall, boxType);
    }
    return methodCall;
  }

  /**
   * Returns whether the given expression is a syntactically valid qualifier for a MethodCall.
   */
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

  /**
   * See JLS 5.4.
   */
  public static boolean matchesStringContext(BinaryExpression binaryExpression) {
    BinaryOperator operator = binaryExpression.getOperator();
    return (operator == BinaryOperator.PLUS_ASSIGN || operator == BinaryOperator.PLUS)
        && binaryExpression.getTypeDescriptor() == TypeDescriptors.get().javaLangString;
  }

  /**
   * See JLS 5.6.1.
   */
  public static boolean matchesUnaryNumericPromotionContext(PrefixExpression prefixExpression) {
    PrefixOperator operator = prefixExpression.getOperator();
    return TypeDescriptors.isBoxedOrPrimitiveType(prefixExpression.getTypeDescriptor())
        && (operator == PrefixOperator.PLUS
            || operator == PrefixOperator.MINUS
            || operator == PrefixOperator.COMPLEMENT);
  }

  /**
   * See JLS 5.6.1.
   */
  public static boolean matchesUnaryNumericPromotionContext(TypeDescriptor returnTypeDescriptor) {
    return TypeDescriptors.isBoxedOrPrimitiveType(returnTypeDescriptor);
  }

  /**
   * See JLS 5.6.1.
   */
  public static boolean matchesUnaryNumericPromotionContext(BinaryExpression binaryExpression) {
    return binaryExpression.getOperator().isShiftOperator();
  }

  /**
   * See JLS 5.6.2.
   */
  public static boolean matchesBinaryNumericPromotionContext(BinaryExpression binaryExpression) {
    // Both operands must be boxed or primitive.
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    BinaryOperator operator = binaryExpression.getOperator();

    return matchesBinaryNumericPromotionContext(leftOperand, operator, rightOperand);
  }

  /**
   * See JLS 5.6.2.
   */
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
   * See JLS 5.6.2.
   *
   * X and double -> double
   * X and float -> float
   * X and long -> long
   * otherwise -> int
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

  public static MethodDescriptor createStringValueOfMethodDescriptor() {
    return MethodDescriptorBuilder.fromDefault()
        .isStatic(true)
        .enclosingClassTypeDescriptor(TypeDescriptors.get().javaLangString)
        .methodName("valueOf")
        .returnTypeDescriptor(TypeDescriptors.get().javaLangString)
        .parameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().javaLangObject))
        .build();
  }

  /**
   * Returns true if {@code expression} is a string literal, or if it is String.valueOf() method
   * call, or if it is a BinaryExpression that matches String conversion context and one of its
   * operands is non null String.
   */
  public static boolean isNonNullString(Expression expression) {
    if (expression instanceof StringLiteral) {
      return true;
    }
    if (expression instanceof MethodCall) {
      return ((MethodCall) expression).getTarget() == createStringValueOfMethodDescriptor();
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
  static Expression getExplicitQualifier(Expression qualifier, Member member) {
    checkNotNull(member);
    if (qualifier != null) {
      return qualifier;
    }
    TypeDescriptor enclosingClassTypeDescriptor = member.getEnclosingClassTypeDescriptor();
    return member.isStatic()
        ? new TypeReference(enclosingClassTypeDescriptor)
        : new ThisReference(enclosingClassTypeDescriptor);
  }

  /**
   * Returns true if the qualifier of the given member reference is 'this' reference.
   */
  public static boolean hasThisReferenceAsQualifier(MemberReference memberReference) {
    Expression qualifier = memberReference.getQualifier();
    return qualifier instanceof ThisReference
        && qualifier.getTypeDescriptor()
            == memberReference.getTarget().getEnclosingClassTypeDescriptor();
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
    return new BinaryExpression(outputType, expressions.get(0), operator, joinedExpressions);
  }

  /**
   * Returns TypeDescriptor that contains the devirtualized JsOverlay methods of a native type.
   */
  public static TypeDescriptor createJsOverlayImplTypeDescriptor(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isNative());
    checkArgument(typeDescriptor instanceof RegularTypeDescriptor);
    RegularTypeDescriptor nativeTypeDescriptor = (RegularTypeDescriptor) typeDescriptor;
    return TypeDescriptor.createSyntheticRegularTypeDescriptor(
        nativeTypeDescriptor.getPackageComponents(),
        Iterables.concat(
            nativeTypeDescriptor.getClassComponents(),
            Arrays.asList(JS_OVERLAY_METHODS_IMPL_SUFFIX)),
        false,
        ImmutableList.<TypeDescriptor>of());
  }

  /**
   * Returns the constructor that is being delegated to (the primary constructor) in a JsConstructor
   * class or in a child class of a JsConstructor class. This constructor will be generated as the
   * real ES6 constructor.
   */
  public static Method getPrimaryConstructor(JavaType javaType) {
    if (!javaType.getDescriptor().subclassesJsConstructorClass()) {
      return null;
    }
    for (Method method : javaType.getMethods()) {
      if (method.isConstructor() && !AstUtils.hasThisCall(method)) {
        return method;
      }
    }
    return null;
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   */
  public static boolean areParameterErasureEqual(MethodDescriptor left, MethodDescriptor right) {
    List<TypeDescriptor> leftParameterTypeDescriptors = left.getParameterTypeDescriptors();
    List<TypeDescriptor> rightParameterTypeDescriptors = right.getParameterTypeDescriptors();
    if (!left.getMethodName().equals(right.getMethodName())
        || leftParameterTypeDescriptors.size() != rightParameterTypeDescriptors.size()) {
      return false;
    }
    for (int i = 0; i < leftParameterTypeDescriptors.size(); i++) {
      if (leftParameterTypeDescriptors.get(i).getRawTypeDescriptor()
          != rightParameterTypeDescriptors.get(i).getRawTypeDescriptor()) {
        return false;
      }
    }
    return true;
  }
}
