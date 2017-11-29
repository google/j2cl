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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractTransformer;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.JsDocFieldDeclaration;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableReference;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
      public Void transformArrayLiteral(ArrayLiteral arrayLiteral) {
        renderDelimitedAndSeparated("[", ", ", "]", arrayLiteral.getValueExpressions());
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
                + "*/");
        process(declaration.getExpression());
        return null;
      }

      @Override
      public Void transformCharacterLiteral(CharacterLiteral characterLiteral) {
        sourceBuilder.append(
            Integer.toString(characterLiteral.getValue())
                + " /* "
                + characterLiteral.getEscapedValue()
                + " */");
        return null;
      }

      @Override
      public Void transformFieldAccess(FieldAccess fieldAccess) {

        String fieldMangledName = ManglingNameUtils.getMangledName(fieldAccess.getTarget());
        renderQualifiedName(fieldAccess.getQualifier(), fieldMangledName);
        return null;
      }

      private void emitVariableWithJsDocAnnotation(Variable variable) {
        sourceBuilder.append("/** ");
        sourceBuilder.append(
            closureTypesGenerator.getClosureTypeString(variable.getTypeDescriptor()));
        sourceBuilder.append(" */ ");
        process(variable);
      }

      private void emitFunctionHeaderJsDoc(FunctionExpression functionExpression) {
        sourceBuilder.append("/** ");
        for (int i = 0; i < functionExpression.getParameters().size(); i++) {
          sourceBuilder.append(closureTypesGenerator.getJsDocForParameter(functionExpression, i));
          sourceBuilder.append(" ");
        }
        sourceBuilder.append("*/ ");
      }

      private void emitParameters(List<Variable> parameters, Consumer<Variable> emitter) {
        sourceBuilder.append("(");
        String separator = "";
        for (Variable parameter : parameters) {
          sourceBuilder.append(separator);
          emitter.accept(parameter);
          separator = ", ";
        }
        sourceBuilder.append(")");
      }

      @Override
      public Void transformFunctionExpression(FunctionExpression expression) {
        sourceBuilder.append("(");

        if (expression.getDescriptor().isJsAsync()) {
          sourceBuilder.append("async ");
        }

        ImmutableList<ParameterDescriptor> parameterDescriptors =
            expression.getDescriptor().getParameterDescriptors();
        if (parameterDescriptors
            .stream()
            .anyMatch(
                Predicates.or(ParameterDescriptor::isJsOptional, ParameterDescriptor::isVarargs))) {
          // TODO(b/36818468, b/36855486): Emit a full method header since optional or varargs
          // are not properly supported as inline annotations.
          emitFunctionHeaderJsDoc(expression);
          emitParameters(
              expression.getParameters(),
              parameter -> {
                if (parameter == expression.getJsVarargsParameter()) {
                  sourceBuilder.append("...");
                }
                process(parameter);
              });
        } else {
          // Otherwise emit the more readable inline short form.
          emitParameters(expression.getParameters(), this::emitVariableWithJsDocAnnotation);
        }

        // After the header is emitted, emit the rest of the arrow function.
        sourceBuilder.append(" =>");
        new StatementTranspiler(sourceBuilder, environment).renderStatement(expression.getBody());
        sourceBuilder.append(")");
        return null;
      }

      @Override
      public Void transformInstanceOfExpression(InstanceOfExpression expression) {
        checkArgument(false, "InstanceOf expression should have been normalized.");
        return null;
      }

      private void renderLongNumberLiteral(NumberLiteral expression) {
        long longValue = expression.getValue().longValue();
        long lowOrderBits = longValue << 32 >> 32;
        long highOrderBits = longValue >> 32;
        sourceBuilder.append(
            environment.aliasForType(BootstrapType.NATIVE_LONG.getDescriptor())
                + ".fromBits("
                + lowOrderBits
                + ", "
                + highOrderBits
                + ") /* "
                + longValue
                + " */");
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
            qualifier + "." + ManglingNameUtils.getMangledName(methodDescriptor) + ".call");
        renderDelimitedAndSeparated(
            "(",
            ", ",
            ")",
            Iterables.concat(
                Collections.singletonList(expression.getQualifier()), expression.getArguments()));
      }

      private void renderQualifiedName(Expression qualifier, String jsPropertyName) {
        if (shouldRenderQualifier(qualifier)) {
          process(qualifier);
          sourceBuilder.append(".");
        }
        sourceBuilder.append(jsPropertyName);
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
          process(expression.getQualifier());
        } else {
          renderQualifiedName(expression.getQualifier(), ManglingNameUtils.getMangledName(target));
        }
      }

      @Override
      public Void transformMultiExpression(MultiExpression multiExpression) {
        List<Expression> expressions = multiExpression.getExpressions();
        if (expressions
            .stream()
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
        TypeDescriptor targetTypeDescriptor =
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
        if (TypeDescriptors.isPrimitiveLong(expression.getTypeDescriptor())) {
          renderLongNumberLiteral(expression);
        } else {
          sourceBuilder.append(expression.getValue().toString());
        }
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
        // The + prefix operator is a NOP.
        if (expression.getOperator() == PrefixOperator.PLUS) {
          process(expression.getOperand());
          return null;
        }
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
          emitVariableWithJsDocAnnotation(variable);
        } else {
          process(variable);
        }
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
            () -> sourceBuilder.append(environment.aliasForVariable(variable)));

        return null;
      }

      @Override
      public Void transformVariableReference(VariableReference expression) {
        sourceBuilder.append(environment.aliasForVariable(expression.getTarget()));
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
