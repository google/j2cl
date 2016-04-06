/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast;

import com.google.common.base.Strings;

import java.util.List;

/**
 * Produces a source code like representation of the AST for debugging purposes (not intending to
 * be completely valid Java).
 */
class ToStringRenderer {

  static String render(Node node) {
    final StringBuilder result = new StringBuilder();

    new AbstractVisitor() {
      int currentIndent = 0;

      @Override
      public boolean enterArrayAccess(ArrayAccess arrayAccess) {
        accept(arrayAccess.getArrayExpression());
        print("[");
        accept(arrayAccess.getIndexExpression());
        print("]");
        return false;
      }

      @Override
      public boolean enterArrayLiteral(ArrayLiteral arrayLiteral) {
        print("[");
        printSeparatedExpressions(",", arrayLiteral.getValueExpressions());
        print("]");
        return false;
      }

      @Override
      public boolean enterAssertStatement(AssertStatement assertStatement) {
        print("assert ");
        accept(assertStatement.getExpression());
        if (assertStatement.getMessage() != null) {
          print(" : ");
          accept(assertStatement.getExpression());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterBinaryExpression(BinaryExpression binaryExpression) {
        accept(binaryExpression.getLeftOperand());
        print(" ");
        print(binaryExpression.getOperator().getSymbol());
        print(" ");
        accept(binaryExpression.getRightOperand());
        return false;
      }

      @Override
      public boolean enterBlock(Block block) {
        print("{");
        indent();
        for (Statement statement : block.getStatements()) {
          newLine();
          accept(statement);
        }
        unIndent();
        newLine();
        print("}");
        return false;
      }

      @Override
      public boolean enterBooleanLiteral(BooleanLiteral booleanLiteral) {
        print(booleanLiteral.getValue() ? "true" : "false");
        return false;
      }

      @Override
      public boolean enterBreakStatement(BreakStatement breakStatement) {
        print("break");
        if (breakStatement.getLabelName() != null) {
          print(" ");
          print(breakStatement.getLabelName());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterCatchClause(CatchClause catchClause) {
        print(" catch (");
        print(catchClause.getExceptionVar().getTypeDescriptor());
        print(" ");
        print(catchClause.getExceptionVar().getName());
        print(") ");
        accept(catchClause.getBody());
        return false;
      }

      @Override
      public boolean enterCastExpression(CastExpression castExpression) {
        print(castExpression.isRaw() ? "/**" : "(");
        print(castExpression.getCastTypeDescriptor());
        print(castExpression.isRaw() ? "**/" : ")");
        print(" ");
        accept(castExpression.getExpression());
        return false;
      }

      @Override
      public boolean enterCharacterLiteral(CharacterLiteral characterLiteral) {
        print(characterLiteral.getEscapedValue());
        return false;
      }

      @Override
      public boolean enterConditionalExpression(ConditionalExpression conditionalExpression) {
        accept(conditionalExpression.getConditionExpression());
        print(" ? ");
        accept(conditionalExpression.getTrueExpression());
        print(" : ");
        accept(conditionalExpression.getFalseExpression());
        return false;
      }

      @Override
      public boolean enterContinueStatement(ContinueStatement continueStatement) {
        print("continue");
        if (continueStatement.getLabelName() != null) {
          print(" ");
          print(continueStatement.getLabelName());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterDoWhileStatement(DoWhileStatement doWhileStatement) {
        print("do");
        accept(doWhileStatement.getBody());
        print(" while (");
        accept(doWhileStatement.getConditionExpression());
        print(");");
        return false;
      }

      @Override
      public boolean enterEmptyStatement(EmptyStatement emptyStatement) {
        print(";");
        return false;
      }

      @Override
      public boolean enterExpression(Expression expression) {
        print("<expr>");
        return false;
      }

      @Override
      public boolean enterExpressionStatement(ExpressionStatement expressionStatement) {
        accept(expressionStatement.getExpression());
        print(";");
        return false;
      }

      @Override
      public boolean enterFieldAccess(FieldAccess fieldAccess) {
        accept(fieldAccess.getQualifier());
        print(fieldAccess.getTarget().getFieldName());
        return false;
      }

      @Override
      public boolean enterForStatement(ForStatement forStatement) {
        print(" for (");
        printSeparatedExpressions(",", forStatement.getInitializers());
        print(";");
        accept(forStatement.getConditionExpression());
        print(";");
        printSeparatedExpressions(",", forStatement.getUpdates());
        print(") ");
        accept(forStatement.getBody());
        return false;
      }

      @Override
      public boolean enterIfStatement(IfStatement ifStatement) {
        print("if (");
        accept(ifStatement.getConditionExpression());
        print(") ");
        accept(ifStatement.getThenStatement());
        if (ifStatement.getElseStatement() != null) {
          print(" else ");
          accept(ifStatement.getElseStatement());
        }
        return false;
      }

      @Override
      public boolean enterInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
        accept(instanceOfExpression.getExpression());
        print(" instanceof ");
        print(instanceOfExpression.getTestTypeDescriptor());
        return false;
      }

      @Override
      public boolean enterJavaType(JavaType javaType) {
        print(javaType.getDescriptor().toString());
        return false;
      }

      @Override
      public boolean enterLabeledStatement(LabeledStatement labeledStatement) {
        print(labeledStatement.getLabelName());
        print(": ");
        accept(labeledStatement.getBody());
        return false;
      }

      @Override
      public boolean enterMethod(Method method) {
        print(method.getDescriptor().toString());
        return false;
      }

      @Override
      public boolean enterMethodCall(MethodCall methodCall) {
        accept(methodCall.qualifier);
        printInvocation(methodCall, "." + methodCall.getTarget().getMethodName());
        return false;
      }

      @Override
      public boolean enterMultiExpression(MultiExpression multiExpression) {
        print("(");
        printSeparatedExpressions(",", multiExpression.getExpressions());
        print(")");
        return false;
      }

      @Override
      public boolean enterNewArray(NewArray newArray) {
        print("new ");
        print(newArray.getLeafTypeDescriptor());
        List<Expression> dimensionExpressions = newArray.getDimensionExpressions();
        for (Expression expression : dimensionExpressions) {
          print("[");
          if (expression != NullLiteral.NULL) {
            accept(expression);
          }
          print("]");
        }
        if (newArray.getArrayLiteral() != null) {
          print(" {");
          printSeparatedExpressions(",", newArray.getArrayLiteral().getValueExpressions());
          print("}");
        }
        return false;
      }

      @Override
      public boolean enterNewInstance(NewInstance newInstance) {
        print(" new ");
        printInvocation(newInstance, newInstance.getTarget().getMethodName());
        return false;
      }

      @Override
      public boolean enterNode(Node node) {
        print("<node>");
        return false;
      }

      @Override
      public boolean enterNullLiteral(NullLiteral nullLiteral) {
        print("null");
        return false;
      }

      @Override
      public boolean enterNumberLiteral(NumberLiteral numberLiteral) {
        print(numberLiteral.getValue().toString());
        return false;
      }

      @Override
      public boolean enterPostfixExpression(PostfixExpression binaryExpression) {
        accept(binaryExpression.getOperand());
        print(binaryExpression.getOperator().getSymbol());
        return false;
      }

      @Override
      public boolean enterPrefixExpression(PrefixExpression binaryExpression) {
        print(binaryExpression.getOperator().getSymbol());
        accept(binaryExpression.getOperand());
        return false;
      }

      @Override
      public boolean enterReturnStatement(ReturnStatement returnStatement) {
        print("return");
        if (returnStatement.getExpression() != null) {
          print(" ");
          accept(returnStatement.getExpression());
        }
        print(";");
        return false;
      }

      @Override
      public boolean enterStatement(Statement statement) {
        print("<statement>");
        return false;
      }

      @Override
      public boolean enterStringLiteral(StringLiteral stringLiteral) {
        print(stringLiteral.getEscapedValue());
        return false;
      }

      @Override
      public boolean enterSuperReference(SuperReference superReference) {
        print("super");
        return false;
      }

      @Override
      public boolean enterSwitchCase(SwitchCase switchCase) {
        if (switchCase.getMatchExpression() != null) {
          print("case ");
          accept(switchCase.getMatchExpression());
        } else {
          print("default");
        }

        print(":");
        return false;
      }

      @Override
      public boolean enterSwitchStatement(SwitchStatement switchStatement) {
        print("switch (");
        accept(switchStatement.getSwitchExpression());
        print(") {");
        indent();
        indent();
        for (Statement statement : switchStatement.getBodyStatements()) {
          if (statement instanceof SwitchCase) {
            unIndent();
          }
          newLine();
          accept(statement);
          if (statement instanceof SwitchCase) {
            indent();
          }
        }
        unIndent();
        unIndent();
        newLine();
        print("}");
        return false;
      }

      @Override
      public boolean enterSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
        print("synchronized (");
        accept(synchronizedStatement.getExpression());
        print(");");
        accept(synchronizedStatement.getBody());
        return false;
      }

      @Override
      public boolean enterThisReference(ThisReference thisReference) {
        print("this");
        return false;
      }

      @Override
      public boolean enterThrowStatement(ThrowStatement throwStatement) {
        print("throw ");
        accept(throwStatement.getExpression());
        print(";");
        return false;
      }

      @Override
      public boolean enterTryStatement(TryStatement tryStatement) {
        print("try ");
        if (!tryStatement.getResourceDeclarations().isEmpty()) {
          print("(");
          indent();
          indent();
          newLine();
          printSeparatedExpressions(";\n", tryStatement.getResourceDeclarations());
          unIndent();
          unIndent();
          print(") ");
        }
        accept(tryStatement.getBody());
        for (CatchClause catchClause : tryStatement.getCatchClauses()) {
          accept(catchClause);
        }
        if (tryStatement.getFinallyBlock() != null) {
          print(" finally ");
          accept(tryStatement.getFinallyBlock());
        }
        return false;
      }

      @Override
      public boolean enterTypeReference(TypeReference typeReference) {
        print(typeReference.getReferencedTypeDescriptor().getSourceName());
        return false;
      }

      @Override
      public boolean enterVariableDeclarationFragment(
          VariableDeclarationFragment variableDeclarationFragment) {
        print(variableDeclarationFragment.getTypeDescriptor());
        print(" ");
        print(variableDeclarationFragment.getVariable().getName());
        if (variableDeclarationFragment.getInitializer() != null) {
          print(" = ");
          accept(variableDeclarationFragment.getInitializer());
        }
        return false;
      }

      @Override
      public boolean enterVariableDeclarationExpression(
          VariableDeclarationExpression variableDeclarationExpression) {
        String separator = "";
        for (VariableDeclarationFragment variableDeclarationFragment :
            variableDeclarationExpression.getFragments()) {
          print(separator);
          separator = ",";
          accept(variableDeclarationFragment);
        }
        return false;
      }

      @Override
      public boolean enterVariableReference(VariableReference variableReference) {
        print(variableReference.getTarget().getName());
        return false;
      }

      @Override
      public boolean enterWhileStatement(WhileStatement whileStatement) {
        print("while (");
        accept(whileStatement.getConditionExpression());
        print(");");
        accept(whileStatement.getBody());
        return false;
      }

      private void accept(Node node) {
        node.accept(this);
      }

      private void indent() {
        currentIndent++;
      }

      private void newLine() {
        print("\n");
      }

      private void print(String string) {
        result.append(string.replace("\n", "\n" + Strings.repeat("  ", currentIndent)));
      }

      private void print(TypeDescriptor typeDescriptor) {
        print(typeDescriptor.toString());
      }

      private void printSeparatedExpressions(
          String separator, List<? extends Expression> expressions) {
        String nextSeparator = "";
        for (Expression argument : expressions) {
          print(nextSeparator);
          nextSeparator = separator;
          accept(argument);
        }
      }

      private void printInvocation(Invocation invocation, String methodName) {
        print(methodName);
        print("(");
        printSeparatedExpressions(",", invocation.getArguments());
        print(")");
      }

      private void unIndent() {
        currentIndent--;
      }
    }.accept(node);
    return result.toString();
  }
}
