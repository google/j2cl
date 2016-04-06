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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractTransformer;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
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
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transforms Expression to JavaScript source strings.
 */
public class ExpressionTranspiler {
  private static String stringForMethodDescriptor(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isConstructor()) {
      return ManglingNameUtils.getCtorMangledName(methodDescriptor);
    } else if (methodDescriptor.isInit()) {
      return ManglingNameUtils.getInitMangledName(
          methodDescriptor.getEnclosingClassTypeDescriptor());
    } else {
      return ManglingNameUtils.getMangledName(methodDescriptor);
    }
  }

  public static String transform(Expression expression, final GenerationEnvironment environment) {
    class ToSourceTransformer extends AbstractTransformer<String> {
      private String annotateWithJsDoc(TypeDescriptor castTypeDescriptor, String expression) {
        String jsdoc = JsDocNameUtils.getJsDocName(castTypeDescriptor, false, environment);
        return String.format("/**@type {%s} */ (%s)", jsdoc, expression);
      }

      private String arraysTypeAlias() {
        return environment.aliasForType(BootstrapType.ARRAYS.getDescriptor());
      }

      private String longsTypeAlias() {
        return environment.aliasForType(BootstrapType.LONGS.getDescriptor());
      }

      @Override
      public String transformArrayAccess(ArrayAccess arrayAccess) {
        return String.format(
            "%s[%s]",
            transform(arrayAccess.getArrayExpression(), environment),
            transform(arrayAccess.getIndexExpression(), environment));
      }

      // TODO: extend to handle long[].
      private String transformArrayAssignmentBinaryExpression(BinaryExpression expression) {
        Preconditions.checkState(
            expression.getOperator().hasSideEffect()
                && expression.getLeftOperand() instanceof ArrayAccess);

        ArrayAccess arrayAccess = (ArrayAccess) expression.getLeftOperand();
        return String.format(
            "%s.%s(%s, %s, %s)",
            arraysTypeAlias(),
            GeneratorUtils.getArrayAssignmentFunctionName(expression.getOperator()),
            transform(arrayAccess.getArrayExpression(), environment),
            transform(arrayAccess.getIndexExpression(), environment),
            transform(expression.getRightOperand(), environment));
      }

      @Override
      public String transformArrayLiteral(ArrayLiteral arrayLiteral) {
        String valuesAsString =
            Joiner.on(", ").join(transformNodesToSource(arrayLiteral.getValueExpressions()));
        return "[ " + valuesAsString + " ]";
      }

      @Override
      public String transformBinaryExpression(BinaryExpression expression) {
        Expression leftOperand = expression.getLeftOperand();
        BinaryOperator operator = expression.getOperator();

        if (operator.hasSideEffect() && leftOperand instanceof ArrayAccess) {
          return transformArrayAssignmentBinaryExpression(expression);
        } else {
          Preconditions.checkState(
              !(expression.getOperator().hasSideEffect()
                  && expression.getLeftOperand() instanceof ArrayAccess));

          return String.format(
              "%s %s %s",
              transform(expression.getLeftOperand(), environment),
              expression.getOperator().toString(),
              transform(expression.getRightOperand(), environment));
        }
      }

      @Override
      public String transformBooleanLiteral(BooleanLiteral expression) {
        return expression.getValue() ? "true" : "false";
      }

      @Override
      public String transformCastExpression(CastExpression castExpression) {
        Preconditions.checkArgument(
            castExpression.isRaw(),
            "Java CastExpression should have been normalized to method call.");
        return annotateWithJsDoc(
            castExpression.getCastTypeDescriptor(),
            transform(castExpression.getExpression(), environment));
      }

      @Override
      public String transformCharacterLiteral(CharacterLiteral characterLiteral) {
        return String.format(
            "%s /* %s */",
            Integer.toString(characterLiteral.getValue()),
            characterLiteral.getEscapedValue());
      }

      @Override
      public String transformFieldAccess(FieldAccess fieldAccess) {
        // When inside the same class, access static fields directly.
        boolean insideSameEnclosingClass =
            fieldAccess.getTarget().isStatic()
                && fieldAccess
                    .getTarget()
                    .getEnclosingClassTypeDescriptor()
                    .equals(environment.getEnclosingTypeDescriptor());
        // No private backing field for compile time constants.
        boolean accessBackingPrivateField =
            !fieldAccess.getTarget().isCompileTimeConstant() && insideSameEnclosingClass;

        String fieldMangledName =
            ManglingNameUtils.getMangledName(fieldAccess.getTarget(), accessBackingPrivateField);
        String qualifier = transform(fieldAccess.getQualifier(), environment);
        return String.format("%s.%s", qualifier, fieldMangledName);
      }

      @Override
      public String transformInstanceOfExpression(InstanceOfExpression expression) {
        Preconditions.checkArgument(false, "InstanceOf expression should have been normalized.");
        return "";
      }

      private String transformLongNumberLiteral(NumberLiteral expression) {
        long longValue = expression.getValue().longValue();

        if (longValue < Integer.MAX_VALUE && longValue > Integer.MIN_VALUE) {
          // The long value is small enough to fit in an int. Emit the terse initialization.
          return String.format("%s.$fromInt(%s)", longsTypeAlias(), Long.toString(longValue));
        }

        // The long value is pretty large. Emit the verbose initialization.
        long lowOrderBits = longValue << 32 >> 32;
        long highOrderBits = longValue >> 32;
        return String.format(
            "%s.$fromBits(%s, %s) /* %s */",
            longsTypeAlias(),
            Long.toString(lowOrderBits),
            Long.toString(highOrderBits),
            Long.toString(longValue));
      }

      @Override
      public String transformMethodCall(MethodCall expression) {
        switch (expression.getCallStyle()) {
          case CALL:
            return transformCallMethodCall(expression);
          default:
            return transformDirectMethodCall(expression);
        }
      }

      private String transformCallMethodCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        List<String> argumentSources = transformNodesToSource(expression.getArguments());
        String typeName =
            environment.aliasForType(methodDescriptor.getEnclosingClassTypeDescriptor());
        String qualifier = methodDescriptor.isStatic() ? typeName : typeName + ".prototype";
        return String.format(
            "%s.%s.call(%s)",
            qualifier,
            ExpressionTranspiler.stringForMethodDescriptor(methodDescriptor),
            Joiner.on(", ")
                .join(
                    Iterables.concat(
                        Arrays.asList(transform(expression.getQualifier(), environment)),
                        argumentSources)));
      }

      @Override
      public String transformConditionalExpression(ConditionalExpression conditionalExpression) {
        String conditionExpressionAsString =
            transform(conditionalExpression.getConditionExpression(), environment);
        String trueExpressionAsString =
            transform(conditionalExpression.getTrueExpression(), environment);
        String falseExpressionAsString =
            transform(conditionalExpression.getFalseExpression(), environment);
        return String.format(
            "%s ? %s : %s",
            conditionExpressionAsString,
            trueExpressionAsString,
            falseExpressionAsString);
      }

      private String transformDirectMethodCall(MethodCall expression) {
        if (expression.getTarget().isJsProperty()) {
          return transformJsPropertyCall(expression);
        } else {
          List<String> argumentSources = transformNodesToSource(expression.getArguments());
          return String.format(
              "%s(%s)",
              transformMethodCallHeader(expression),
              Joiner.on(", ").join(argumentSources));
        }
      }

      private String transformJsPropertyCall(MethodCall expression) {
        if (expression.getTarget().isJsPropertyGetter()) {
          return transformJsPropertyGetter(expression);
        } else {
          return transformJsPropertySetter(expression);
        }
      }

      /**
       * JsProperty getter is emitted as property access: qualifier.property.
       */
      private String transformJsPropertyGetter(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = transform(expression.getQualifier(), environment);
        String propertyName = methodDescriptor.getJsPropertyName();
        return Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), propertyName);
      }

      /**
       * JsProperty setter is emitted as property set: qualifier.property = argument.
       */
      private String transformJsPropertySetter(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = transform(expression.getQualifier(), environment);
        String propertyName = methodDescriptor.getJsPropertyName();
        Preconditions.checkArgument(expression.getArguments().size() == 1);
        return String.format(
            "%s = %s",
            Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), propertyName),
            transform(expression.getArguments().get(0), environment));
      }

      private String transformMethodCallHeader(MethodCall expression) {
        MethodDescriptor target = expression.getTarget();
        String qualifier = transform(expression.getQualifier(), environment);
        if (target.isJsFunction()) {
          // Call to a JsFunction method is emitted as the call on the qualifier itself:
          return String.format("%s", qualifier);
        } else if (expression.isStaticDispatch()) {
          String typeName = environment.aliasForType(target.getEnclosingClassTypeDescriptor());
          String methodName = ExpressionTranspiler.stringForMethodDescriptor(target);
          return typeName + ".prototype." + methodName;
        } else {
          String methodName = ExpressionTranspiler.stringForMethodDescriptor(target);
          return Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), methodName);
        }
      }

      @Override
      public String transformMultiExpression(MultiExpression multipleExpression) {
        String expressionsAsString =
            Joiner.on(", ").join(transformNodesToSource(multipleExpression.getExpressions()));
        return "(" + expressionsAsString + ")";
      }

      @Override
      public String transformNewArray(NewArray newArrayExpression) {
        Preconditions.checkArgument(false, "NewArray should have been normalized.");
        return "";
      }

      @Override
      public String transformNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor().getRawTypeDescriptor();
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format(
            "new %s(%s)", environment.aliasForType(targetTypeDescriptor), argumentsList);
      }

      public List<String> transformNodesToSource(List<Expression> nodes) {
        return Lists.transform(
            nodes,
            new Function<Expression, String>() {
              @Override
              public String apply(Expression node) {
                return transform(node, environment);
              }
            });
      }

      @Override
      public String transformNullLiteral(NullLiteral expression) {
        return "null";
      }

      @Override
      public String transformNumberLiteral(NumberLiteral expression) {
        if (TypeDescriptors.get().primitiveLong == expression.getTypeDescriptor()) {
          return transformLongNumberLiteral(expression);
        }
        return expression.getValue().toString();
      }

      @Override
      public String transformPostfixExpression(PostfixExpression expression) {
        Preconditions.checkArgument(
            TypeDescriptors.get().primitiveLong != expression.getTypeDescriptor());
        return String.format(
            "%s%s",
            transform(expression.getOperand(), environment),
            expression.getOperator().toString());
      }

      @Override
      public String transformPrefixExpression(PrefixExpression expression) {
        // The + prefix operator is a NOP.
        if (expression.getOperator() == PrefixOperator.PLUS) {
          return transform(expression.getOperand(), environment);
        }

        return String.format(
            "%s%s",
            expression.getOperator().toString(),
            transform(expression.getOperand(), environment));
      }

      @Override
      public String transformStringLiteral(StringLiteral expression) {
        return expression.getEscapedValue();
      }

      @Override
      public String transformSuperReference(SuperReference expression) {
        return "super";
      }

      @Override
      public String transformThisReference(ThisReference expression) {
        return "this";
      }

      @Override
      public String transformTypeReference(TypeReference typeReference) {
        return environment.aliasForType(typeReference.getReferencedTypeDescriptor());
      }

      @Override
      public String transformVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        List<String> fragmentsAsString = new ArrayList<>();
        for (VariableDeclarationFragment fragment : variableDeclarationExpression.getFragments()) {
          fragmentsAsString.add(transform(fragment, environment));
        }
        return "let " + Joiner.on(", ").join(fragmentsAsString);
      }

      @Override
      public String transformVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        Variable variable = fragment.getVariable();
        String variableAsString = environment.aliasForVariable(variable);
        if (fragment.getInitializer() == null) {
          return variableAsString;
        }

        String initializerAsString = transform(fragment.getInitializer(), environment);
        return String.format("%s = %s", variableAsString, initializerAsString);
      }

      @Override
      public String transformVariableReference(VariableReference expression) {
        return environment.aliasForVariable(expression.getTarget());
      }
    }
    if (expression == null) {
      return "";
    }
    return new ToSourceTransformer().process(expression);
  }
}
