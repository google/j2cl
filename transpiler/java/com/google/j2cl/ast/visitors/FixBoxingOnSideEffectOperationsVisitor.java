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
package com.google.j2cl.ast.visitors;

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.generator.TranspilerUtils;

import java.util.Arrays;

/**
 * Most autoboxing is handled by {@code CompilationUnitBuilder}. The only cases it does not handle
 * are {@code ++}, {@code --}, and compound assignment operations ({@code +=}, etc.) when applied to
 * a boxed type. This class fixes these cases.
 */
public class FixBoxingOnSideEffectOperationsVisitor extends AbstractRewriter {
  private static final String NUMBERS_Q = "$q";
  private static final String NUMBERS_V = "$v";

  public static void doFixBoxingOnSideEffectOperations(CompilationUnit compilationUnit) {
    new FixBoxingOnSideEffectOperationsVisitor().fixBoxingOnSideEffectOperations(compilationUnit);
  }

  private void fixBoxingOnSideEffectOperations(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression expression) {
    BinaryOperator operator = expression.getOperator();
    Expression leftOperand = expression.getLeftOperand();
    Expression rightOperand = expression.getRightOperand();
    if (!TranspilerUtils.isAssignment(operator) || !isValueMethodCall(leftOperand)) {
      return expression;
    }

    // Fix the case boxA.intValue() += x;

    Preconditions.checkArgument(leftOperand instanceof MethodCall);
    MethodCall valueMethodCall = (MethodCall) leftOperand;
    Expression boxedInstance = valueMethodCall.getQualifier(); // boxA, the boxed instance.
    Expression qualifier = getQualifier(boxedInstance); // qualifier of boxed instance.

    TypeDescriptor primitiveType = leftOperand.getTypeDescriptor();
    Preconditions.checkArgument(primitiveType.isPrimitive());
    TypeDescriptor boxType = TypeDescriptors.boxedTypeByPrimitiveType.get(primitiveType);

    // The corresponding binary operator, e.g += => +, -= => -, etc.
    BinaryOperator binaryOperator = TranspilerUtils.getBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // boxA.intValue() += x => boxA = box(boxA.intValue() + x)
      return boxExpressionNoQualifier(
          primitiveType, boxType, valueMethodCall, binaryOperator, rightOperand);
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.boxA.intValue() += b; =>
    // (Numbers.$q = q, Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + b))
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression boxAssignment =
        boxExpressionWithQualifier(
            primitiveType,
            boxType,
            valueMethodCall,
            binaryOperator,
            rightOperand); // Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + b)
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, boxAssignment));
  }

  @Override
  public Node rewritePrefixExpression(PrefixExpression expression) {
    Expression operand = expression.getOperand();
    PrefixOperator operator = expression.getOperator();
    if (!(TranspilerUtils.hasSideEffect(operator)) || !isValueMethodCall(operand)) {
      return expression;
    }

    // Fix the case ++boxA.intValue()

    Preconditions.checkArgument(operand instanceof MethodCall);
    MethodCall valueMethodCall = (MethodCall) operand;
    Expression boxedInstance = valueMethodCall.getQualifier(); // boxA, the boxed instance
    Expression qualifier = getQualifier(boxedInstance); // qualifier of the boxed instance

    TypeDescriptor primitiveType = operand.getTypeDescriptor();
    Preconditions.checkArgument(primitiveType.isPrimitive());
    TypeDescriptor boxType = TypeDescriptors.boxedTypeByPrimitiveType.get(primitiveType);

    // The corresponding binary operator, e.g ++ => +, -- => -
    BinaryOperator binaryOperator = TranspilerUtils.getBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // ++boxA.intValue() => boxA = box(boxA.intValue() + 1)
      return boxExpressionNoQualifier(
          primitiveType, boxType, valueMethodCall, binaryOperator, getOne(primitiveType));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // ++q.boxA.intValue(); =>
    // (Numbers.$q = q, Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + 1))
    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    Expression boxAssignment =
        boxExpressionWithQualifier(
            primitiveType,
            boxType,
            valueMethodCall,
            binaryOperator,
            getOne(primitiveType)); // Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + 1)
    return new MultiExpression(Arrays.<Expression>asList(assignQualifier, boxAssignment));
  }

  @Override
  public Node rewritePostfixExpression(PostfixExpression expression) {
    Expression operand = expression.getOperand();
    PostfixOperator operator = expression.getOperator();
    if (!isValueMethodCall(operand)) {
      return expression;
    }

    // Fix the case boxA.intValue()++

    Preconditions.checkArgument(operand instanceof MethodCall);
    MethodCall valueMethodCall = (MethodCall) operand;
    Expression boxedInstance = valueMethodCall.getQualifier(); // boxA, the boxed instance.
    Expression qualifier = getQualifier(boxedInstance);

    TypeDescriptor primitiveType = operand.getTypeDescriptor();
    Preconditions.checkArgument(primitiveType.isPrimitive());
    TypeDescriptor boxType = TypeDescriptors.boxedTypeByPrimitiveType.get(primitiveType);

    // The corresponding binary operator, e.g ++ => +, -- => -
    BinaryOperator binaryOperator = TranspilerUtils.getBinaryOperator(operator);

    if (qualifier == null) {
      // The referenced expression *is* being modified but it has no qualifier so no care needs to
      // be taken to avoid double side-effects from dereferencing the qualifier twice.
      // boxA.intValue()++; => (Numbers.$v = boxA, boxA = box(boxA.intValue() + 1), Numbers.$v)
      BinaryExpression assignVar =
          new BinaryExpression(
              boxType,
              createNumbersFieldAccess(NUMBERS_V),
              BinaryOperator.ASSIGN,
              boxedInstance); //Numbers.$v = boxA
      BinaryExpression boxAssignment =
          boxExpressionNoQualifier(
              primitiveType,
              boxType,
              valueMethodCall,
              binaryOperator,
              getOne(primitiveType)); // boxA = box(boxA.intValue() + 1)
      return new MultiExpression(
          Arrays.<Expression>asList(assignVar, boxAssignment, createNumbersFieldAccess(NUMBERS_V)));
    }

    // The referenced expression is being modified and it has a qualifier. Take special
    // care to only dereference the qualifier once (to avoid double side effects), store it in a
    // temporary variable and use that temporary variable in the rest of the computation.
    // q.boxA.intValue()++; =>
    // (Numbers.$q = q, Numbers.$v = Numbers.$q.boxA,
    //  Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + 1), Numbers.$v)
    Preconditions.checkArgument(boxedInstance instanceof FieldAccess);
    FieldDescriptor target = ((FieldAccess) boxedInstance).getTarget();

    BinaryExpression assignQualifier =
        new BinaryExpression(
            qualifier.getTypeDescriptor(),
            createNumbersFieldAccess(NUMBERS_Q),
            BinaryOperator.ASSIGN,
            qualifier); // Numbers.$q = q
    BinaryExpression assignVar =
        new BinaryExpression(
            boxType,
            createNumbersFieldAccess(NUMBERS_V),
            BinaryOperator.ASSIGN,
            new FieldAccess(
                createNumbersFieldAccess(NUMBERS_Q), target)); //Numbers.$v = Numbers.$q.boxA
    BinaryExpression boxAssignment =
        boxExpressionWithQualifier(
            primitiveType,
            boxType,
            valueMethodCall,
            binaryOperator,
            getOne(primitiveType)); // Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() + 1)
    return new MultiExpression(
        Arrays.<Expression>asList(
            assignQualifier, assignVar, boxAssignment, createNumbersFieldAccess(NUMBERS_V)));
  }

  /**
   * For {@code valueMethodCall} in the form of boxA.intValue(),
   * returns BinaryExpression in the form of:
   * boxA = box(boxA.intValue() operator rightOperand).
   */
  private static BinaryExpression boxExpressionNoQualifier(
      TypeDescriptor primitiveTypeDescriptor,
      TypeDescriptor boxTypeDescriptor,
      MethodCall valueMethodCall,
      BinaryOperator operator,
      Expression rightOperand) {
    BinaryExpression binaryExpression =
        new BinaryExpression(primitiveTypeDescriptor, valueMethodCall, operator, rightOperand);
    return new BinaryExpression(
        boxTypeDescriptor,
        valueMethodCall.getQualifier(),
        BinaryOperator.ASSIGN,
        ASTUtils.box(binaryExpression));
  }

  /**
   * For {@code valueMethodCall} in the form of qualifier.boxA.intValue(),
   * returns BinaryExpression in the form of:
   * Numbers.$q.boxA = box(Numbers.$q.boxA.intValue() operator rightOperand).
   */
  private static BinaryExpression boxExpressionWithQualifier(
      TypeDescriptor primitiveTypeDescriptor,
      TypeDescriptor boxTypeDescriptor,
      MethodCall valueMethodCall,
      BinaryOperator operator,
      Expression rightOperand) {
    Expression boxedInstance = valueMethodCall.getQualifier(); // qualifier.boxA
    Preconditions.checkArgument(boxedInstance instanceof FieldAccess);
    FieldDescriptor target = ((FieldAccess) boxedInstance).getTarget(); // boxA

    BinaryExpression binaryExpression =
        new BinaryExpression(
            primitiveTypeDescriptor,
            new MethodCall(
                new FieldAccess(createNumbersFieldAccess(NUMBERS_Q), target), // Numbers.$q.boxA
                valueMethodCall.getTarget(),
                valueMethodCall.getArguments()), // Numbers.$q.boxA.intValue()
            operator,
            rightOperand);
    BinaryExpression assignment =
        new BinaryExpression(
            boxTypeDescriptor,
            new FieldAccess(createNumbersFieldAccess(NUMBERS_Q), target), // Numbers.$q.boxA
            BinaryOperator.ASSIGN,
            ASTUtils.box(binaryExpression));
    return assignment;
  }

  /**
   * Returns if the expression is the method call to Integer.intValue(), or Byte.bytevalue(), etc.
   */
  private static boolean isValueMethodCall(Expression expression) {
    if (!(expression instanceof MethodCall)) {
      return false;
    }
    MethodCall methodCall = (MethodCall) expression;
    MethodDescriptor target = methodCall.getTarget();
    TypeDescriptor enclosingTypeDescriptor = target.getEnclosingClassTypeDescriptor();
    if (!TypeDescriptors.boxedTypeByPrimitiveType.containsValue(enclosingTypeDescriptor)) {
      return false; // not a boxed type.
    }
    TypeDescriptor primitiveTypeDescriptor =
        TypeDescriptors.boxedTypeByPrimitiveType.inverse().get(enclosingTypeDescriptor);
    String expectedMethodName =
        primitiveTypeDescriptor.getSimpleName() + MethodDescriptor.VALUE_METHOD_SUFFIX;
    return target.getMethodName().equals(expectedMethodName);
  }

  /**
   * If expression is FieldAccess, returns its qualifier. Otherwise returns null.
   */
  private static Expression getQualifier(Expression expression) {
    if (!(expression instanceof FieldAccess)) {
      return null;
    }
    return ((FieldAccess) expression).getQualifier();
  }

  /**
   * Returns Numbers.fieldName.
   */
  private static FieldAccess createNumbersFieldAccess(String fieldName) {
    return new FieldAccess(
        null,
        FieldDescriptor.createRaw(
            true, // isStatic
            TypeDescriptors.NUMBERS_TYPE_DESCRIPTOR,
            fieldName,
            TypeDescriptors.OBJECT_TYPE_DESCRIPTOR));
  }

  /**
   * Returns number literal with value 1.
   */
  private static Expression getOne(TypeDescriptor typeDescriptor) {
    return (TypeDescriptors.LONG_TYPE_DESCRIPTOR == typeDescriptor)
        ? new NumberLiteral(typeDescriptor, 1L)
        : new NumberLiteral(typeDescriptor, 1);
  }
}
