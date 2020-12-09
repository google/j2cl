/*
 * Copyright 2020 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isPrimitiveVoid;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.List;

/**
 * Transforms expressions into WASM code.
 *
 * <p>As is typical in stack based VMs, expressions evaluate leaving the result in the stack.
 */
final class ExpressionTranspiler {
  public static void render(
      Expression expression,
      final SourceBuilder sourceBuilder,
      final GenerationEnvironment environment) {

    new AbstractVisitor() {
      @Override
      public boolean enterBooleanLiteral(BooleanLiteral booleanLiteral) {
        sourceBuilder.append("(i32.const " + (booleanLiteral.getValue() ? "1" : "0") + ")");
        return false;
      }

      @Override
      public boolean enterBinaryExpression(BinaryExpression expression) {
        BinaryOperator operator = expression.getOperator();
        if (operator == BinaryOperator.ASSIGN) {
          return renderAssignmentExpression(expression);
        }

        renderBinaryOperation(expression);
        return false;
      }

      private void renderBinaryOperation(BinaryExpression expression) {
        WasmBinaryOperation wasmOperation = WasmBinaryOperation.getOperation(expression);
        if (wasmOperation == null) {
          // TODO(dramaix): remove and checkArgument once every operator is implemented.
          enterExpression(expression);
          return;
        }

        sourceBuilder.append("(" + wasmOperation.getInstruction(expression) + " ");
        renderTypedExpression(
            wasmOperation.getOperandType(expression), expression.getLeftOperand());
        sourceBuilder.append(" ");
        renderTypedExpression(
            wasmOperation.getOperandType(expression), expression.getRightOperand());
        sourceBuilder.append(")");
      }

      private boolean renderAssignmentExpression(BinaryExpression expression) {
        Expression left = expression.getLeftOperand();
        if (left instanceof VariableReference) {
          renderVariableAssignment(
              ((VariableReference) left).getTarget(), expression.getRightOperand());
          return false;
        }
        return enterExpression(expression);
      }

      @Override
      public boolean enterCastExpression(CastExpression castExpression) {
        TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
        if (castExpression.getExpression().getTypeDescriptor().isPrimitive()) {
          // TODO(b/170691747): Remove when boxing is done.
          return enterExpression(castExpression);
        }
        if (castTypeDescriptor.isInterface()) {
          // TODO(rluble): implement interface casts.
          render(castExpression.getExpression());
          return false;
        } else if (castTypeDescriptor.isClass() || castTypeDescriptor.isEnum()) {
          sourceBuilder.append(
              String.format(
                  "(ref.cast %s %s ",
                  environment.getWasmTypeName(
                      castExpression.getExpression().getDeclaredTypeDescriptor()),
                  environment.getWasmTypeName(castTypeDescriptor)));
          render(castExpression.getExpression());
          sourceBuilder.append(
              String.format(
                  " (global.get %s))",
                  environment.getRttGlobalName(
                      ((DeclaredTypeDescriptor) castTypeDescriptor).getTypeDeclaration())));
          return false;
        }
        // TODO(rluble): handle primitive and array casts.
        return enterExpression(castExpression);
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        TypeDescriptor typeDescriptor = conditionalExpression.getTypeDescriptor();
        sourceBuilder.append("(if (result " + environment.getWasmType(typeDescriptor) + ") ");
        renderTypedExpression(
            PrimitiveTypes.BOOLEAN, conditionalExpression.getConditionExpression());
        sourceBuilder.append(" (then ");
        renderTypedExpression(typeDescriptor, conditionalExpression.getTrueExpression());
        sourceBuilder.append(") (else ");
        renderTypedExpression(typeDescriptor, conditionalExpression.getFalseExpression());
        sourceBuilder.append("))");
        return false;
      }

      @Override
      public boolean enterExpression(Expression expression) {
        // TODO(rluble): remove this method which is only a place holder until all expressions are
        // implemented.
        if (!isPrimitiveVoid(expression.getTypeDescriptor())) {
          // This is an unimplemented expression that returns a value (i.e. not a call to a
          // method returning void).
          // Emit the default value for the type as a place holder so that the module compiles.
          render(expression.getTypeDescriptor().getDefaultValue());
        }
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expressionWithComment) {
        sourceBuilder.append(";; " + expressionWithComment.getComment());
        render(expressionWithComment.getExpression());
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall methodCall) {
        MethodDescriptor target = methodCall.getTarget();
        if (target.isStatic()) {
          sourceBuilder.append("(call " + environment.getMethodImplementationName(target) + " ");
          renderTypedExpressions(target.getParameterTypeDescriptors(), methodCall.getArguments());
          sourceBuilder.append(")");
        } else {
          // TODO(rluble): remove once all method call types are implemented.
          return super.enterMethodCall(methodCall);
        }
        return false;
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        List<Expression> expressions = multiExpression.getExpressions();
        Expression returnValue = Iterables.getLast(expressions);
        sourceBuilder.append("(block");
        sourceBuilder.indent();
        sourceBuilder.newLine();
        sourceBuilder.append(
            "(result " + environment.getWasmType(returnValue.getTypeDescriptor()) + ")");
        expressions.forEach(
            expression -> {
              sourceBuilder.newLine();
              render(expression);
              if (!isPrimitiveVoid(expression.getTypeDescriptor()) && expression != returnValue) {
                // Remove the result of the expression from the stack.
                sourceBuilder.newLine();
                sourceBuilder.append("drop");
              }
            });
        sourceBuilder.unindent();
        sourceBuilder.newLine();
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance newInstance) {
        MethodDescriptor target = newInstance.getTarget();
        sourceBuilder.append("(call " + environment.getMethodImplementationName(target) + " ");
        sourceBuilder.append(
            String.format(
                "(struct.new_default_with_rtt %s (global.get %s)) ",
                environment.getWasmTypeName(newInstance.getTypeDescriptor()),
                environment.getRttGlobalName(
                    newInstance.getTypeDescriptor().getTypeDeclaration())));
        renderTypedExpressions(target.getParameterTypeDescriptors(), newInstance.getArguments());
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral nullLiteral) {
        sourceBuilder.append(
            "(ref.null " + environment.getWasmTypeName(nullLiteral.getTypeDescriptor()) + ")");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        PrimitiveTypeDescriptor typeDescriptor = numberLiteral.getTypeDescriptor();
        String wasmType = checkNotNull(environment.getWasmType(typeDescriptor));
        sourceBuilder.append("(" + wasmType + ".const " + numberLiteral.getValue() + ")");
        return false;
      }

      @Override
      public boolean enterUnaryExpression(UnaryExpression expression) {
        WasmUnaryOperation wasmUnaryOperation = WasmUnaryOperation.get(expression);

        sourceBuilder.append("(" + wasmUnaryOperation.getInstruction(expression) + " ");
        renderTypedExpression(
            wasmUnaryOperation.getOperandType(expression), expression.getOperand());

        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(VariableDeclarationExpression expression) {
        expression.getFragments().forEach(this::renderVariableDeclarationFragment);
        return false;
      }

      private void renderVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        if (fragment.getInitializer() != null) {
          renderVariableAssignment(fragment.getVariable(), fragment.getInitializer());
          sourceBuilder.newLine();
          sourceBuilder.append("drop");
          sourceBuilder.newLine();
        }
      }

      private void renderVariableAssignment(Variable variable, Expression assignment) {
        sourceBuilder.append("(local.tee " + environment.getDeclarationName(variable) + " ");
        renderTypedExpression(variable.getTypeDescriptor(), assignment);
        sourceBuilder.append(")");
      }

      // TODO(dramaix): remove when coercions and casting are in place.
      private void renderTypedExpression(TypeDescriptor typeDescriptor, Expression expression) {
        ExpressionTranspiler.renderTypedExpression(
            typeDescriptor, expression, sourceBuilder, environment);
      }

      private void renderTypedExpressions(
          List<TypeDescriptor> typeDescriptors, List<Expression> expressions) {
        checkArgument(typeDescriptors.size() == expressions.size());
        for (int i = 0; i < typeDescriptors.size(); i++) {
          renderTypedExpression(typeDescriptors.get(i), expressions.get(i));
        }
      }

      @Override
      public boolean enterVariableReference(VariableReference variableReference) {
        sourceBuilder.append(
            "(local.get " + environment.getDeclarationName(variableReference.getTarget()) + ")");
        return false;
      }

      private void render(Expression expression) {
        expression.accept(this);
      }
    }.render(expression);
  }

  // TODO(dramaix): remove when coercions and casting are in place.
  public static void renderTypedExpression(
      TypeDescriptor typeDescriptor,
      Expression expression,
      SourceBuilder sourceBuilder,
      GenerationEnvironment environment) {
    boolean isAssignable =
        typeDescriptor.isPrimitive() || expression.getTypeDescriptor().isPrimitive()
            ? typeDescriptor.equals(expression.getTypeDescriptor())
            : expression.getTypeDescriptor().isAssignableTo(typeDescriptor);
    if (isAssignable) {
      render(expression, sourceBuilder, environment);
    } else {
      render(typeDescriptor.getDefaultValue(), sourceBuilder, environment);
    }
  }

  private ExpressionTranspiler() {}
}
