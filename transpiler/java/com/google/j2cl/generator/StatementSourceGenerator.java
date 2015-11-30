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
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.BreakStatement;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CharacterLiteral;
import com.google.j2cl.ast.ContinueStatement;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableDeclarationStatement;
import com.google.j2cl.ast.VariableReference;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.generator.visitors.Import;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate javascript source code for {@code Statement}.
 * TODO: Keep an eye on performance and if things get slow then replace these String operations with
 * a StringBuilder.
 */
public class StatementSourceGenerator {
  private Map<TypeDescriptor, String> aliasByTypeDescriptor = new HashMap<>();
  private final Map<Variable, String> aliasByVariable;

  public StatementSourceGenerator(
      Collection<Import> imports, Map<Variable, String> aliasByVariable) {
    JsDocNameUtils.init();
    for (Import anImport : imports) {
      aliasByTypeDescriptor.put(anImport.getTypeDescriptor(), anImport.getAlias());
    }
    this.aliasByVariable = aliasByVariable;
  }

  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, this);
  }

  public String getJsDocName(TypeDescriptor typeDescriptor, boolean shouldUseClassName) {
    return JsDocNameUtils.getJsDocName(typeDescriptor, shouldUseClassName, this);
  }

  public String getJsDocNames(List<TypeDescriptor> typeDescriptors) {
    return JsDocNameUtils.getJsDocNames(typeDescriptors, this);
  }

  public String toSource(Node node) {
    return toSource(node, null);
  }

  public String toSource(Node node, final TypeDescriptor inClinitForTypeDescriptor) {
    class ToSourceTransformer extends AbstractTransformer<String> {

      String toSource(Node node) {
        return StatementSourceGenerator.this.toSource(node, inClinitForTypeDescriptor);
      }

      @Override
      public String transformAssertStatement(AssertStatement statement) {
        if (statement.getMessage() == null) {
          return String.format(
              "%s.$enabled() && %s.$assert(%s);",
              assertsTypeAlias(),
              assertsTypeAlias(),
              toSource(statement.getExpression()));
        } else {
          return String.format(
              "%s.$enabled() && %s.$assertWithMessage(%s, %s);",
              assertsTypeAlias(),
              assertsTypeAlias(),
              toSource(statement.getExpression()),
              toSource(statement.getMessage()));
        }
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
              toSource(expression.getLeftOperand()),
              expression.getOperator().toString(),
              toSource(expression.getRightOperand()));
        }
      }

      @Override
      public String transformLabeledStatement(LabeledStatement statement) {
        return String.format("%s: %s", statement.getLabelName(), toSource(statement.getBody()));
      }

      @Override
      public String transformBreakStatement(BreakStatement statement) {
        if (statement.getLabelName() == null) {
          return "break;";
        }
        return String.format("break %s;", statement.getLabelName());
      }

      @Override
      public String transformContinueStatement(ContinueStatement statement) {
        if (statement.getLabelName() == null) {
          return "continue;";
        }
        return String.format("continue %s;", statement.getLabelName());
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
            toSource(arrayAccess.getArrayExpression()),
            toSource(arrayAccess.getIndexExpression()),
            toSource(expression.getRightOperand()));
      }

      @Override
      public String transformBlock(Block statement) {
        String statementsAsString =
            Joiner.on("\n").join(transformNodesToSource(statement.getStatements()));
        return "{\n" + statementsAsString + "}";
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
            expression.getCastTypeDescriptor(), toSource(expression.getExpression()));
      }

      @Override
      public String transformCharacterLiteral(CharacterLiteral characterLiteral) {
        return String.format(
            "%s /* %s */",
            Integer.toString(characterLiteral.getValue()),
            characterLiteral.getEscapedValue());
      }

      @Override
      public String transformExpressionStatement(ExpressionStatement statement) {
        return toSource(statement.getExpression()) + ";";
      }

      @Override
      public String transformFieldDescriptor(FieldDescriptor fieldDescriptor) {
        return ManglingNameUtils.getMangledName(fieldDescriptor);
      }

      @Override
      public String transformFieldAccess(FieldAccess fieldAccess) {
        String fieldMangledName = toSource(fieldAccess.getTarget());
        String qualifier = toSource(fieldAccess.getQualifier());
        return String.format("%s.%s", qualifier, fieldMangledName);
      }

      @Override
      public String transformInstanceOfExpression(InstanceOfExpression expression) {
        TypeDescriptor checkTypeDescriptor = expression.getTestTypeDescriptor();
        if (checkTypeDescriptor.isArray()) {
          return transformArrayInstanceOfExpression(expression);
        }
        return String.format(
            "%s.$isInstance(%s)",
            getAlias(checkTypeDescriptor),
            toSource(expression.getExpression()));
      }

      private String transformArrayInstanceOfExpression(
          InstanceOfExpression arrayInstanceOfExpression) {
        TypeDescriptor checkTypeDescriptor = arrayInstanceOfExpression.getTestTypeDescriptor();
        Preconditions.checkArgument(checkTypeDescriptor.isArray());

        String leafTypeName = getAlias(checkTypeDescriptor.getLeafTypeDescriptor());
        String expressionStr = toSource(arrayInstanceOfExpression.getExpression());
        return String.format(
            "%s.$instanceIsOfType(%s, %s, %s)",
            arraysTypeAlias(),
            expressionStr,
            leafTypeName,
            checkTypeDescriptor.getDimensions());
      }

      @Override
      public String transformMethodCall(MethodCall expression) {
        if (expression.isPrototypeCall()) {
          return transformPrototypeCall(expression);
        } else if (expression.getTarget().isJsProperty()) {
          return transformJsPropertyCall(expression);
        } else {
          return transformRegularMethodCall(expression);
        }
      }

      private String transformPrototypeCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        List<String> argumentSources = transformNodesToSource(expression.getArguments());
        return String.format(
            "%s.prototype.%s.call(%s)",
            toSource(methodDescriptor.getEnclosingClassTypeDescriptor()),
            toSource(methodDescriptor),
            Joiner.on(", ")
                .join(
                    Iterables.concat(
                        Arrays.asList(toSource(expression.getQualifier())), argumentSources)));
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
        String qualifier = toSource(expression.getQualifier());
        String propertyName = methodDescriptor.getJsPropertyName();
        return Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), propertyName);
      }

      /**
       * JsProperty setter is emitted as property set: qualifier.property = argument.
       */
      private String transformJsPropertySetter(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = toSource(expression.getQualifier());
        String propertyName = methodDescriptor.getJsPropertyName();
        Preconditions.checkArgument(expression.getArguments().size() == 1);
        return String.format(
            "%s = %s",
            Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), propertyName),
            toSource(expression.getArguments().get(0)));
      }

      private String transformRegularMethodCall(MethodCall expression) {
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = toSource(expression.getQualifier());
        String methodName = toSource(methodDescriptor);
        List<String> argumentSources = transformNodesToSource(expression.getArguments());
        return String.format(
            "%s(%s)",
            Joiner.on(".").skipNulls().join(Strings.emptyToNull(qualifier), methodName),
            Joiner.on(", ").join(argumentSources));
      }

      @Override
      public String transformMethodDescriptor(MethodDescriptor methodDescriptor) {
        if (methodDescriptor.isConstructor()) {
          return ManglingNameUtils.getCtorMangledName(methodDescriptor);
        } else if (methodDescriptor.isInit()) {
          return ManglingNameUtils.getInitMangledName(
              methodDescriptor.getEnclosingClassTypeDescriptor());
        } else {
          return ManglingNameUtils.getMangledName(methodDescriptor);
        }
      }

      @Override
      public String transformArrayAccess(ArrayAccess arrayAccess) {
        return String.format(
            "%s[%s]",
            toSource(arrayAccess.getArrayExpression()),
            toSource(arrayAccess.getIndexExpression()));
      }

      @Override
      public String transformArrayLiteral(ArrayLiteral arrayLiteral) {
        String valuesAsString =
            Joiner.on(", ").join(transformNodesToSource(arrayLiteral.getValueExpressions()));
        return "[ " + valuesAsString + " ]";
      }

      @Override
      public String transformNewArray(NewArray newArrayExpression) {
        if (newArrayExpression.getArrayLiteral() != null) {
          return transformArrayInit(newArrayExpression);
        }
        return transformArrayCreate(newArrayExpression);
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
                getAlias(newArrayExpression.getLeafTypeDescriptor())));
      }

      private String transformArrayInit(NewArray newArrayExpression) {

        Preconditions.checkArgument(newArrayExpression.getArrayLiteral() != null);

        String leafTypeName = getAlias(newArrayExpression.getLeafTypeDescriptor());
        int dimensionCount = newArrayExpression.getDimensionExpressions().size();
        String arrayLiteralAsString = toSource(newArrayExpression.getArrayLiteral());

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
      public String transformNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor();
        if (targetTypeDescriptor.isNative()) {
          return transformNativeNewInstance(expression);
        } else {
          return transformRegularNewInstance(expression);
        }
      }

      private String transformNativeNewInstance(NewInstance expression) {
        TypeDescriptor targetTypeDescriptor =
            expression.getTarget().getEnclosingClassTypeDescriptor();
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format("new %s(%s)", toSource(targetTypeDescriptor), argumentsList);
      }

      private String transformRegularNewInstance(NewInstance expression) {
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        String className = getAlias(expression.getTarget().getEnclosingClassTypeDescriptor());
        String constructorMangledName =
            ManglingNameUtils.getConstructorMangledName(expression.getTarget());
        return String.format("%s.%s(%s)", className, constructorMangledName, argumentsList);
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

      private String transformLongNumberLiteral(NumberLiteral expression) {
        long longValue = expression.getValue().longValue();

        if (longValue < Integer.MAX_VALUE && longValue > Integer.MIN_VALUE) {
          // The long value is small enough to fit in an int. Emit the terse initialization.
          return String.format("%s.$fromInt(%s)", longsTypeAlias(), Long.toString(longValue));
        }

        // The long value is pretty large. Emit the verbose initialization.
        return String.format("%s.$fromString('%s')", longsTypeAlias(), Long.toString(longValue));
      }

      @Override
      public String transformParenthesizedExpression(ParenthesizedExpression expression) {
        return String.format("(%s)", toSource(expression.getExpression()));
      }

      @Override
      public String transformPostfixExpression(PostfixExpression expression) {
        Preconditions.checkArgument(
            TypeDescriptors.get().primitiveLong != expression.getTypeDescriptor());
        return String.format(
            "%s%s", toSource(expression.getOperand()), expression.getOperator().toString());
      }

      @Override
      public String transformMultiExpression(MultiExpression multipleExpression) {
        String expressionsAsString =
            Joiner.on(", ").join(transformNodesToSource(multipleExpression.getExpressions()));
        return "( " + expressionsAsString + " )";
      }

      @Override
      public String transformPrefixExpression(PrefixExpression expression) {
        // The + prefix operator is a NOP.
        if (expression.getOperator() == PrefixOperator.PLUS) {
          return toSource(expression.getOperand());
        }

        return String.format(
            "%s%s", expression.getOperator().toString(), toSource(expression.getOperand()));
      }

      @Override
      public String transformReturnStatement(ReturnStatement statement) {
        Expression expression = statement.getExpression();
        if (expression == null) {
          return "return;";
        } else {
          return "return " + toSource(expression) + ";";
        }
      }

      @Override
      public String transformStringLiteral(StringLiteral expression) {
        return expression.getEscapedValue();
      }

      @Override
      public String transformSwitchCase(SwitchCase switchCase) {
        if (switchCase.isDefault()) {
          return "default:";
        }
        return "case " + toSource(switchCase.getMatchExpression()) + ":";
      }

      @Override
      public String transformSwitchStatement(SwitchStatement switchStatement) {
        String switchCasesAsString =
            Joiner.on("\n").join(transformNodesToSource(switchStatement.getBodyStatements()));
        return String.format(
            "switch (%s) { %s }",
            toSource(switchStatement.getMatchExpression()),
            switchCasesAsString);
      }

      @Override
      public String transformSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        return toSource(synchronizedStatement.getExpression())
            + ";"
            + toSource(synchronizedStatement.getBody());
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
      public String transformThrowStatement(ThrowStatement statement) {
        return "throw " + toSource(statement.getExpression()) + ";";
      }

      @Override
      public String transformTryStatement(TryStatement statement) {
        String tryBlock = String.format("try %s", toSource(statement.getBody()));
        String catchBlock;
        if (statement.getCatchClauses().isEmpty()) {
          catchBlock = "";
        } else {
          String exceptionVarName = statement.getCatchClauses().get(0).getExceptionVar().getName();
          catchBlock =
              String.format(
                  "catch (%s) { %s }\n",
                  exceptionVarName,
                  transformCatchClauses(statement.getCatchClauses(), exceptionVarName));
        }
        String finallyBlock =
            statement.getFinallyBlock() == null
                ? ""
                : String.format("finally %s", toSource(statement.getFinallyBlock()));
        return tryBlock + catchBlock + finallyBlock;
      }

      private String annotateWithJsDoc(TypeDescriptor castTypeDescriptor, String expression) {
        return String.format("/**@type {%s} */ (%s)", getJsDocName(castTypeDescriptor), expression);
      }

      /**
       * Translates multiple catch clauses to if-else statement.
       */
      private String transformCatchClauses(
          List<CatchClause> catchClauses, final String exceptionVarName) {
        List<String> transformedCatchClauses =
            Lists.transform(
                catchClauses,
                new Function<CatchClause, String>() {
                  @Override
                  public String apply(CatchClause catchClause) {
                    return transformCatchClause(catchClause, exceptionVarName);
                  }
                });
        String ifBranches = Joiner.on(" else ").join(transformedCatchClauses);
        String elseBranch = String.format("else { throw %s; }", exceptionVarName);
        return ifBranches + elseBranch;
      }

      /**
       * Translates a catch clause like catch (Exception e) { ...body... }
       * to a if statement like
       * if ($Exception.isInstance(e)) { ...body... }
       */
      private String transformCatchClause(
          CatchClause catchClause, final String globalExceptionVarName) {
        Variable localExceptionVar = catchClause.getExceptionVar();
        // If this catch clause uses a different exception variable name than the global one, then
        // re-expose the exception variable via the global exception variable name.
        String localVarDecl =
            localExceptionVar.getName().equals(globalExceptionVarName)
                ? ""
                : String.format(
                    "let %s = %s;", localExceptionVar.getName(), globalExceptionVarName);
        String blockStatementsAsString =
            Joiner.on("\n").join(transformNodesToSource(catchClause.getBody().getStatements()));
        return String.format(
            "if (%s) {%s %s}",
            transformExceptionVariable(
                localExceptionVar.getTypeDescriptor(), globalExceptionVarName),
            localVarDecl,
            blockStatementsAsString);
      }

      /**
       * Translates exception variable declaration in a catch clause like
       * (RuntimeException | NullPointerException e)
       * to the condition expression in a if statement like
       * (RuntimeException.$isInstance(e) || NullPointerException.$isInstance(e))
       */
      private String transformExceptionVariable(
          TypeDescriptor exceptionTypeDescriptor, final String globalExceptionVarName) {
        List<TypeDescriptor> exceptionTypeDescriptors = new ArrayList<>();
        if (exceptionTypeDescriptor instanceof UnionTypeDescriptor) {
          exceptionTypeDescriptors.addAll(
              ((UnionTypeDescriptor) exceptionTypeDescriptor).getTypes());
        } else {
          exceptionTypeDescriptors.add(exceptionTypeDescriptor);
        }
        List<String> isInstanceCalls =
            Lists.transform(
                exceptionTypeDescriptors,
                new Function<TypeDescriptor, String>() {
                  @Override
                  public String apply(TypeDescriptor typeDescriptor) {
                    return String.format(
                        "%s.$isInstance(%s)", getAlias(typeDescriptor), globalExceptionVarName);
                  }
                });
        return Joiner.on(" || ").join(isInstanceCalls);
      }

      @Override
      public String transformTypeDescriptor(TypeDescriptor typeDescriptor) {
        return getAlias(typeDescriptor);
      }

      @Override
      public String transformVariableReference(VariableReference expression) {
        return toSource(expression.getTarget());
      }

      @Override
      public String transformIfStatement(IfStatement ifStatement) {
        if (ifStatement.getElseStatement() == null) {
          return String.format(
              "if (%s) %s",
              toSource(ifStatement.getConditionExpression()),
              toSource(ifStatement.getThenStatement()));
        }
        return String.format(
            "if (%s) %s else %s",
            toSource(ifStatement.getConditionExpression()),
            toSource(ifStatement.getThenStatement()),
            toSource(ifStatement.getElseStatement()));
      }

      @Override
      public String transformEmptyStatement(EmptyStatement emptyStatement) {
        return ";";
      }

      @Override
      public String transformTernaryExpression(TernaryExpression ternaryExpression) {
        String conditionExpressionAsString = toSource(ternaryExpression.getConditionExpression());
        String trueExpressionAsString = toSource(ternaryExpression.getTrueExpression());
        String falseExpressionAsString = toSource(ternaryExpression.getFalseExpression());
        return String.format(
            "%s ? %s : %s",
            conditionExpressionAsString,
            trueExpressionAsString,
            falseExpressionAsString);
      }

      @Override
      public String transformWhileStatement(WhileStatement whileStatement) {
        String conditionAsString = toSource(whileStatement.getConditionExpression());
        String blockAsString = toSource(whileStatement.getBody());
        return String.format("while (%s) %s", conditionAsString, blockAsString);
      }

      @Override
      public String transformDoWhileStatement(DoWhileStatement doWhileStatement) {
        String conditionAsString = toSource(doWhileStatement.getConditionExpression());
        String blockAsString = toSource(doWhileStatement.getBody());
        return String.format("do %s while(%s);", blockAsString, conditionAsString);
      }

      @Override
      public String transformForStatement(ForStatement forStatement) {

        List<String> initializers = new ArrayList<>();
        for (Expression e : forStatement.getInitializers()) {
          initializers.add(toSource(e));
        }
        String initializerAsString = Joiner.on(",").join(initializers);

        String conditionExpressionAsString =
            forStatement.getConditionExpression() == null
                ? ""
                : toSource(forStatement.getConditionExpression());

        List<String> updaters = new ArrayList<>();
        for (Expression e : forStatement.getUpdaters()) {
          updaters.add(toSource(e));
        }
        String updatersAsString = Joiner.on(",").join(updaters);

        String blockAsString = toSource(forStatement.getBody());

        return String.format(
            "for (%s; %s; %s) %s",
            initializerAsString,
            conditionExpressionAsString,
            updatersAsString,
            blockAsString);
      }

      @Override
      public String transformVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        List<String> fragmentsAsString =
            transformNodesToSource(variableDeclarationExpression.getFragments());
        return "let " + Joiner.on(", ").join(fragmentsAsString);
      }

      @Override
      public String transformVariableDeclarationStatement(
          VariableDeclarationStatement variableDeclarationStatement) {
        List<String> fragmentsAsString =
            transformNodesToSource(variableDeclarationStatement.getFragments());
        return "let " + Joiner.on(", ").join(fragmentsAsString) + ";";
      }

      @Override
      public String transformVariableDeclarationFragment(
          VariableDeclarationFragment variableDeclarationFragment) {

        String variableAsString = toSource(variableDeclarationFragment.getVariable());
        if (variableDeclarationFragment.getInitializer() == null) {
          return variableAsString;
        }

        String initializerAsString = toSource(variableDeclarationFragment.getInitializer());
        return String.format("%s = %s", variableAsString, initializerAsString);
      }

      @Override
      public String transformVariable(Variable variable) {
        return aliasByVariable.containsKey(variable)
            ? aliasByVariable.get(variable)
            : variable.getName();
      }

      public List<String> transformNodesToSource(List<? extends Node> nodes) {
        return Lists.transform(
            nodes,
            new Function<Node, String>() {
              @Override
              public String apply(Node node) {
                return toSource(node);
              }
            });
      }

      public String getAlias(TypeDescriptor typeDescriptor) {
        String alias = aliasByTypeDescriptor.get(typeDescriptor);
        return alias == null ? typeDescriptor.getClassName() : alias;
      }

      private String arraysTypeAlias() {
        return toSource(BootstrapType.ARRAYS.getDescriptor());
      }

      private String assertsTypeAlias() {
        return toSource(BootstrapType.ASSERTS.getDescriptor());
      }

      private String longsTypeAlias() {
        return toSource(BootstrapType.LONGS.getDescriptor());
      }
    }
    return new ToSourceTransformer().process(node);
  }
}
