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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractTransformer;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayTypeReference;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BooleanLiteral;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldReference;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.ParenthesizedExpression;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.RegularTypeReference;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.StringLiteral;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.UnionTypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableDeclarationStatement;
import com.google.j2cl.ast.VariableReference;
import com.google.j2cl.ast.WhileStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate javascript source code for {@code Statement}.
 * TODO: Keep an eye on performance and if things get slow then replace these String operations with
 * a StringBuilder.
 */
public class StatementSourceGenerator {
  // TODO: static field may be a potential threading problem.
  private static TypeReference inClinitForType = null;

  public static String toSource(Node node) {
    class ToSourceTransformer extends AbstractTransformer<String> {
      @Override
      public String transformAssertStatement(AssertStatement statement) {
        if (statement.getMessage() == null) {
          return String.format(
              "Asserts.$enabled() && Asserts.$assert(%s);", toSource(statement.getExpression()));
        } else {
          return String.format(
              "Asserts.$enabled() && Asserts.$assertWithMessage(%s, %s);",
              toSource(statement.getExpression()),
              toSource(statement.getMessage()));
        }
      }

      @Override
      public String transformBinaryExpression(BinaryExpression expression) {
        if (TranspilerUtils.isAssignment(expression.getOperator())
            && expression.getLeftOperand() instanceof ArrayAccess) {
          return transformArrayAssignmentBinaryExpression(expression);
        } else {
          return transformRegularBinaryExpression(expression);
        }
      }

      private String transformRegularBinaryExpression(BinaryExpression expression) {
        Preconditions.checkState(
            !(TranspilerUtils.isAssignment(expression.getOperator())
                && expression.getLeftOperand() instanceof ArrayAccess));

        return String.format(
            "%s %s %s",
            toSource(expression.getLeftOperand()),
            expression.getOperator().toString(),
            toSource(expression.getRightOperand()));
      }

      // TODO: extend to handle long[].
      private String transformArrayAssignmentBinaryExpression(BinaryExpression expression) {
        Preconditions.checkState(
            TranspilerUtils.isAssignment(expression.getOperator())
                && expression.getLeftOperand() instanceof ArrayAccess);

        ArrayAccess arrayAccess = (ArrayAccess) expression.getLeftOperand();
        return String.format(
            "Arrays.%s(%s, %s, %s)",
            TranspilerUtils.getArrayAssignmentFunctionName(expression.getOperator()),
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
        TypeReference castType = expression.getCastType();
        if (castType instanceof ArrayTypeReference) {
          return transformArrayCastExpression(expression);
        } else if (castType instanceof RegularTypeReference) {
          return transformRegularCastExpression(expression);
        }
        throw new RuntimeException(
            "TODO: Implement toSource() for cast to " + castType.getClass().getName());
      }

      private String transformRegularCastExpression(CastExpression castExpression) {
        Preconditions.checkArgument(castExpression.getCastType() instanceof RegularTypeReference);
        RegularTypeReference regularTypeRef = (RegularTypeReference) castExpression.getCastType();

        if (regularTypeRef.isPrimitive()) {
          throw new RuntimeException("TODO: Implement toSource() for cast to primitive type");
        }

        String jsDocTypeName = TranspilerUtils.getJsDocName(regularTypeRef);
        String typeName = TranspilerUtils.getClassName(regularTypeRef);
        String expressionStr = toSource(castExpression.getExpression());
        String isInstanceCallStr = String.format("%s.$isInstance(%s)", typeName, expressionStr);
        return String.format(
            "/**@type {%s} */ (Casts.to(%s, %s))", jsDocTypeName, expressionStr, isInstanceCallStr);
      }

      private String transformArrayCastExpression(CastExpression castExpression) {
        Preconditions.checkArgument(castExpression.getCastType() instanceof ArrayTypeReference);
        ArrayTypeReference arrayCastType = (ArrayTypeReference) castExpression.getCastType();

        String jsDocTypeName = TranspilerUtils.getJsDocName(arrayCastType);
        String leafTypeName = TranspilerUtils.getClassName(arrayCastType.getLeafTypeRef());
        String expressionStr = toSource(castExpression.getExpression());
        return String.format(
            "/**@type {%s} */ (Arrays.$castTo(%s, %s, %s))",
            jsDocTypeName,
            expressionStr,
            leafTypeName,
            arrayCastType.getDimensions());
      }

      @Override
      public String transformExpressionStatement(ExpressionStatement statement) {
        return toSource(statement.getExpression()) + ";";
      }

      @Override
      public String transformFieldAccess(FieldAccess fieldAccess) {
        FieldReference target = fieldAccess.getTarget();
        String fieldMangledName =
            ManglingNameUtils.getMangledName(target, isInClinit(target.getEnclosingClassRef()));

        // make 'this.' reference and static reference explicit.
        // TODO(rluble): We should probably make this explicit at the AST level, either by a
        // normalization pass or by construction.
        String qualifier = transformQualifier(fieldAccess);
        return String.format("%s.%s", qualifier, fieldMangledName);
      }

      @Override
      public String transformInstanceOfExpression(InstanceOfExpression expression) {
        TypeReference checkType = expression.getTestTypeRef();
        if (checkType.isArray()) {
          throw new RuntimeException(
              "TODO: Implement toSource() for instanceOf ArrayTypeReference");
        }
        return String.format(
            "%s.$isInstance(%s)",
            TranspilerUtils.getClassName(checkType),
            toSource(expression.getExpression()));
      }

      @Override
      public String transformMethodCall(MethodCall expression) {
        MethodReference methodRef = expression.getTarget();
        String qualifier = transformQualifier(expression);
        String argumentList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format("%s.%s(%s)", qualifier, toSource(methodRef), argumentList);
      }

      @Override
      public String transformMethodReference(MethodReference methodRef) {
        if (methodRef.isConstructor()) {
          return ManglingNameUtils.getCtorMangledName(methodRef);
        } else if (methodRef.isInit()) {
          return ManglingNameUtils.getInitMangledName(methodRef.getEnclosingClassRef());
        } else {
          return ManglingNameUtils.getMangledName(methodRef);
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
      public String transformNewArray(NewArray expression) {
        String dimensionsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getDimensionExpressions()));

        TypeReference leafTypeRef = expression.getLeafTypeRef();
        if (leafTypeRef.isPrimitive()) {
          // Primitive array creations look like Arrays.$createByte, etc.
          return String.format(
              "Arrays.$create%s([%s])",
              TranspilerUtils.toProperCase(leafTypeRef.getSimpleName()),
              dimensionsList);
        }

        return String.format(
            "Arrays.$create([%s], %s)", dimensionsList, TranspilerUtils.getClassName(leafTypeRef));
      }

      @Override
      public String transformNewInstance(NewInstance expression) {
        String className =
            TranspilerUtils.getClassName(expression.getConstructorRef().getEnclosingClassRef());
        String constructorMangledName =
            ManglingNameUtils.getConstructorMangledName(expression.getConstructorRef());
        String argumentsList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format("%s.%s(%s)", className, constructorMangledName, argumentsList);
      }

      @Override
      public String transformNullLiteral(NullLiteral expression) {
        return "null";
      }

      @Override
      public String transformNumberLiteral(NumberLiteral expression) {
        return expression.getToken();
      }

      @Override
      public String transformParenthesizedExpression(ParenthesizedExpression expression) {
        return String.format("(%s)", toSource(expression.getExpression()));
      }

      @Override
      public String transformPostfixExpression(PostfixExpression expression) {
        return String.format(
            "%s%s", toSource(expression.getOperand()), expression.getOperator().toString());
      }

      @Override
      public String transformPrefixExpression(PrefixExpression expression) {
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
      public String transformThisReference(ThisReference expression) {
        // We expect that after normalization (InnerClassExtractor) there should be no qualified
        // this reference.
        Preconditions.checkArgument(
            expression.getTypeRef() == null,
            "There should be no qualified thisRef after normalization.");
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
          String exceptionVarName =
              statement
                  .getCatchClauses()
                  .get(0)
                  .getExceptionVar()
                  .getName();
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
            transformExceptionVariable(localExceptionVar.getTypeRef(), globalExceptionVarName),
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
          TypeReference exceptionTypeRef, final String globalExceptionVarName) {
        List<TypeReference> exceptionTypeRefs = new ArrayList<>();
        if (exceptionTypeRef instanceof UnionTypeReference) {
          exceptionTypeRefs.addAll(((UnionTypeReference) exceptionTypeRef).getTypes());
        } else {
          exceptionTypeRefs.add(exceptionTypeRef);
        }
        List<String> isInstanceCalls =
            Lists.transform(
                exceptionTypeRefs,
                new Function<TypeReference, String>() {
                  @Override
                  public String apply(TypeReference typeRef) {
                    return String.format(
                        "%s.$isInstance(%s)",
                        TranspilerUtils.getClassName(typeRef),
                        globalExceptionVarName);
                  }
                });
        return Joiner.on(" || ").join(isInstanceCalls);
      }

      @Override
      public String transformTypeReference(TypeReference typeRef) {
        return TranspilerUtils.getClassName(typeRef);
      }

      @Override
      public String transformVariableReference(VariableReference expression) {
        return expression.getTarget().getName();
      }

      @Override
      public String transformIfStatement(IfStatement ifStatement) {
        String conditionAsString = toSource(ifStatement.getConditionExpression());
        String trueBlockAsString = toSource(ifStatement.getTrueBlock());

        Block falseBlock = ifStatement.getFalseBlock();

        if (falseBlock == null) {
          return String.format("if (%s) %s", conditionAsString, trueBlockAsString);
        }
        String falseBlockAsString = null;
        if (falseBlock.getStatements().size() == 1
            && falseBlock.getStatements().get(0) instanceof IfStatement) {
          IfStatement nestedIfStatement = (IfStatement) falseBlock.getStatements().get(0);
          falseBlockAsString = toSource(nestedIfStatement);
        } else {
          falseBlockAsString = toSource(ifStatement.getFalseBlock());
        }

        return String.format(
            "if (%s) %s else %s", conditionAsString, trueBlockAsString, falseBlockAsString);
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
        String blockAsString = toSource(whileStatement.getBlock());
        return String.format("while (%s) %s", conditionAsString, blockAsString);
      }

      @Override
      public String transformDoWhileStatement(DoWhileStatement doWhileStatement) {
        String conditionAsString = toSource(doWhileStatement.getConditionExpression());
        String blockAsString = toSource(doWhileStatement.getBlock());
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

        String blockAsString = toSource(forStatement.getBlock());

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
        return variable.getName();
      }

      private String transformQualifier(MemberReference memberRef) {
        Member member = memberRef.getTarget();
        String qualifier =
            memberRef.getQualifier() == null
                ? (member.isStatic()
                    ? TranspilerUtils.getClassName(member.getEnclosingClassRef())
                    : "this")
                : toSource(memberRef.getQualifier());
        return qualifier;
      }
    }
    return new ToSourceTransformer().process(node);
  }

  public static List<String> transformNodesToSource(List<? extends Node> nodes) {
    return Lists.transform(
        nodes,
        new Function<Node, String>() {
          @Override
          public String apply(Node node) {
            return toSource(node);
          }
        });
  }

  public static void setInClinit(TypeReference type) {
    inClinitForType = type;
  }

  public static void resetInClinit() {
    inClinitForType = null;
  }

  private static boolean isInClinit(TypeReference type) {
    return type.equals(inClinitForType);
  }
}
