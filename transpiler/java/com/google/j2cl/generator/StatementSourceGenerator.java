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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractTransformer;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.AssertStatement;
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
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
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
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.UnionTypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableDeclarationStatement;
import com.google.j2cl.ast.VariableReference;
import com.google.j2cl.ast.WhileStatement;
import com.google.j2cl.generator.visitors.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate javascript source code for {@code Statement}.
 * TODO: Keep an eye on performance and if things get slow then replace these String operations with
 * a StringBuilder.
 */
public class StatementSourceGenerator {
  private Map<TypeDescriptor, String> aliasByTypeDescriptor = new HashMap<>();

  public StatementSourceGenerator(Set<Import> imports) {
    for (Import anImport : imports) {
      aliasByTypeDescriptor.put(anImport.getTypeDescriptor(), anImport.getAlias());
    }
  }

  /**
   * Returns the JsDoc type name.
   */
  public String getJsDocName(TypeDescriptor typeDescriptor) {
    return getJsDocName(typeDescriptor, false);
  }

  /**
   * Returns the JsDoc type name.
   */
  public String getJsDocName(TypeDescriptor typeDescriptor, boolean forUseInExtendsOrImplements) {
    if (typeDescriptor.isArray()) {
      return String.format(
          "%s%s%s",
          Strings.repeat("Array<", typeDescriptor.getDimensions()),
          getJsDocName(typeDescriptor.getLeafTypeDescriptor()),
          Strings.repeat(">", typeDescriptor.getDimensions()));
    }

    if (typeDescriptor.isParameterizedType()) {
      return String.format(
          "%s<%s>",
          getJsDocName(typeDescriptor.getRawTypeDescriptor(), forUseInExtendsOrImplements),
          getJsDocNames(typeDescriptor.getTypeArgumentDescriptors()));
    }

    // Special cases.
    switch (typeDescriptor.getSourceName()) {
      case TypeDescriptor.BYTE_TYPE_NAME:
      case TypeDescriptor.SHORT_TYPE_NAME:
      case TypeDescriptor.INT_TYPE_NAME:
      case TypeDescriptor.FLOAT_TYPE_NAME:
      case TypeDescriptor.DOUBLE_TYPE_NAME:
      case TypeDescriptor.CHAR_TYPE_NAME:
        return "number";
      case TypeDescriptor.LONG_TYPE_NAME:
        return "!" + toSource(TypeDescriptors.NATIVE_LONG_TYPE_DESCRIPTOR);
      case "java.lang.String":
        return "?string";
      case "java.io.Serializable":
      case "java.lang.CharSequence":
      case "java.lang.Comparable":
        if (!forUseInExtendsOrImplements) {
          // Interfaces that might be also implemented by string
          return "(" + toSource(typeDescriptor) + "|string)";
        }
      case "java.lang.Object":
        if (!forUseInExtendsOrImplements) {
          // Object covers also string and arrays.
          return "*";
        }
    }
    if (typeDescriptor.isPrimitive()) {
      return typeDescriptor.getSimpleName();
    }
    return toSource(typeDescriptor);
  }

  /**
   * Returns the list of JsDoc names of a list of type descriptors.
   */
  public String getJsDocNames(List<TypeDescriptor> typeDescriptors) {
    List<String> typeParameterDescriptors =
        Lists.transform(
            typeDescriptors,
            new Function<TypeDescriptor, String>() {
              @Override
              public String apply(TypeDescriptor typeDescriptor) {
                return getJsDocName(typeDescriptor);
              }
            });
    return Joiner.on(", ").join(typeParameterDescriptors);
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

        if (TranspilerUtils.isAssignment(operator) && leftOperand instanceof ArrayAccess) {
          return transformArrayAssignmentBinaryExpression(expression);
        } else if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == leftOperand.getTypeDescriptor()
            && operator != BinaryOperator.ASSIGN) {
          // Skips assignment because it doesn't need special handling.
          return transformLongBinaryExpression(expression);
        } else {
          return transformRegularBinaryExpression(expression);
        }
      }

      private String transformLongBinaryExpression(BinaryExpression expression) {
        Preconditions.checkArgument(TranspilerUtils.isValidForLongs(expression.getOperator()));

        String longOperationFunctionName =
            TranspilerUtils.getLongOperationFunctionName(expression.getOperator());
        Expression leftOperand = expression.getLeftOperand();
        Expression rightOperand = expression.getRightOperand();
        Expression qualifier = TranspilerUtils.getQualifier(leftOperand);

        boolean hasSideEffect = TranspilerUtils.isAssignment(expression.getOperator());
        if (!hasSideEffect) {
          // The simplest case. The referenced long is not being modified so all that's necessary is
          // to do some computation and return a new value.
          return String.format(
              "%s.%s(%s, %s)",
              longsTypeAlias(),
              longOperationFunctionName,
              toSource(leftOperand),
              toSource(rightOperand));
        }

        if (qualifier == null) {
          // The medium case. The referenced long *is* being modified but it has no qualifier so no
          // care needs to be taken to avoid double side-effects from dereferencing the qualifier
          // twice.
          String leftOperandAsSource = toSource(leftOperand);
          return String.format(
              "%s = %s.%s(%s, %s)",
              leftOperandAsSource,
              longsTypeAlias(),
              longOperationFunctionName,
              leftOperandAsSource,
              toSource(rightOperand));
        }

        // The hard case. The referenced long is being modified and it has a qualifier. Take special
        // care to only dereference the qualifier once (to avoid double side effects), store it in a
        // temporary variable and use that temporary variable in the rest of the computation.
        MemberReference memberReference = (MemberReference) leftOperand;
        String fieldOrMethodDescriptorAsString = toSource((Node) memberReference.getTarget());
        return String.format(
            "$LongUtils.$q = %s, $LongUtils.$q.%s = %s.%s($LongUtils.$q.%s, %s)",
            toSource(qualifier),
            fieldOrMethodDescriptorAsString,
            longsTypeAlias(),
            longOperationFunctionName,
            fieldOrMethodDescriptorAsString,
            toSource(rightOperand));
      }

      @Override
      public String transformBreakStatement(BreakStatement statement) {
        return "break;";
      }

      @Override
      public String transformContinueStatement(ContinueStatement statement) {
        return "continue;";
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
            "%s.%s(%s, %s, %s)",
            arraysTypeAlias(),
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
        return ManglingNameUtils.getMangledName(
            fieldDescriptor, isInClinit(fieldDescriptor.getEnclosingClassTypeDescriptor()));
      }

      @Override
      public String transformFieldAccess(FieldAccess fieldAccess) {
        String fieldMangledName = toSource(fieldAccess.getTarget());

        // make 'this.' reference and static reference explicit.
        // TODO(rluble): We should probably make this explicit at the AST level, either by a
        // normalization pass or by construction.
        String qualifier = transformQualifier(fieldAccess);
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
        MethodDescriptor methodDescriptor = expression.getTarget();
        String qualifier = transformQualifier(expression);
        String argumentList =
            Joiner.on(", ").join(transformNodesToSource(expression.getArguments()));
        return String.format("%s.%s(%s)", qualifier, toSource(methodDescriptor), argumentList);
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
          if (TypeDescriptors.OBJECT_TYPE_DESCRIPTOR
              == newArrayExpression.getLeafTypeDescriptor()) {
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
        String className =
            getAlias(expression.getConstructorMethodDescriptor().getEnclosingClassTypeDescriptor());
        String constructorMangledName =
            ManglingNameUtils.getConstructorMangledName(
                expression.getConstructorMethodDescriptor());
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
        if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == expression.getTypeDescriptor()) {
          return transformLongNumberLiteral(expression);
        }
        return expression.getValue().toString();
      }

      private String transformLongNumberLiteral(NumberLiteral expression) {
        Preconditions.checkArgument(expression.getValue() instanceof Long);

        long longValue = (long) expression.getValue();

        if (longValue < Integer.MAX_VALUE && longValue > Integer.MIN_VALUE) {
          // The long value is small enough to fit in an int. Emit the terse initialization.
          return String.format(
              "%s.$fromInt(%s)", longsTypeAlias(), Long.toString((long) expression.getValue()));
        }

        // The long value is pretty large. Emit the verbose initialization.
        return String.format(
            "%s.$fromString('%s')", longsTypeAlias(), Long.toString((long) expression.getValue()));
      }

      @Override
      public String transformParenthesizedExpression(ParenthesizedExpression expression) {
        return String.format("(%s)", toSource(expression.getExpression()));
      }

      @Override
      public String transformPostfixExpression(PostfixExpression expression) {
        if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == expression.getTypeDescriptor()) {
          return transformLongPostfixExpression(expression);
        }
        return String.format(
            "%s%s", toSource(expression.getOperand()), expression.getOperator().toString());
      }

      private String transformLongPostfixExpression(PostfixExpression expression) {
        Expression operand = expression.getOperand();
        Expression qualifier = TranspilerUtils.getQualifier(operand);

        if (qualifier == null) {
          // The easier case. The referenced long is being modified but since the pre-modified value
          // must be returned it is first stored in a temporary variable which is returned at the
          // end. Since there is no qualifier no care needs to be taken to avoid double side-effects
          // from dereferencing the qualifier twice.
          String operandAsSource = toSource(operand);
          return String.format(
              "($LongUtils.$v = %s, %s = %s.%s(%s), $LongUtils.$v)",
              operandAsSource,
              operandAsSource,
              longsTypeAlias(),
              TranspilerUtils.getLongOperationFunctionName(expression.getOperator()),
              operandAsSource);
        }

        // The harder case. Like the easier case except that special care needs to be taken to only
        // dereference the qualifier once (to avoid double side effects). This is accomplished by
        // storing it in a temporary variable and using that temporary variable in the rest of the
        // computation.
        MemberReference memberReference = (MemberReference) operand;
        Member target = memberReference.getTarget();
        String fieldOrMethodDescriptorAsString = toSource((Node) target);
        return String.format(
            "($LongUtils.$q = %s, $LongUtils.$v = $LongUtils.$q.%s, "
                + "$LongUtils.$q.%s = %s.%s($LongUtils.$q.%s), $LongUtils.$v)",
            toSource(qualifier),
            fieldOrMethodDescriptorAsString,
            fieldOrMethodDescriptorAsString,
            longsTypeAlias(),
            TranspilerUtils.getLongOperationFunctionName(expression.getOperator()),
            fieldOrMethodDescriptorAsString);
      }

      @Override
      public String transformPrefixExpression(PrefixExpression expression) {
        if (TypeDescriptors.LONG_TYPE_DESCRIPTOR == expression.getTypeDescriptor()
            && expression.getOperator() != PrefixOperator.PLUS) {
          // Skips the + prefix because it is a NOP.
          return transformLongPrefixExpression(expression);
        }
        return transformRegularPrefixExpression(expression);
      }

      private String transformLongPrefixExpression(PrefixExpression expression) {
        Preconditions.checkArgument(TranspilerUtils.isValidForLongs(expression.getOperator()));

        String longOperationFunctionName =
            TranspilerUtils.getLongOperationFunctionName(expression.getOperator());
        Expression operand = expression.getOperand();
        Expression qualifier = TranspilerUtils.getQualifier(operand);

        if (!TranspilerUtils.hasSideEffect(expression.getOperator())) {
          // The simplest case. The referenced long is not being modified so all that's necessary is
          // to do some computation and return a new value.
          return String.format(
              "%s.%s(%s)", longsTypeAlias(), longOperationFunctionName, toSource(operand));
        }

        if (qualifier == null) {
          // The medium case. The referenced long *is* being modified but it has no qualifier so no
          // care needs to be taken to avoid double side-effects from dereferencing the qualifier
          // twice.
          return String.format(
              "(%s = %s.%s(%s))",
              toSource(operand),
              longsTypeAlias(),
              longOperationFunctionName,
              toSource(operand));
        }

        // The hard case. The referenced long is being modified and it has a qualifier. Take special
        // care to only dereference the qualifier once (to avoid double side effects), store it in a
        // temporary variable and use that temporary variable in the rest of the computation.
        MemberReference memberReference = (MemberReference) operand;
        String fieldOrMethodDescriptorAsString = toSource((Node) memberReference.getTarget());
        return String.format(
            "($LongUtils.$q = %s, $LongUtils.$q.%s = %s.%s($LongUtils.$q.%s))",
            toSource(qualifier),
            fieldOrMethodDescriptorAsString,
            longsTypeAlias(),
            longOperationFunctionName,
            fieldOrMethodDescriptorAsString);
      }

      private String transformRegularPrefixExpression(PrefixExpression expression) {
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
        return expression.getTarget().getName();
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
        return variable.getName();
      }

      private String transformQualifier(MemberReference memberRef) {
        Member member = memberRef.getTarget();
        String qualifier =
            memberRef.getQualifier() == null
                ? (member.isStatic() ? getAlias(member.getEnclosingClassTypeDescriptor()) : "this")
                : toSource(memberRef.getQualifier());
        return qualifier;
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
        return toSource(TypeDescriptors.VM_ARRAYS_TYPE_DESCRIPTOR);
      }

      private String assertsTypeAlias() {
        return toSource(TypeDescriptors.VM_ASSERTS_TYPE_DESCRIPTOR);
      }

      private String longsTypeAlias() {
        return toSource(TypeDescriptors.NATIVE_LONGS_TYPE_DESCRIPTOR);
      }

      private boolean isInClinit(TypeDescriptor typeDescriptor) {
        return typeDescriptor.equals(inClinitForTypeDescriptor);
      }
    }
    return new ToSourceTransformer().process(node);
  }
}
