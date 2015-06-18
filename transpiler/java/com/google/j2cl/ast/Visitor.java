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
 * A general Visitor class to traverse/modify the AST.
 */
public class Visitor {

  public boolean enterJavaType(JavaType type) {
    return enterNode(type);
  }

  public void exitJavaType(JavaType type) {
    exitNode(type);
  }

  public void exitMethod(Method method) {
    exitNode(method);
  }

  public void exitNode(Node node) {}

  public boolean enterNode(Node node) {
    return true;
  }

  public boolean enterMethod(Method method) {
    return enterNode(method);
  }

  public boolean enterCompilationUnit(CompilationUnit compilationUnit) {
    return enterNode(compilationUnit);
  }

  public void exitCompilationUnit(CompilationUnit compilationUnit) {
    exitNode(compilationUnit);
  }

  public boolean enterTypeReference(TypeReference typeReference) {
    return enterNode(typeReference);
  }

  public void exitTypeReference(TypeReference typeReference) {
    exitNode(typeReference);
  }

  public boolean enterAssertStatement(AssertStatement assertStatement) {
    return enterNode(assertStatement);
  }

  public void exitAssertStatement(AssertStatement assertStatement) {
    exitNode(assertStatement);
  }

  public boolean enterExpression(Expression expression) {
    return enterNode(expression);
  }

  public void exitExpression(Expression expression) {
    exitNode(expression);
  }

  public boolean enterBlock(Block block) {
    return enterNode(block);
  }

  public void exitBlock(Block block) {
    exitNode(block);
  }

  public boolean enterStatement(Statement statement) {
    return enterNode(statement);
  }

  public void exitStatement(Statement statement) {
    exitNode(statement);
  }
}
