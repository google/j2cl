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
import com.google.j2cl.transpiler.ast.AssertStatement
import com.google.j2cl.transpiler.ast.Block
import com.google.j2cl.transpiler.ast.BreakStatement
import com.google.j2cl.transpiler.ast.CatchClause
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
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.WhileStatement
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.block
import com.google.j2cl.transpiler.backend.kotlin.source.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotNullSource
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

internal fun Renderer.statementsSource(statements: List<Statement>): Source =
  newLineSeparated(statements.map(::statementSource))

fun Renderer.statementSource(statement: Statement): Source =
  when (statement) {
    is AssertStatement -> assertStatementSource(statement)
    is Block -> blockSource(statement)
    is BreakStatement -> breakStatementSource(statement)
    is ContinueStatement -> continueStatementSource(statement)
    is DoWhileStatement -> doWhileStatementSource(statement)
    is ExpressionStatement -> expressionStatementSource(statement)
    is FieldDeclarationStatement -> fieldDeclarationStatementSource(statement)
    is ForEachStatement -> forEachStatementSource(statement)
    is IfStatement -> ifStatementSource(statement)
    is LabeledStatement -> labeledStatementSource(statement)
    is LocalClassDeclarationStatement -> localClassDeclarationStatementSource(statement)
    is ReturnStatement -> returnStatementSource(statement)
    is SwitchStatement -> switchStatementSource(statement)
    is SynchronizedStatement -> synchronizedStatementSource(statement)
    is WhileStatement -> whileStatementSource(statement)
    is ThrowStatement -> throwStatementSource(statement)
    is TryStatement -> tryStatementSource(statement)
    else -> throw InternalCompilerError("Unexpected ${statement::class.java.simpleName}")
  }

private fun Renderer.assertStatementSource(assertStatement: AssertStatement): Source =
  spaceSeparated(
    join(
      extensionMemberQualifiedNameSource("kotlin.assert"),
      inRoundBrackets(expressionSource(assertStatement.expression))
    ),
    assertStatement.message.ifNotNullSource { block(expressionSource(it)) }
  )

private fun Renderer.blockSource(block: Block): Source = block(statementsSource(block.statements))

private fun Renderer.breakStatementSource(breakStatement: BreakStatement): Source =
  join(source("break"), breakStatement.labelReference.ifNotNullSource(::labelReferenceSource))

private fun Renderer.continueStatementSource(continueStatement: ContinueStatement): Source =
  join(source("continue"), continueStatement.labelReference.ifNotNullSource(::labelReferenceSource))

private fun Renderer.labelReferenceSource(labelReference: LabelReference): Source =
  at(nameSource(labelReference.target))

private fun Renderer.doWhileStatementSource(doWhileStatement: DoWhileStatement): Source =
  spaceSeparated(
    source("do"),
    statementSource(doWhileStatement.body),
    source("while"),
    inRoundBrackets(expressionSource(doWhileStatement.conditionExpression))
  )

private fun Renderer.expressionStatementSource(expressionStatement: ExpressionStatement): Source =
  expressionSource(expressionStatement.expression)

private fun Renderer.forEachStatementSource(forEachStatement: ForEachStatement): Source =
  spaceSeparated(
    source("for"),
    inRoundBrackets(
      infix(
        nameSource(forEachStatement.loopVariable),
        "in",
        expressionSource(forEachStatement.iterableExpression)
      )
    ),
    statementSource(forEachStatement.body)
  )

private fun Renderer.ifStatementSource(ifStatement: IfStatement): Source =
  spaceSeparated(
    source("if"),
    inRoundBrackets(expressionSource(ifStatement.conditionExpression)),
    statementSource(ifStatement.thenStatement),
    ifStatement.elseStatement.ifNotNullSource {
      spaceSeparated(source("else"), statementSource(it))
    }
  )

private fun Renderer.fieldDeclarationStatementSource(
  declaration: FieldDeclarationStatement
): Source =
  declaration.fieldDescriptor.let { fieldDescriptor ->
    spaceSeparated(
      source("var"),
      assignment(
        colonSeparated(
          identifierSource(fieldDescriptor.name!!),
          typeDescriptorSource(fieldDescriptor.typeDescriptor)
        ),
        expressionSource(declaration.expression)
      )
    )
  }

private fun Renderer.labeledStatementSource(labelStatement: LabeledStatement): Source =
  spaceSeparated(
    join(nameSource(labelStatement.label), source("@")),
    labelStatement.statement.let { statementSource(it).letIf(it is LabeledStatement, ::block) }
  )

private fun Renderer.localClassDeclarationStatementSource(
  localClassDeclarationStatement: LocalClassDeclarationStatement
): Source = typeSource(localClassDeclarationStatement.localClass)

private fun Renderer.returnStatementSource(returnStatement: ReturnStatement): Source =
  spaceSeparated(
    join(source("return"), currentReturnLabelIdentifier.ifNotNullSource(::labelReference)),
    returnStatement.expression.ifNotNullSource(::expressionSource)
  )

private fun Renderer.switchStatementSource(switchStatement: SwitchStatement): Source =
  spaceSeparated(
    source("when"),
    inRoundBrackets(expressionSource(switchStatement.switchExpression)),
    block(
      newLineSeparated(
        // TODO(b/263161219): Represent WhenStatement as a data class, convert from SwitchStatement
        // and render as Source.
        run {
          val caseExpressions = mutableListOf<Expression>()
          switchStatement.cases.map { case ->
            val caseExpression = case.caseExpression
            if (caseExpression == null) {
              // It's OK to skip empty cases, since they will fall-through to the default case, and
              // since
              // these are case clauses from Java, their evaluation does never have side effects.
              caseExpressions.clear()
              infix(source("else"), "->", block(statementsSource(case.statements)))
            } else {
              caseExpressions.add(caseExpression)
              val caseStatements = case.statements
              if (caseStatements.isNotEmpty()) {
                infix(
                    commaSeparated(caseExpressions.map(::expressionSource)),
                    "->",
                    block(statementsSource(caseStatements))
                  )
                  .also { caseExpressions.clear() }
              } else {
                emptySource
              }
            }
          }
        }
      )
    )
  )

private fun Renderer.synchronizedStatementSource(
  synchronizedStatement: SynchronizedStatement
): Source =
  spaceSeparated(
    join(
      extensionMemberQualifiedNameSource("kotlin.synchronized"),
      inRoundBrackets(expressionSource(synchronizedStatement.expression))
    ),
    statementSource(synchronizedStatement.body)
  )

private fun Renderer.whileStatementSource(whileStatement: WhileStatement): Source =
  spaceSeparated(
    source("while"),
    inRoundBrackets(expressionSource(whileStatement.conditionExpression)),
    statementSource(whileStatement.body)
  )

private fun Renderer.throwStatementSource(throwStatement: ThrowStatement): Source =
  spaceSeparated(source("throw"), expressionSource(throwStatement.expression))

private fun Renderer.tryStatementSource(tryStatement: TryStatement): Source =
  spaceSeparated(
    source("try"),
    statementSource(tryStatement.body),
    spaceSeparated(tryStatement.catchClauses.map(::catchClauseSource)),
    tryStatement.finallyBlock.ifNotNullSource {
      spaceSeparated(source("finally"), statementSource(it))
    }
  )

private val TypeDescriptor.catchTypeDescriptors
  get() = if (this is UnionTypeDescriptor) unionTypeDescriptors else listOf(this)

private fun Renderer.catchClauseSource(catchClause: CatchClause): Source =
  spaceSeparated(
    // Duplicate catch block for each type in the union, which are not available in Kotlin.
    catchClause.exceptionVariable.typeDescriptor.catchTypeDescriptors.map {
      catchClauseSource(catchClause.exceptionVariable, it, catchClause.body)
    }
  )

private fun Renderer.catchClauseSource(
  variable: Variable,
  type: TypeDescriptor,
  body: Block
): Source =
  spaceSeparated(
    source("catch"),
    inRoundBrackets(
      colonSeparated(nameSource(variable), typeDescriptorSource(type.toNonNullable()))
    ),
    blockSource(body)
  )
