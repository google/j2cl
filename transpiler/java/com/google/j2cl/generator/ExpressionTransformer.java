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
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
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
public class ExpressionTransformer {
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
        String jsdoc = JsDocNameUtils.getJsDocName(castTypeDescriptor, environment);
        return String.format("/**@type {%s} */ (%s)", jsdoc, expression);
      }

      private String arraysTypeAlias() {
        return transform(BootstrapType.ARRAYS.getDescriptor(), environment);
      }

      private String longsTypeAlias() {
        return transform(BootstrapType.LONGS.getDescriptor(), environment);
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
            AstUtils.isAssignmentOperator(expression.getOperator())
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

      private String transformArrayCreate(NewArray newArrayExpression) {
        Preconditions.checkArgument(newArrayExpression.getArrayLiteral() == null);

        String dimensionsList =
            Joiner.on(", ")
                .join(transformNodesToSource(newArrayExpression.getDimensionExpressions()));
        return annotateWithJsDoc(
            newArrayExpression.getTypeDescriptor(),
            String.format(
                "%s.$create([%s], %s)",
                arraysTypeAlias(),
                dimensionsList,
                environment.aliasForType(newArrayExpression.getLeafTypeDescriptor())));
      }

      private String transformArrayInit(NewArray newArrayExpression) {
        Preconditions.checkArgument(newArrayExpression.getArrayLiteral() != null);

        String leafTypeName = environment.aliasForType(newArrayExpression.getLeafTypeDescriptor());
        int dimensionCount = newArrayExpression.getDimensionExpressions().size();
        String arrayLiteralAsString = transform(newArrayExpression.getArrayLiteral(), environment);

        if (dimensionCount == 1) {
          // It's 1 dimensional.
          if (TypeDescriptors.get().javaLangObject == newArrayExpression.getLeafTypeDescriptor()) {
            // And the leaf type is Object. All arrays are implicitly Array<Object> so leave out the
            // init.
            return arrayLiteralAsString;
          }
          // Number of dimensions defaults to 1 so we can leave that parameter out.
          return annotateWithJsDoc(
              newArrayExpression.getTypeDescriptor(),
              String.format(
                  "%s.$init(%s, %s)", arraysTypeAlias(), arrayLiteralAsString, leafTypeName));
        } else {
          // It's multidimensional, make dimensions explicit.
          return annotateWithJsDoc(
              newArrayExpression.getTypeDescriptor(),
              String.format(
                  "%s.$init(%s, %s, %s)",
                  arraysTypeAlias(),
                  arrayLiteralAsString,
                  leafTypeName,
                  dimensionCount));
        }
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

        if (AstUtils.isAssignmentOperator(operator) && leftOperand instanceof ArrayAccess) {
          return transformArrayAssignmentBinaryExpression(expression);
        } else {
          Preconditions.checkState(
              !(AstUtils.isAssignmentOperator(expression.getOperator())
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
      public String transformCastExpression(CastExpression expression) {
        Preconditions.checkArgument(
            expression.isRaw(), "Java CastExpression should have been normalized to method call.");
        return annotateWithJsDoc(
            expression.getCastTypeDescriptor(), transform(expression.getExpression(), environment));
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
        String fieldMangledName = ManglingNameUtils.getMangledName(fieldAccess.getTarget());
        String qualifier = transform(fieldAccess.getQualifier(), environment);
        return String.format("%s.%s", qualifier, fieldMangledName);
      }

      @Override
      public String transformInstanceOfExpression(InstanceOfExpression expression) {
        Preconditions.checkArgument(false, "InstanceOf expression should have been normalized.");
        return "";
      }

      /**
       * Call to a JsFunction method is emitted as the call on the qualifier itself:
       * a.fun(); => a();
       */
      private String transformJsFunctionCall(MethodCall expression) {
        return String.format(
            "(/** @type {Function} */(%s))(%s)",
            transform(expression.getQualifier(), environment),
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments())));
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
        if (expression.isPrototypeCall()) {
          return transformPrototypeCall(expression);
        } else if (expression.getTarget().isJsProperty()) {
          return transformJsPropertyCall(expression);
        } else if (expression.getTarget().isJsFunction()) {
          return transformJsFunctionCall(expression);
        } else {
          return transformRegularMethodCall(expression);
        }
      }

      @Override
      public String transformMultiExpression(MultiExpression multipleExpression) {
        String expressionsAsString =
            Joiner.on(", ").join(transformNodesToSource(multipleExpression.getExpressions()));
        return "( " + expressionsAsString + " )";
      }

      private String transformNativeNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor();
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format(
            "new %s(%s)", transform(targetTypeDescriptor, environment), argumentsList);
      }

      @Override
      public String transformNewArray(NewArray newArrayExpression) {
        if (newArrayExpression.getArrayLiteral() != null) {
          return transformArrayInit(newArrayExpression);
        }
        return transformArrayCreate(newArrayExpression);
      }

      @Override
      public String transformNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor();
        if (targetTypeDescriptor.isNative()) {
          return transformNativeNewInstance(expression);
        } else if (targetTypeDescriptor.isJsFunctionImplementation()) {
          return transfromJsFunctionNewInstance(expression);
        } else {
          return transformRegularNewInstance(expression);
        }
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
      public String transformParenthesizedExpression(ParenthesizedExpression expression) {
        return String.format("(%s)", transform(expression.getExpression(), environment));
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

      private String transformPrototypeCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        List<String> argumentSources = transformNodesToSource(expression.getArguments());
        return String.format(
            "%s.prototype.%s.call(%s)",
            transform(methodDescriptor.getEnclosingClassTypeDescriptor(), environment),
            ExpressionTransformer.stringForMethodDescriptor(methodDescriptor),
            Joiner.on(", ")
                .join(
                    Iterables.concat(
                        Arrays.asList(transform(expression.getQualifier(), environment)),
                        argumentSources)));
      }

      private String transformRegularMethodCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = transform(expression.getQualifier(), environment);
        String methodName = ExpressionTransformer.stringForMethodDescriptor(methodDescriptor);
        List<String> argumentSources = transformNodesToSource(expression.getArguments());
        return String.format(
            "%s(%s)",
            Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), methodName),
            Joiner.on(", ").join(argumentSources));
      }

      private String transformRegularNewInstance(NewInstance expression) {
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        String className =
            environment.aliasForType(expression.getTarget().getEnclosingClassTypeDescriptor());
        String constructorMangledName =
            ManglingNameUtils.getConstructorMangledName(expression.getTarget());
        return String.format("%s.%s(%s)", className, constructorMangledName, argumentsList);
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
      public String transformTernaryExpression(TernaryExpression ternaryExpression) {
        String conditionExpressionAsString =
            transform(ternaryExpression.getConditionExpression(), environment);
        String trueExpressionAsString =
            transform(ternaryExpression.getTrueExpression(), environment);
        String falseExpressionAsString =
            transform(ternaryExpression.getFalseExpression(), environment);
        return String.format(
            "%s ? %s : %s",
            conditionExpressionAsString,
            trueExpressionAsString,
            falseExpressionAsString);
      }

      @Override
      public String transformThisReference(ThisReference expression) {
        return "this";
      }

      @Override
      public String transformTypeDescriptor(TypeDescriptor typeDescriptor) {
        return environment.aliasForType(typeDescriptor);
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

      /**
       * We transform:
       *
       * new A() // A implements a JsFunction interface.
       *
       * to:
       *
       * Util.$makeLambdaFunction(A.prototype.fun, new A(), A.$copy).
       *
       * TODO: translate anonymous class and lambda to light function. (Real JS function, without
       * generating any class literals).
       */
      private String transfromJsFunctionNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor();
        String enclosingClassName = transform(targetTypeDescriptor, environment);
        return String.format(
            "%s.$makeLambdaFunction(%s.prototype.%s, %s, %s.prototype.$copy)",
            transform(TypeDescriptors.BootstrapType.NATIVE_UTIL.getDescriptor(), environment),
            enclosingClassName,
            ManglingNameUtils.getMangledName(targetTypeDescriptor.getJsFunctionMethodDescriptor()),
            transformRegularNewInstance(expression),
            enclosingClassName);
      }
    }
    if (expression == null) {
      return "";
    }
    return new ToSourceTransformer().process(expression);
  }
}
