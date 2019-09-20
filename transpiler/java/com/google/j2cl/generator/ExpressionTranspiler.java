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
import com.google.j2cl.ast.AbstractTransformer;
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
import com.google.j2cl.ast.JsDocFieldDeclaration;
import com.google.j2cl.ast.ManglingNameUtils;
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
import java.util.Optional;

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
    new AbstractTransformer<Void>() {
      ClosureTypesGenerator closureTypesGenerator = new ClosureTypesGenerator(environment);

      @Override
      public Void transformArrayAccess(ArrayAccess arrayAccess) {
        process(arrayAccess.getArrayExpression());
        sourceBuilder.append("[");
        process(arrayAccess.getIndexExpression());
        sourceBuilder.append("]");
        return null;
      }

      @Override
      public Void transformArrayLength(ArrayLength arrayLength) {
        process(arrayLength.getArrayExpression());
        sourceBuilder.append(".length");
        return null;
      }

      @Override
      public Void transformArrayLiteral(ArrayLiteral arrayLiteral) {
        renderDelimitedAndSeparated("[", ", ", "]", arrayLiteral.getValueExpressions());
        return null;
      }

      @Override
      public Void transformAwaitExpression(AwaitExpression awaitExpression) {
        sourceBuilder.append("(await ");
        process(awaitExpression.getExpression());
        sourceBuilder.append(")");
        return null;
      }

      @Override
      public Void transformBinaryExpression(BinaryExpression expression) {
        process(expression.getLeftOperand());
        sourceBuilder.append(" " + expression.getOperator() + " ");
        process(expression.getRightOperand());
        return null;
      }

      @Override
      public Void transformBooleanLiteral(BooleanLiteral expression) {
        sourceBuilder.append(expression.getValue() ? "true" : "false");
        return null;
      }

      @Override
      public Void transformCastExpression(CastExpression castExpression) {
        checkArgument(
            false, castExpression + " CastExpression should have been normalized to method call.");
        return null;
      }

      @Override
      public Void transformJsDocCastExpression(JsDocCastExpression jsDocCastExpression) {
        String jsdoc =
            closureTypesGenerator.getClosureTypeString(jsDocCastExpression.getTypeDescriptor());
        sourceBuilder.append("/**@type {" + jsdoc + "} */ (");
        process(jsDocCastExpression.getExpression());
        sourceBuilder.append(")");
        return null;
      }

      @Override
      public Void transformJsDocFieldDeclaration(JsDocFieldDeclaration declaration) {
        String jsdoc = closureTypesGenerator.getClosureTypeString(declaration.getTypeDescriptor());
        sourceBuilder.appendln(
            "/** "
                + (declaration.isPublic() ? "@public" : "@private")
                + " {"
                + jsdoc
                + "} "
                + (declaration.isConst() ? "@const " : "")
                + (declaration.isDeprecated() ? "@deprecated " : "")
                + "*/");
        process(declaration.getExpression());
        return null;
      }

      @Override
      public Void transformExpressionWithComment(ExpressionWithComment expressionWithComment) {
        process(expressionWithComment.getExpression());
        sourceBuilder.append(" /* " + expressionWithComment.getComment() + " */");
        return null;
      }

      @Override
      public Void transformFieldAccess(FieldAccess fieldAccess) {
        String fieldMangledName = ManglingNameUtils.getMangledName(fieldAccess.getTarget());
        renderQualifiedName(
            fieldAccess.getQualifier(), fieldMangledName, fieldAccess.getSourcePosition());
        return null;
      }

      @Override
      public Void transformFunctionExpression(FunctionExpression expression) {
        if (expression.getDescriptor().isJsAsync()) {
          sourceBuilder.append("async ");
        }

        emitParameters(expression);

        // After the header is emitted, emit the rest of the arrow function.
        sourceBuilder.append(" =>");
        new StatementTranspiler(sourceBuilder, environment).renderStatement(expression.getBody());

        return null;
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
          // The inline type annotation for parameters has to be just right preceding the parameter
          // name, hence if it is a varargs parameter then it would be emitted as follows:
          // ... /* <inline type annotation> */ <parameter name>
          //
          sourceBuilder.append(
              "/** " + closureTypesGenerator.getJsDocForParameter(expression, i) + " */ ");
        }
        process(parameter);
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
      public Void transformInstanceOfExpression(InstanceOfExpression expression) {
        checkArgument(false, "InstanceOf expression should have been normalized.");
        return null;
      }

      @Override
      public Void transformConditionalExpression(ConditionalExpression conditionalExpression) {
        process(conditionalExpression.getConditionExpression());
        sourceBuilder.append(" ? ");
        process(conditionalExpression.getTrueExpression());
        sourceBuilder.append(" : ");
        process(conditionalExpression.getFalseExpression());
        return null;
      }

      @Override
      public Void transformMethodCall(MethodCall expression) {
        if (expression.isStaticDispatch()) {
          renderStaticDispatchMethodCall(expression);
        } else if (expression.getTarget().isJsPropertyGetter()) {
          renderJsPropertyAccess(expression);
        } else if (expression.getTarget().isJsPropertySetter()) {
          renderJsPropertySetter(expression);
        } else {
          renderMethodCallHeader(expression);
          renderDelimitedAndSeparated("(", ", ", ")", expression.getArguments());
        }
        return null;
      }

      private void renderStaticDispatchMethodCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String typeName = environment.aliasForType(methodDescriptor.getEnclosingTypeDescriptor());
        String qualifier = methodDescriptor.isStatic() ? typeName : typeName + ".prototype";

        sourceBuilder.append(
            AstUtils.buildQualifiedName(
                qualifier, ManglingNameUtils.getMangledName(methodDescriptor), "call"));
        renderDelimitedAndSeparated(
            "(",
            ", ",
            ")",
            Iterables.concat(
                Collections.singletonList(expression.getQualifier()), expression.getArguments()));
      }

      private void renderQualifiedName(Expression qualifier, String jsPropertyName) {
        renderQualifiedName(qualifier, jsPropertyName, /* sourcePosition */ Optional.empty());
      }

      private void renderQualifiedName(
          Expression qualifier, String jsPropertyName, Optional<SourcePosition> sourcePosition) {
        if (shouldRenderQualifier(qualifier)) {
          process(qualifier);
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
        renderQualifiedName(expression.getQualifier(), expression.getTarget().getSimpleJsName());
      }

      /** JsProperty setter is emitted as property set: qualifier.property = argument. */
      private String renderJsPropertySetter(MethodCall expression) {
        renderJsPropertyAccess(expression);
        sourceBuilder.append(" = ");
        process(expression.getArguments().get(0));
        return null;
      }

      private void renderMethodCallHeader(MethodCall expression) {
        checkArgument(!expression.isStaticDispatch());
        MethodDescriptor target = expression.getTarget();
        if (target.isConstructor()) {
          sourceBuilder.append("super");
        } else if (target.isJsFunction()) {
          // Call to a JsFunction method is emitted as the call on the qualifier itself:
          process(expression.getQualifier().parenthesize());
        } else {
          renderQualifiedName(expression.getQualifier(), ManglingNameUtils.getMangledName(target));
        }
      }

      @Override
      public Void transformMultiExpression(MultiExpression multiExpression) {
        List<Expression> expressions = multiExpression.getExpressions();
        if (expressions.stream()
            .anyMatch(Predicates.instanceOf(VariableDeclarationExpression.class))) {
          // If the multiexpression declares variables enclosing in an anonymous function context.
          sourceBuilder.append("( () => {");
          for (Expression expression : expressions.subList(0, expressions.size() - 1)) {
            process(expression);
            sourceBuilder.append(";");
          }
          Expression returnValue = Iterables.getLast(expressions);
          sourceBuilder.append(" return ");
          process(returnValue);
          sourceBuilder.append(";})()");
        } else {
          renderDelimitedAndSeparated("(", ", ", ")", expressions);
        }
        return null;
      }

      @Override
      public Void transformNewArray(NewArray newArrayExpression) {
        checkArgument(false, "NewArray should have been normalized.");
        return null;
      }

      @Override
      public Void transformNewInstance(NewInstance expression) {
        checkArgument(expression.getQualifier() == null);
        DeclaredTypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingTypeDescriptor().toRawTypeDescriptor();

        sourceBuilder.append("new " + environment.aliasForType(targetTypeDescriptor));
        renderDelimitedAndSeparated("(", ", ", ")", expression.getArguments());
        return null;
      }

      @Override
      public Void transformNullLiteral(NullLiteral expression) {
        sourceBuilder.append("null");
        return null;
      }

      @Override
      public Void transformNumberLiteral(NumberLiteral expression) {
        checkState(!TypeDescriptors.isPrimitiveLong(expression.getTypeDescriptor()));
        sourceBuilder.append(expression.getValue().toString());
        return null;
      }

      @Override
      public Void transformPostfixExpression(PostfixExpression expression) {
        checkArgument(!TypeDescriptors.isPrimitiveLong(expression.getTypeDescriptor()));
        process(expression.getOperand());
        sourceBuilder.append(expression.getOperator().toString());
        return null;
      }

      @Override
      public Void transformPrefixExpression(PrefixExpression expression) {
        sourceBuilder.append(expression.getOperator().toString());
        process(expression.getOperand());
        return null;
      }

      @Override
      public Void transformStringLiteral(StringLiteral expression) {
        sourceBuilder.append(expression.getEscapedValue());
        return null;
      }

      @Override
      public Void transformSuperReference(SuperReference expression) {
        sourceBuilder.append("super");
        return null;
      }

      @Override
      public Void transformThisReference(ThisReference expression) {
        sourceBuilder.append("this");
        return null;
      }

      @Override
      public Void transformJavaScriptConstructorReference(
          JavaScriptConstructorReference constructorReference) {
        sourceBuilder.append(
            environment.aliasForType(constructorReference.getReferencedTypeDeclaration()));
        return null;
      }

      @Override
      public Void transformVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        renderDelimitedAndSeparated("let ", ", ", "", variableDeclarationExpression.getFragments());
        return null;
      }

      @Override
      public Void transformVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        Variable variable = fragment.getVariable();
        if (fragment.needsTypeDeclaration()) {
          sourceBuilder.append(
              "/** "
                  + closureTypesGenerator.getClosureTypeString(variable.getTypeDescriptor())
                  + " */ ");
        }

        process(variable);

        if (fragment.getInitializer() != null) {
          sourceBuilder.append(" = ");
          process(fragment.getInitializer());
        }
        return null;
      }

      @Override
      public Void transformVariable(Variable variable) {
        sourceBuilder.emitWithMapping(
            // Only map variables if they are named.
            AstUtils.emptySourcePositionIfNotNamed(variable.getSourcePosition()),
            () -> sourceBuilder.append(environment.getUniqueNameForVariable(variable)));

        return null;
      }

      @Override
      public Void transformVariableReference(VariableReference expression) {
        sourceBuilder.append(environment.getUniqueNameForVariable(expression.getTarget()));
        return null;
      }

      private void renderDelimitedAndSeparated(
          String prefix, String separator, String suffix, Iterable<? extends Node> nodes) {
        sourceBuilder.append(prefix);
        renderSeparated(separator, nodes);
        sourceBuilder.append(suffix);
      }

      private void renderSeparated(String separator, Iterable<? extends Node> nodes) {
        String currentSeparator = "";
        for (Node node : nodes) {
          if (node == null) {
            continue;
          }
          sourceBuilder.append(currentSeparator);
          currentSeparator = separator;
          process(node);
        }
      }
    }.process(expression);
  }
}
