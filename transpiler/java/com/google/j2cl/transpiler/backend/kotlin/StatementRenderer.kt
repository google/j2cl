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

import com.google.j2cl.transpiler.ast.AssertStatement
import com.google.j2cl.transpiler.ast.Block
import com.google.j2cl.transpiler.ast.BreakStatement
import com.google.j2cl.transpiler.ast.ContinueStatement
import com.google.j2cl.transpiler.ast.DoWhileStatement
import com.google.j2cl.transpiler.ast.Expression
import com.google.j2cl.transpiler.ast.ExpressionStatement
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement
import com.google.j2cl.transpiler.ast.ForEachStatement
import com.google.j2cl.transpiler.ast.IfStatement
import com.google.j2cl.transpiler.ast.LabelReference
import com.google.j2cl.transpiler.ast.LabeledStatement
import com.google.j2cl.transpiler.ast.LocalClassDeclarationStatement
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.SwitchStatement
import com.google.j2cl.transpiler.ast.SynchronizedStatement
import com.google.j2cl.transpiler.ast.ThrowStatement
import com.google.j2cl.transpiler.ast.TryStatement
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.ast.WhileStatement

internal fun Renderer.renderStatements(statements: List<Statement>) {
  renderStartingWithNewLines(statements) { renderStatement(it) }
}

fun Renderer.renderStatement(statement: Statement) {
  when (statement) {
    is AssertStatement -> renderAssertStatement(statement)
    is Block -> renderBlock(statement)
    is BreakStatement -> renderBreakStatement(statement)
    is ContinueStatement -> renderContinueStatement(statement)
    is DoWhileStatement -> renderDoWhileStatement(statement)
    is ExpressionStatement -> renderExpressionStatement(statement)
    is FieldDeclarationStatement -> renderFieldDeclarationStatement(statement)
    is ForEachStatement -> renderForEachStatement(statement)
    is IfStatement -> renderIfStatement(statement)
    is LabeledStatement -> renderLabeledStatement(statement)
    is LocalClassDeclarationStatement -> renderLocalClassDeclarationStatement(statement)
    is ReturnStatement -> renderReturnStatement(statement)
    is SwitchStatement -> renderSwitchStatement(statement)
    is SynchronizedStatement -> renderSynchronizedStatement(statement)
    is WhileStatement -> renderWhileStatement(statement)
    is ThrowStatement -> renderThrowStatement(statement)
    is TryStatement -> renderTryStatement(statement)
    else -> renderTodo(statement::class.java.simpleName)
  }
}

private fun Renderer.renderAssertStatement(assertStatement: AssertStatement) {
  render(extensionMemberQualifiedNameSource("kotlin.assert"))
  renderInParentheses { renderExpression(assertStatement.expression) }
  assertStatement.message?.let {
    render(" ")
    renderInCurlyBrackets {
      renderNewLine()
      renderExpression(it)
    }
  }
}

private fun Renderer.renderBlock(block: Block) {
  renderInCurlyBrackets { renderStatements(block.statements) }
}

private fun Renderer.renderBreakStatement(breakStatement: BreakStatement) {
  render("break")
  breakStatement.labelReference?.let { renderLabelReference(it) }
}

private fun Renderer.renderContinueStatement(continueStatement: ContinueStatement) {
  render("continue")
  continueStatement.labelReference?.let { renderLabelReference(it) }
}

private fun Renderer.renderLabelReference(labelReference: LabelReference) {
  render("@")
  renderName(labelReference.target)
}

private fun Renderer.renderDoWhileStatement(doWhileStatement: DoWhileStatement) {
  render("do ")
  renderStatement(doWhileStatement.body)
  render(" while ")
  renderInParentheses { renderExpression(doWhileStatement.conditionExpression) }
}

private fun Renderer.renderExpressionStatement(expressionStatement: ExpressionStatement) {
  renderExpression(expressionStatement.expression)
}

private fun Renderer.renderForEachStatement(forEachStatement: ForEachStatement) {
  render("for ")
  renderInParentheses {
    renderName(forEachStatement.loopVariable)
    render(" in ")
    renderExpression(forEachStatement.iterableExpression)
  }
  render(" ")
  renderStatement(forEachStatement.body)
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
  render("var ")
  render(identifierSource(fieldDescriptor.name!!))
  if (!fieldDescriptor.typeDescriptor.isProtobufBuilder()) {
    render(": ")
    render(typeDescriptorSource(fieldDescriptor.typeDescriptor))
  }
  render(" = ")
  renderExpression(declaration.expression)
}

private fun Renderer.renderLabeledStatement(labelStatement: LabeledStatement) {
  renderName(labelStatement.label)
  render("@ ")
  val innerStatement = labelStatement.statement
  if (innerStatement is LabeledStatement) renderInCurlyBrackets { renderStatement(innerStatement) }
  else renderStatement(innerStatement)
}

private fun Renderer.renderLocalClassDeclarationStatement(
  localClassDeclarationStatement: LocalClassDeclarationStatement
) {
  renderType(localClassDeclarationStatement.localClass)
}

private fun Renderer.renderReturnStatement(returnStatement: ReturnStatement) {
  render("return")
  currentReturnLabelIdentifier?.let {
    render("@")
    render(identifierSource(it))
  }
  returnStatement.expression?.let {
    render(" ")
    renderExpression(it)
  }
}

private fun Renderer.renderSwitchStatement(switchStatement: SwitchStatement) {
  render("when ")
  renderInParentheses { renderExpression(switchStatement.switchExpression) }
  render(" ")
  renderInCurlyBrackets {
    val caseExpressions = mutableListOf<Expression>()
    for (case in switchStatement.cases) {
      val caseExpression = case.caseExpression
      if (caseExpression == null) {
        // It's OK to skip empty cases, since they will fall-through to the default case, and since
        // these are case clauses from Java, their evaluation does never have side effects.
        caseExpressions.clear()
        renderNewLine()
        render("else -> ")
        renderInCurlyBrackets { renderStatements(case.statements) }
      } else {
        caseExpressions.add(caseExpression)
        val caseStatements = case.statements
        if (caseStatements.isNotEmpty()) {
          renderNewLine()
          renderCommaSeparated(caseExpressions) { renderExpression(it) }
          caseExpressions.clear()
          render(" -> ")
          renderInCurlyBrackets { renderStatements(caseStatements) }
        }
      }
    }
  }
}

private fun Renderer.renderSynchronizedStatement(synchronizedStatement: SynchronizedStatement) {
  render(extensionMemberQualifiedNameSource("kotlin.synchronized"))
  renderInParentheses { renderExpression(synchronizedStatement.expression) }
  render(" ")
  renderBlock(synchronizedStatement.body)
}

private fun Renderer.renderWhileStatement(whileStatement: WhileStatement) {
  render("while ")
  renderInParentheses { renderExpression(whileStatement.conditionExpression) }
  render(" ")
  renderStatement(whileStatement.body)
}

private fun Renderer.renderThrowStatement(throwStatement: ThrowStatement) {
  render("throw ")
  renderExpression(throwStatement.expression)
}

private fun Renderer.renderTryStatement(tryStatement: TryStatement) {
  // Render try/catch/finally.
  render("try ")
  renderStatement(tryStatement.body)
  tryStatement.catchClauses.forEach { catchClause ->
    val catchVariable = catchClause.exceptionVariable
    // Duplicate catch block for each type in the union, which are not available in Kotlin.
    val catchTypeDescriptors =
      catchVariable.typeDescriptor.let {
        if (it is UnionTypeDescriptor) it.unionTypeDescriptors else listOf(it)
      }
    catchTypeDescriptors.forEach { catchType ->
      render(" catch ")
      renderInParentheses {
        renderName(catchVariable)
        render(": ")
        render(typeDescriptorSource(catchType.toNonNullable()))
      }
      render(" ")
      renderBlock(catchClause.body)
    }
  }
  tryStatement.finallyBlock?.let {
    render(" finally ")
    renderStatement(it)
  }
}
