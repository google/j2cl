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
package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLength;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.AwaitExpression;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionWithComment;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeVariable;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableReference;
import com.google.j2cl.common.SourcePosition;
import java.util.Collections;
import java.util.List;

/**
 * Transforms Expression to JavaScript source strings.
 */
public class ExpressionTranspiler {
  public static void render(
      Expression expression,
      final GenerationEnvironment environment,
      final SourceBuilder sourceBuilder) {

    if (expression == null) {
      return;
    }

    // TODO(rluble): create a visitor based abstraction for cases like this where the only
    // feature that is needed is the delegated dynamic dispatch.
    new AbstractVisitor() {
      @Override
      public boolean enterArrayAccess(ArrayAccess arrayAccess) {
        processLeftSubExpression(arrayAccess, arrayAccess.getArrayExpression());
        sourceBuilder.append("[");
        renderNoParens(arrayAccess.getIndexExpression());
        sourceBuilder.append("]");
        return false;
      }

      @Override
      public boolean enterArrayLength(ArrayLength arrayLength) {
        processLeftSubExpression(arrayLength, arrayLength.getArrayExpression());
        sourceBuilder.append(".length");
        return false;
      }

      @Override
      public boolean enterArrayLiteral(ArrayLiteral arrayLiteral) {
        renderDelimitedAndCommaSeparated("[", "]", arrayLiteral.getValueExpressions());
        return false;
      }

      @Override
      public boolean enterAwaitExpression(AwaitExpression awaitExpression) {
        sourceBuilder.append("await ");
        processRightSubExpression(awaitExpression, awaitExpression.getExpression());
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
      public boolean enterBooleanLiteral(BooleanLiteral expression) {
        sourceBuilder.append(expression.getValue() ? "true" : "false");
        return false;
      }

      @Override
      public boolean enterCastExpression(CastExpression castExpression) {
        checkArgument(
            false, castExpression + " CastExpression should have been normalized to method call.");
        return false;
      }

      @Override
      public boolean enterJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
        String jsdoc = environment.getClosureTypeString(jsDocCastExpression.getTypeDescriptor());
        // Parenthesis are part of JsDoc casts no need to add extra.
        sourceBuilder.append("/**@type {" + jsdoc + "}*/ (");
        renderNoParens(jsDocCastExpression.getExpression());
        sourceBuilder.append(")");
        return false;
      }

      @Override
      public boolean enterExpressionWithComment(ExpressionWithComment expressionWithComment) {
        // Comments do not count as operations, but parenthesis will be emitted by the
        // outer context if needed given that getPrecedence is just a passthrough to the inner
        // expression.
        renderNoParens(expressionWithComment.getExpression());
        sourceBuilder.append(" /* " + expressionWithComment.getComment() + " */");
        return false;
      }

      @Override
      public boolean enterFieldAccess(FieldAccess fieldAccess) {
        String fieldMangledName = fieldAccess.getTarget().getMangledName();
        renderQualifiedName(fieldAccess, fieldMangledName, fieldAccess.getSourcePosition());
        return false;
      }

      @Override
      public boolean enterFunctionExpression(FunctionExpression expression) {
        if (expression.getDescriptor().isJsAsync()) {
          sourceBuilder.append("async ");
        }

        emitParameters(expression);

        // After the header is emitted, emit the rest of the arrow function.
        sourceBuilder.append(" =>");
        new StatementTranspiler(sourceBuilder, environment).renderStatement(expression.getBody());

        return false;
      }

      private void emitParameters(FunctionExpression expression) {
        List<Variable> parameters = expression.getParameters();
        sourceBuilder.append("(");

        String separator = "";
        for (int i = 0; i < parameters.size(); i++) {
          sourceBuilder.append(separator);
          // Emit parameters in the more readable inline short form.
          emitParameter(expression, i);
          separator = ", ";
        }
        sourceBuilder.append(")");
      }

      private void emitParameter(FunctionExpression expression, int i) {
        Variable parameter = expression.getParameters().get(i);

        if (parameter == expression.getJsVarargsParameter()) {
          sourceBuilder.append("...");
        }

        // Avoid explicitly declaring unknown parameters in anonymous functions to avoid spurious
        // conformance errors. Parameter annotations are not required for anonymous functions so
        // they can be safely skipped here.
        if (!isUnknownTypeParameter(expression, i)) {
          // The inline type annotation for parameters has to be just right preceding the  parameter
          // name, hence if it is a varargs parameter then it would be emitted as follows:
          // ... /* <inline type annotation> */ <parameter name>
          //
          sourceBuilder.append("/** " + environment.getJsDocForParameter(expression, i) + " */ ");
        }
        // Render the parameter, which is not an expression but is just a name, so no parens.
        renderNoParens(parameter);
      }

      private boolean isUnknownTypeParameter(FunctionExpression functionExpression, int i) {
        Variable parameter = functionExpression.getParameters().get(i);

        TypeDescriptor parameterType = parameter.getTypeDescriptor();
        if (parameter == functionExpression.getJsVarargsParameter()) {
          parameterType = ((ArrayTypeDescriptor) parameterType).getComponentTypeDescriptor();
        }

        return parameterType.isTypeVariable()
            && ((TypeVariable) parameterType).isWildcardOrCapture();
      }

      @Override
      public boolean enterInstanceOfExpression(InstanceOfExpression expression) {
        processLeftSubExpression(expression, expression.getExpression());
        sourceBuilder.append(" instanceof ");
        // At this point only a declared type can appear as the type in the lhs of an instanceof.
        // Arrays, the only other type that can appear as a rhs of an instanceof, have been
        // normalized away.
        sourceBuilder.append(
            environment.aliasForType((DeclaredTypeDescriptor) expression.getTestTypeDescriptor()));
        return false;
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        // Conditional expressions are in its own precedence class. So when they are nested in the
        // in the condition position they need parenthesis, but not in the second or third position.
        processLeftSubExpression(
            conditionalExpression, conditionalExpression.getConditionExpression());
        sourceBuilder.append(" ? ");
        renderNoParens(conditionalExpression.getTrueExpression());
        sourceBuilder.append(" : ");
        renderNoParens(conditionalExpression.getFalseExpression());
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall expression) {
        if (expression.isStaticDispatch()) {
          renderStaticDispatchMethodCall(expression);
        } else if (expression.getTarget().isJsPropertyGetter()) {
          renderJsPropertyAccess(expression);
        } else if (expression.getTarget().isJsPropertySetter()) {
          renderJsPropertySetter(expression);
        } else {
          renderMethodCallHeader(expression);
          renderDelimitedAndCommaSeparated("(", ")", expression.getArguments());
        }
        return false;
      }

      private void renderStaticDispatchMethodCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String typeName = environment.aliasForType(methodDescriptor.getEnclosingTypeDescriptor());
        String qualifier = methodDescriptor.isStatic() ? typeName : typeName + ".prototype";

        sourceBuilder.append(
            AstUtils.buildQualifiedName(qualifier, methodDescriptor.getMangledName(), "call"));
        renderDelimitedAndCommaSeparated(
            "(",
            ")",
            Iterables.concat(
                Collections.singletonList(expression.getQualifier()), expression.getArguments()));
      }

      private void renderQualifiedName(Expression enclosingExpression, String jsPropertyName) {
        renderQualifiedName(enclosingExpression, jsPropertyName, SourcePosition.NONE);
      }

      private void renderQualifiedName(
          Expression enclosingExpression, String jsPropertyName, SourcePosition sourcePosition) {
        Expression qualifier = ((MemberReference) enclosingExpression).getQualifier();
        if (shouldRenderQualifier(qualifier)) {
          processLeftSubExpression(enclosingExpression, qualifier);
          sourceBuilder.append(".");
        }
        sourceBuilder.emitWithMapping(sourcePosition, () -> sourceBuilder.append(jsPropertyName));
      }

      @SuppressWarnings("ReferenceEquality")
      private boolean shouldRenderQualifier(Expression qualifier) {
        checkNotNull(qualifier);
        if (!(qualifier instanceof JavaScriptConstructorReference)) {
          return true;
        }

        // Static members in the global scope are explicitly qualified by a
        // JavaScriptConstructorReference node to the TypeDescriptor representing the global scope.
        JavaScriptConstructorReference constructorReference =
            (JavaScriptConstructorReference) qualifier;
        return constructorReference.getReferencedTypeDeclaration()
            != TypeDescriptors.get().globalNamespace.getTypeDeclaration();
      }

      /** JsProperty getter is emitted as property access: qualifier.property. */
      private void renderJsPropertyAccess(MethodCall expression) {
        renderQualifiedName(expression, expression.getTarget().getSimpleJsName());
      }

      /** JsProperty setter is emitted as property set: qualifier.property = argument. */
      private void renderJsPropertySetter(MethodCall expression) {
        renderJsPropertyAccess(expression);
        sourceBuilder.append(" = ");
        // Setters are a special case. They cannot be nested in any non top level expression as they
        // return void.
        renderNoParens(expression.getArguments().get(0));
      }

      private void renderMethodCallHeader(MethodCall expression) {
        checkArgument(!expression.isStaticDispatch());
        MethodDescriptor target = expression.getTarget();
        if (target.isConstructor()) {
          sourceBuilder.append("super");
        } else if (target.isJsFunction()) {
          // Call to a JsFunction method is emitted as the call on the qualifier itself.
          processLeftSubExpression(expression, expression.getQualifier());
        } else {
          renderQualifiedName(expression, target.getMangledName());
        }
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        List<Expression> expressions = multiExpression.getExpressions();
        if (expressions.stream()
            .anyMatch(Predicates.instanceOf(VariableDeclarationExpression.class))) {
          // If the multiexpression declares variables enclosing in an anonymous function
          // context.
          sourceBuilder.append("( () => {");
          for (Expression expression : expressions.subList(0, expressions.size() - 1)) {
            // Top level expression no need for parens.
            renderNoParens(expression);
            sourceBuilder.append(";");
          }
          Expression returnValue = Iterables.getLast(expressions);
          sourceBuilder.append(" return ");
          // Return values never need enclosing parens.
          renderNoParens(returnValue);
          sourceBuilder.append(";})()");
        } else {
          checkArgument(expressions.size() > 1);
          renderDelimitedAndCommaSeparated("(", ")", expressions);
        }
        return false;
      }

      @Override
      public boolean enterNewArray(NewArray newArrayExpression) {
        checkArgument(false, "NewArray should have been normalized.");
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance expression) {
        checkArgument(expression.getQualifier() == null);
        DeclaredTypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingTypeDescriptor().toRawTypeDescriptor();

        sourceBuilder.append("new " + environment.aliasForType(targetTypeDescriptor));
        renderDelimitedAndCommaSeparated("(", ")", expression.getArguments());
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral expression) {
        sourceBuilder.append("null");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral expression) {
        checkState(!TypeDescriptors.isPrimitiveLong(expression.getTypeDescriptor()));
        sourceBuilder.append(expression.toString());
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
      public boolean enterStringLiteral(StringLiteral expression) {
        sourceBuilder.append(expression.getEscapedValue());
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
          JavaScriptConstructorReference constructorReference) {
        sourceBuilder.append(
            environment.aliasForType(constructorReference.getReferencedTypeDeclaration()));
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        renderDelimitedAndCommaSeparated("let ", "", variableDeclarationExpression.getFragments());
        return false;
      }

      @Override
      public boolean enterVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        Variable variable = fragment.getVariable();
        if (fragment.needsTypeDeclaration()) {
          sourceBuilder.append(
              "/** " + environment.getClosureTypeString(variable.getTypeDescriptor()) + " */ ");
        }

        // Variable declarations are separated with comma, so no need for parens.
        renderNoParens(variable);

        if (fragment.getInitializer() != null) {
          sourceBuilder.append(" = ");
          renderNoParens(fragment.getInitializer());
        }
        return false;
      }

      @Override
      public boolean enterVariable(Variable variable) {
        sourceBuilder.emitWithMapping(
            // Only map variables if they are named.
            AstUtils.removeUnnamedSourcePosition(variable.getSourcePosition()),
            () -> sourceBuilder.append(environment.getUniqueNameForVariable(variable)));

        return false;
      }

      @Override
      public boolean enterVariableReference(VariableReference expression) {
        sourceBuilder.append(environment.getUniqueNameForVariable(expression.getTarget()));
        return false;
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
