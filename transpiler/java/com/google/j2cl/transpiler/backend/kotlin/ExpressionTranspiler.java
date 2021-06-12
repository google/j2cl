/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;
import java.util.List;

/** Transforms Expression to Kotlin source strings. */
public class ExpressionTranspiler {
  public static void render(Expression expression, final SourceBuilder sourceBuilder) {
    if (expression == null) {
      return;
    }

    new AbstractVisitor() {
      @Override
      public boolean enterArrayAccess(ArrayAccess arrayAccess) {
        processLeftSubExpression(arrayAccess, arrayAccess.getArrayExpression());
        sourceBuilder.append("get(");
        renderNoParens(arrayAccess.getIndexExpression());
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterArrayLength(ArrayLength arrayLength) {
        processLeftSubExpression(arrayLength, arrayLength.getArrayExpression());
        sourceBuilder.append(".size");
        return false;
      }

      @Override
      public boolean enterArrayLiteral(ArrayLiteral arrayLiteral) {
        sourceBuilder.append("arrayOf");
        renderDelimitedAndCommaSeparated("(", ")", arrayLiteral.getValueExpressions());
        return false;
      }

      @Override
      public boolean enterBinaryExpression(BinaryExpression expression) {
        processLeftSubExpression(expression, expression.getLeftOperand());
        sourceBuilder.append(" " + expression.getOperator() + " ");
        processRightSubExpression(expression, expression.getRightOperand());
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expressionWithComment) {
        // Comments do not count as operations, but parenthesis will be emitted by the
        // outer context if needed given that getPrecedence is just a passthrough to the inner
        // expression.
        renderNoParens(expressionWithComment.getExpression());
        return false;
      }

      @Override
      public boolean enterFieldAccess(FieldAccess fieldAccess) {
        String fieldName = fieldAccess.getTarget().getName();
        renderQualifiedName(fieldAccess, fieldName);
        return false;
      }

      @Override
      public boolean enterFunctionExpression(FunctionExpression expression) {
        sourceBuilder.append("{ ");
        emitParameters(expression);
        // After the header is emitted, emit the rest of the arrow function.
        sourceBuilder.append(" ->");
        new StatementTranspiler(sourceBuilder).renderStatement(expression.getBody());
        sourceBuilder.append(" }");
        return false;
      }

      private void emitParameters(FunctionExpression expression) {
        List<Variable> parameters = expression.getParameters();
        String separator = "";
        for (Variable parameter : parameters) {
          sourceBuilder.append(separator);
          renderNoParens(parameter);
          separator = ", ";
        }
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        // Conditional expressions are in its own precedence class. So when they are nested in the
        // in the condition position they need parenthesis, but not in the second or third position.
        sourceBuilder.append("if (");
        processLeftSubExpression(
            conditionalExpression, conditionalExpression.getConditionExpression());
        sourceBuilder.append(") ");
        renderNoParens(conditionalExpression.getTrueExpression());
        sourceBuilder.append(" else ");
        renderNoParens(conditionalExpression.getFalseExpression());
        return false;
      }

      @Override
      public boolean enterLiteral(Literal literal) {
        if (literal instanceof TypeLiteral) {
           throw new IllegalArgumentException(literal + " not currently supported.");
        }
        // Assumes source literals have been translated to Kotlin format.
        sourceBuilder.append(literal.getSourceText());
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall expression) {
        if (expression.isStaticDispatch()) {
          throw new IllegalArgumentException(expression + " not currently supported.");
        } else {
          renderMethodCallHeader(expression);
          renderDelimitedAndCommaSeparated("(", ")", expression.getArguments());
        }
        return false;
      }

      private void renderQualifiedName(Expression enclosingExpression, String propertyName) {
        Expression qualifier = ((MemberReference) enclosingExpression).getQualifier();
        if (shouldRenderQualifier(qualifier)) {
          processLeftSubExpression(enclosingExpression, qualifier);
          sourceBuilder.append(".");
        }
        sourceBuilder.append(propertyName);
      }

      @SuppressWarnings("ReferenceEquality")
      private boolean shouldRenderQualifier(Expression expression) {
        checkNotNull(expression);
        if (!(expression instanceof JavaScriptConstructorReference)) {
          return true;
        }
        checkArgument(false, expression + " not supported.");
        return false;
      }

      private void renderMethodCallHeader(MethodCall expression) {
        checkArgument(!expression.isStaticDispatch());
        MethodDescriptor target = expression.getTarget();
        if (target.isConstructor()) {
          sourceBuilder.append("super");
        } else {
          renderQualifiedName(expression, target.getName());
        }
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        String separator = "";
        sourceBuilder.append("{ ");
        for (Expression expression : multiExpression.getExpressions()) {
          renderNoParens(expression);
          sourceBuilder.append(separator);
          renderNoParens(expression);
          separator = " ";
        }
        sourceBuilder.append(" }");
        return false;
      }

      @Override
      public boolean enterNewArray(NewArray expression) {
        checkArgument(false, expression + " should have been normalized.");
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance expression) {
        checkArgument(expression.getQualifier() == null);
        DeclaredTypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingTypeDescriptor().toRawTypeDescriptor();
        sourceBuilder.append("new " + targetTypeDescriptor);
        renderDelimitedAndCommaSeparated("(", ")", expression.getArguments());
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        sourceBuilder.append(numberLiteral.getValue().toString());
        return false;
      }

      @Override
      public boolean enterPostfixExpression(PostfixExpression expression) {
        checkArgument(!TypeDescriptors.isPrimitiveLong(expression.getTypeDescriptor()));
        processLeftSubExpression(expression, expression.getOperand());
        sourceBuilder.append(expression.getOperator().toString());
        return false;
      }

      @Override
      public boolean enterPrefixExpression(PrefixExpression expression) {
        sourceBuilder.append(expression.getOperator().toString());
        processRightSubExpression(expression, expression.getOperand());
        return false;
      }

      @Override
      public boolean enterSuperReference(SuperReference expression) {
        sourceBuilder.append("super");
        return false;
      }

      @Override
      public boolean enterThisReference(ThisReference expression) {
        sourceBuilder.append("this");
        return false;
      }

      @Override
      public boolean enterJavaScriptConstructorReference(
          JavaScriptConstructorReference expression) {
        checkArgument(false, expression + " not supported.");
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        renderDelimitedAndCommaSeparated("var ", "", variableDeclarationExpression.getFragments());
        return false;
      }

      @Override
      public boolean enterVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        Variable variable = fragment.getVariable();
        // Variable declarations are separated with comma, so no need for parens.
        sourceBuilder.append(variable.getName());
        sourceBuilder.append(": ");
        sourceBuilder.append(variable.getTypeDescriptor().getReadableDescription());
        if (fragment.getInitializer() != null) {
          sourceBuilder.append(" = ");
          renderNoParens(fragment.getInitializer());
        }
        return false;
      }

      @Override
      public boolean enterVariable(Variable variable) {
        sourceBuilder.append(variable.getName());
        sourceBuilder.append(": ");
        sourceBuilder.append(variable.getTypeDescriptor().getReadableDescription());
        return false;
      }

      @Override
      public boolean enterVariableReference(VariableReference expression) {
        sourceBuilder.append(expression.getTarget().getName());
        return false;
      }

      @Override
      public boolean enterExpression(Expression expression) {
        throw new IllegalStateException("Unhandled expression " + expression);
      }

      private void renderDelimitedAndCommaSeparated(
          String prefix, String suffix, Iterable<? extends Node> nodes) {
        sourceBuilder.append(prefix);
        renderCommaSeparated(nodes);
        sourceBuilder.append(suffix);
      }

      private void renderCommaSeparated(Iterable<? extends Node> nodes) {
        String separator = ", ";
        String currentSeparator = "";
        for (Node node : nodes) {
          if (node == null) {
            continue;
          }
          sourceBuilder.append(currentSeparator);
          currentSeparator = separator;
          // Comma separated expressions never need enclosing parens.
          renderNoParens(node);
        }
      }

      private void processLeftSubExpression(Expression expression, Expression operand) {
        renderExpression(operand, expression.requiresParensOnLeft(operand));
      }

      private void processRightSubExpression(Expression expression, Expression operand) {
        renderExpression(operand, expression.requiresParensOnRight(operand));
      }

      private void renderExpression(Expression expression, boolean needsParentheses) {
        if (needsParentheses) {
          sourceBuilder.append("(");
          renderNoParens(expression);
          sourceBuilder.append(")");
        } else {
          renderNoParens(expression);
        }
      }

      private void renderNoParens(Node node) {
        node.accept(this);
      }
    }.renderNoParens(expression);
  }
}
