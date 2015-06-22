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
package com.google.j2cl.ast;

/**
 * A general Visitor intefeace to traverse/modify the AST.
 */
interface Visitor {
  boolean enterArrayTypeReference(ArrayTypeReference arrayTypeReference);

  boolean enterAssertStatement(AssertStatement assertStatement);

  boolean enterBinaryExpression(BinaryExpression node);

  boolean enterBlock(Block block);

  boolean enterCompilationUnit(CompilationUnit compilationUnit);

  boolean enterExpression(Expression expression);

  boolean enterExpressionStatement(ExpressionStatement node);

  boolean enterField(Field field);

  boolean enterFieldReference(FieldReference fieldReference);

  boolean enterInstanceOfExpression(InstanceOfExpression instanceofExpression);

  boolean enterJavaType(JavaType type);

  boolean enterMethod(Method method);

  boolean enterMethodReference(MethodReference methodReference);

  boolean enterNode(Node node);

  boolean enterNewInstance(NewInstance node);

  boolean enterNumberLiteral(NumberLiteral node);

  boolean enterParenthesizedExpression(ParenthesizedExpression node);

  boolean enterPostfixExpression(PostfixExpression node);

  boolean enterPrefixExpression(PrefixExpression node);

  boolean enterRegularTypeReference(RegularTypeReference regularTypeReference);

  boolean enterStatement(Statement statement);

  boolean enterTypeReference(TypeReference typeReference);

  boolean enterVariable(Variable node);

  boolean enterVariableDeclaration(VariableDeclaration node);

  boolean enterVariableReference(VariableReference variableRefernce);

  void exitArrayTypeReference(ArrayTypeReference typeReference);

  void exitAssertStatement(AssertStatement assertStatement);

  void exitBinaryExpression(BinaryExpression node);

  void exitBlock(Block block);

  void exitCompilationUnit(CompilationUnit compilationUnit);

  void exitExpression(Expression expression);

  void exitExpressionStatement(ExpressionStatement node);

  void exitField(Field field);

  void exitFieldReference(FieldReference fieldReference);

  void exitInstanceOfExpression(InstanceOfExpression instanceofExpression);

  void exitJavaType(JavaType type);

  void exitMethod(Method method);

  void exitMethodReference(MethodReference methodReference);

  void exitNewInstance(NewInstance node);

  void exitNode(Node node);

  void exitNumberLiteral(NumberLiteral node);

  void exitParenthesizedExpression(ParenthesizedExpression node);

  void exitPostfixExpression(PostfixExpression node);

  void exitPrefixExpression(PrefixExpression node);

  void exitRegularTypeReference(RegularTypeReference typeReference);

  void exitStatement(Statement statement);

  void exitTypeReference(TypeReference typeReference);

  void exitVariable(Variable node);

  void exitVariableDeclaration(VariableDeclaration node);

  void exitVariableReference(VariableReference variableReference);
}
