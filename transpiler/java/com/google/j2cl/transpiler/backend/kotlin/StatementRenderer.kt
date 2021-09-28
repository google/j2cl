/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.Block
import com.google.j2cl.transpiler.ast.BreakStatement
import com.google.j2cl.transpiler.ast.ContinueStatement
import com.google.j2cl.transpiler.ast.DoWhileStatement
import com.google.j2cl.transpiler.ast.ExpressionStatement
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement
import com.google.j2cl.transpiler.ast.IfStatement
import com.google.j2cl.transpiler.ast.LabeledStatement
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.SwitchCase
import com.google.j2cl.transpiler.ast.SwitchStatement
import com.google.j2cl.transpiler.ast.WhileStatement

fun Renderer.renderStatement(statement: Statement) {
  when (statement) {
    is Block -> renderBlock(statement)
    is BreakStatement -> renderBreakStatement(statement)
    is ContinueStatement -> renderContinueStatement(statement)
    is DoWhileStatement -> renderDoWhileStatement(statement)
    is ExpressionStatement -> renderExpressionStatement(statement)
    is FieldDeclarationStatement -> renderFieldDeclarationStatement(statement)
    is IfStatement -> renderIfStatement(statement)
    is LabeledStatement -> renderLabeledStatement(statement)
    is ReturnStatement -> renderReturnStatement(statement)
    is SwitchStatement -> renderSwitchStatement(statement)
    is WhileStatement -> renderWhileStatement(statement)
    // TODO(micapolos): Handle remaining statement types.
    else -> throw InternalCompilerError("Unhandled ${statement::class}.")
  }
}

private fun Renderer.renderBlock(block: Block) {
  renderInCurlyBrackets { renderStartingWithNewLines(block.statements) { renderStatement(it) } }
}

private fun Renderer.renderBreakStatement(breakStatement: BreakStatement) {
  render("break")
  breakStatement.labelReference?.let { render(" @${breakStatement.labelReference.target.name}") }
}

private fun Renderer.renderContinueStatement(continueStatement: ContinueStatement) {
  render("continue")
  continueStatement.labelReference?.let {
    render(" @${continueStatement.labelReference.target.name}")
  }
}

private fun Renderer.renderDoWhileStatement(doWhileStatement: DoWhileStatement) {
  render("do ")
  renderStatement(doWhileStatement.body)
  render("while ")
  renderInParentheses { renderExpression(doWhileStatement.conditionExpression) }
}

private fun Renderer.renderExpressionStatement(expressionStatement: ExpressionStatement) {
  renderExpression(expressionStatement.expression)
}

private fun Renderer.renderIfStatement(ifStatement: IfStatement) {
  render("if ")
  renderInParentheses { renderExpression(ifStatement.conditionExpression) }
  render(" ")
  renderStatement(ifStatement.thenStatement)
  ifStatement.elseStatement?.let {
    render(" else ")
    renderStatement(it)
  }
}

private fun Renderer.renderFieldDeclarationStatement(declaration: FieldDeclarationStatement) {
  var fieldDescriptor = declaration.fieldDescriptor
  render("var ${fieldDescriptor.name}: ${fieldDescriptor.typeDescriptor.sourceString} = ")
  renderExpression(declaration.expression)
}

private fun Renderer.renderLabeledStatement(labelStatement: LabeledStatement) {
  render("${labelStatement.label.name}@ ")
  val innerStatement = labelStatement.statement
  if (innerStatement is LabeledStatement) renderInCurlyBrackets { renderStatement(innerStatement) }
  else renderStatement(innerStatement)
}

private fun Renderer.renderReturnStatement(returnStatement: ReturnStatement) {
  render("return ")
  returnStatement.expression.let { if (it != null) renderExpression(it) else render("Unit") }
}

private fun Renderer.renderSwitchCase(switchCase: SwitchCase) {
  if (switchCase.isDefault) render("else") else renderExpression(switchCase.caseExpression)
  render(" -> ")
  renderIndented { renderStartingWithNewLines(switchCase.statements) { renderStatement(it) } }
}

private fun Renderer.renderSwitchStatement(switchStatement: SwitchStatement) {
  // Note: assumes Java Switch-Case statements have been transformed into Kotlin
  // When-statements.
  render("when ")
  renderInParentheses { renderExpression(switchStatement.switchExpression) }
  renderInCurlyBrackets {
    renderStartingWithNewLines(switchStatement.cases) { renderSwitchCase(it) }
  }
}

private fun Renderer.renderWhileStatement(whileStatement: WhileStatement) {
  render("while ")
  renderInParentheses { renderExpression(whileStatement.conditionExpression) }
  renderStatement(whileStatement.body)
}
