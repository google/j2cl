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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility functions to manipulate J2CL AST.
 */
public class AstUtils {

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
    return MethodDescriptor.create(
        false,
        Visibility.PRIVATE,
        enclosingClassTypeDescriptor,
        MethodDescriptor.INIT_METHOD_NAME,
        false,
        false,
        TypeDescriptors.get().primitiveVoid);
  }

  /**
   * Create constructor MethodDescriptor.
   */
  public static MethodDescriptor createConstructorDescriptor(
      TypeDescriptor typeDescriptor,
      Visibility visibility,
      TypeDescriptor... parameterTypeDescriptors) {
    return MethodDescriptor.create(
        false,
        visibility,
        typeDescriptor,
        typeDescriptor.getClassName(),
        true,
        false,
        TypeDescriptors.get().primitiveVoid,
        parameterTypeDescriptors);
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor,
   * or returns null if it does not have one.
   */
  public static MethodCall getConstructorInvocation(Method method) {
    Preconditions.checkArgument(method.isConstructor());
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
    return FieldDescriptor.create(
        false, // not static
        Visibility.PUBLIC,
        enclosingClassDescriptor,
        ENCLOSING_INSTANCE_NAME,
        fieldTypeDescriptor);
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
    boolean isStatic = false;
    String methodName = CREATE_PREFIX + innerclassConstructorDescriptor.getMethodName();
    boolean isConstructor = false;
    boolean isNative = false;
    TypeDescriptor returnTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    // if the inner class is a generic type, add its type parameters to the wrapper method.
    List<TypeDescriptor> typeParameterDescriptors = new ArrayList<>();
    TypeDescriptor innerclassTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    if (innerclassTypeDescriptor.isParameterizedType()) {
      typeParameterDescriptors.addAll(
          Lists.newArrayList(
              Iterables.filter( // filters out the type parameters declared in the outer class.
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
    return MethodDescriptor.create(
        isStatic,
        innerclassConstructorDescriptor.getVisibility(),
        outerclassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        innerclassConstructorDescriptor.getParameterTypeDescriptors(),
        typeParameterDescriptors);
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
            newInnerClass,
            innerclassConstructorDescriptor.getReturnTypeDescriptor())); // return new InnerClass();
    Block body = new Block(statements);

    return new Method(methodDescriptor, innerclassConstructor.getParameters(), body);
  }

  /**
   * Returns forwarding method for exposed package private methods (which means that one of its
   * overriding method is public or protected, so the package private method is exposed).
   * The forwarding method is like:
   * exposedMethodDescriptor (args) {return this.forwardToMethodDescriptor(args);}
   */
  public static Method createForwardingMethod(
      MethodDescriptor exposedMethodDescriptor, MethodDescriptor forwardToMethodDescriptor) {
    MethodDescriptor forwardingMethodDescriptor =
        MethodDescriptorBuilder.from(exposedMethodDescriptor)
            .enclosingClassTypeDescriptor(
                forwardToMethodDescriptor.getEnclosingClassTypeDescriptor())
            .returnTypeDescriptor(forwardToMethodDescriptor.getReturnTypeDescriptor())
            .build();
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    for (int i = 0; i < exposedMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          new Variable(
              "arg" + i, exposedMethodDescriptor.getParameterTypeDescriptors().get(i), false, true);
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }
    Expression forwardingMethodCall =
        MethodCall.createRegularMethodCall(null, forwardToMethodDescriptor, arguments);
    Statement statement =
        exposedMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.get().primitiveVoid
            ? new ExpressionStatement(forwardingMethodCall)
            : new ReturnStatement(
                forwardingMethodCall, exposedMethodDescriptor.getReturnTypeDescriptor());
    return Method.createSynthetic(
        forwardingMethodDescriptor, parameters, new Block(Arrays.asList(statement)), false, true);
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method
   * in {@code enclosingClassTypeDescriptor} with the {@code instanceTypeDescriptor} as the first
   * parameter type.
   *
   * <p>instance.instanceMethod(a, b) => staticMethod(instance, a, b)
   */
  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall,
      TypeDescriptor enclosingClassTypeDescriptor,
      TypeDescriptor instanceTypeDescriptor) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    Preconditions.checkArgument(!targetMethodDescriptor.isConstructor());
    Preconditions.checkArgument(!targetMethodDescriptor.isStatic());

    MethodDescriptor methodDescriptor =
        MethodDescriptorBuilder.from(targetMethodDescriptor)
            .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
            .parameterTypeDescriptors(
                Iterables.concat(
                    Arrays.asList(instanceTypeDescriptor), // add the first parameter type.
                    targetMethodDescriptor.getParameterTypeDescriptors()))
            .isStatic(true)
            .build();

    List<Expression> arguments = methodCall.getArguments();
    // Turn the instance into now a first parameter to the devirtualized method.
    Expression instance = methodCall.getQualifier();
    Preconditions.checkNotNull(instance);
    arguments.add(0, instance);
    // Call the method like Objects.foo(instance, ...)
    return MethodCall.createRegularMethodCall(null, methodDescriptor, arguments);
  }

  /**
   * Boxes {@code expression} using the valueOf() method of the corresponding boxed type.
   * e.g. expression => Integer.valueOf(expression).
   */
  public static Expression box(Expression expression) {
    TypeDescriptor primitiveType = expression.getTypeDescriptor();
    Preconditions.checkArgument(TypeDescriptors.isPrimitiveType(primitiveType));
    Preconditions.checkArgument(!TypeDescriptors.isPrimitiveBooleanOrDouble(primitiveType));
    TypeDescriptor boxType = TypeDescriptors.getBoxTypeFromPrimitiveType(primitiveType);

    MethodDescriptor valueOfMethodDescriptor =
        MethodDescriptor.create(
            true, // isStatic
            Visibility.PUBLIC,
            boxType, // enclosingClass
            MethodDescriptor.VALUE_OF_METHOD_NAME,
            false, // isConstructor,
            false, // isNative,
            boxType, // returnTypeDescriptor,
            primitiveType // parameterTypeDescriptor
            );
    return MethodCall.createRegularMethodCall(
        null, valueOfMethodDescriptor, Arrays.asList(expression));
  }

  /**
   * Unboxes {expression} using the ***Value() method of the corresponding boxed type.
   * e.g expression => expression.intValue().
   */
  public static Expression unbox(Expression expression) {
    TypeDescriptor boxType = expression.getTypeDescriptor();
    Preconditions.checkArgument(TypeDescriptors.isBoxedType(boxType));
    Preconditions.checkArgument(!TypeDescriptors.isBoxedBooleanOrDouble(boxType));
    TypeDescriptor primitiveType = TypeDescriptors.getPrimitiveTypeFromBoxType(boxType);

    MethodDescriptor valueMethodDescriptor =
        MethodDescriptor.create(
            false, // isStatic
            Visibility.PUBLIC,
            boxType, // enclosingClass
            primitiveType.getSimpleName() + MethodDescriptor.VALUE_METHOD_SUFFIX,
            false, // isConstructor,
            false, // isNative,
            primitiveType // returnTypeDescriptor
            );

    // We want "(a ? b : c).intValue()", not "a ? b : c.intValue()".
    expression =
        isValidMethodCallQualifier(expression)
            ? expression
            : new ParenthesizedExpression(expression);

    return MethodCall.createRegularMethodCall(
        expression, valueMethodDescriptor, new ArrayList<Expression>());
  }

  /**
   * Returns whether the given expression is a syntactically valid qualifier for a MethodCall.
   */
  private static boolean isValidMethodCallQualifier(Expression expression) {
    return !(expression instanceof TernaryExpression
        || expression instanceof BinaryExpression
        || expression instanceof PrefixExpression
        || expression instanceof PostfixExpression);
  }

  public static boolean isConstructorOfImmediateNestedClass(Method method, JavaType targetType) {
    if (method == null || targetType == null || targetType.getEnclosingTypeDescriptor() == null) {
      return false;
    }
    if (method.getDescriptor().getEnclosingClassTypeDescriptor() != targetType.getDescriptor()) {
      return false;
    }
    return !targetType.isStatic() && method.isConstructor();
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

  public static boolean isAssignmentOperator(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case ASSIGN:
      case PLUS_ASSIGN:
      case MINUS_ASSIGN:
      case TIMES_ASSIGN:
      case DIVIDE_ASSIGN:
      case BIT_AND_ASSIGN:
      case BIT_OR_ASSIGN:
      case BIT_XOR_ASSIGN:
      case REMAINDER_ASSIGN:
      case LEFT_SHIFT_ASSIGN:
      case RIGHT_SHIFT_SIGNED_ASSIGN:
      case RIGHT_SHIFT_UNSIGNED_ASSIGN:
        return true;
      default:
        return false;
    }
  }

  public static boolean isValidForLongs(BinaryOperator binaryOperator) {
    return binaryOperator != BinaryOperator.CONDITIONAL_AND
        && binaryOperator != BinaryOperator.CONDITIONAL_OR;
  }

  /**
   * See JLS 5.2.
   *
   * <p>Would normally also verify that the right operand type is being changed, but we're leaving
   * that check up to our conversion implementation(s)
   */
  public static boolean matchesAssignmentContext(BinaryOperator binaryOperator) {
    return binaryOperator.doesAssignment();
  }

  /**
   * See JLS 5.4.
   */
  public static boolean matchesStringContext(BinaryExpression binaryExpression) {
    BinaryOperator operator = binaryExpression.getOperator();
    TypeDescriptor leftTypeDescriptor = binaryExpression.getLeftOperand().getTypeDescriptor();
    TypeDescriptor rightTypeDescriptor = binaryExpression.getRightOperand().getTypeDescriptor();

    if (operator == BinaryOperator.PLUS_ASSIGN
        && leftTypeDescriptor == TypeDescriptors.get().javaLangString) {
      return true;
    }
    if (operator == BinaryOperator.PLUS
        && (leftTypeDescriptor == TypeDescriptors.get().javaLangString
            || rightTypeDescriptor == TypeDescriptors.get().javaLangString)) {
      return true;
    }
    return false;
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
    switch (binaryExpression.getOperator()) {
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        return true;
      default:
        return false;
    }
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
      case EQUALS:
      case NOT_EQUALS:
      case XOR:
      case AND:
      case OR:
        return true;
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
    Preconditions.checkArgument(leftTypeDescriptor.isPrimitive());
    Preconditions.checkArgument(rightTypeDescriptor.isPrimitive());

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
    return MethodDescriptor.create(
        true, // static
        Visibility.PUBLIC,
        TypeDescriptors.get().javaLangString,
        "valueOf",
        false, // constructor
        false, // native
        TypeDescriptors.get().javaLangString,
        Lists.newArrayList(TypeDescriptors.get().javaLangObject));
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
}
