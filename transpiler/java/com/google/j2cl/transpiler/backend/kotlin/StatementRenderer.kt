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
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.WhileStatement
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.ARROW_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.AT_OPERATOR
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.BREAK_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CATCH_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.CONTINUE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.DO_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.ELSE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.FINALLY_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.FOR_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.IF_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.IN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.RETURN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.THROW_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.TRY_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.WHEN_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.WHILE_KEYWORD
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.assignment
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.at
import com.google.j2cl.transpiler.backend.kotlin.KotlinSource.labelReference
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.infix
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.orEmpty

/**
 * Renderer of statements.
 *
 * @property nameRenderer underlying name renderer
 * @property enclosingType enclosing type
 * @property currentReturnLabelIdentifier optional label to render in return statements
 * @property renderThisReferenceWithLabel whether to render this reference with explicit qualifier
 */
internal data class StatementRenderer(
  val nameRenderer: NameRenderer,
  val enclosingType: Type,
  val currentReturnLabelIdentifier: String? = null,
  // TODO(b/252138814): Remove when KT-54349 is fixed
  val renderThisReferenceWithLabel: Boolean = false,
) {
  private val expressionRenderer: ExpressionRenderer
    get() =
      ExpressionRenderer(
        nameRenderer,
        enclosingType,
        renderThisReferenceWithLabel = renderThisReferenceWithLabel,
      )

  private val typeRenderer: TypeRenderer
    get() = TypeRenderer(nameRenderer)

  private fun expressionSource(expression: Expression): Source =
    expressionRenderer.expressionSource(expression)

  fun statementsSource(statements: List<Statement>): Source =
    newLineSeparated(statements.map(::statementSource))

  fun statementSource(statement: Statement): Source =
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
    }.withMapping(statement.sourcePosition)

  private fun assertStatementSource(assertStatement: AssertStatement): Source =
    spaceSeparated(
      join(
        nameRenderer.extensionMemberQualifiedNameSource("kotlin.assert"),
        inParentheses(expressionSource(assertStatement.expression)),
      ),
      assertStatement.message?.let { block(expressionSource(it)) }.orEmpty(),
    )

  private fun blockSource(block: Block): Source = block(statementsSource(block.statements))

  private fun breakStatementSource(breakStatement: BreakStatement): Source =
    join(BREAK_KEYWORD, breakStatement.labelReference?.let(::labelReferenceSource).orEmpty())

  private fun continueStatementSource(continueStatement: ContinueStatement): Source =
    join(CONTINUE_KEYWORD, continueStatement.labelReference?.let(::labelReferenceSource).orEmpty())

  private fun labelReferenceSource(labelReference: LabelReference): Source =
    at(nameRenderer.nameSource(labelReference.target))

  private fun doWhileStatementSource(doWhileStatement: DoWhileStatement): Source =
    spaceSeparated(
      DO_KEYWORD,
      statementSource(doWhileStatement.body),
      WHILE_KEYWORD,
      inParentheses(expressionSource(doWhileStatement.conditionExpression!!)),
    )

  private fun expressionStatementSource(expressionStatement: ExpressionStatement): Source =
    expressionSource(expressionStatement.expression)

  private fun forEachStatementSource(forEachStatement: ForEachStatement): Source =
    spaceSeparated(
      FOR_KEYWORD,
      inParentheses(
        infix(
          nameRenderer.nameSource(forEachStatement.loopVariable),
          IN_KEYWORD,
          expressionSource(forEachStatement.iterableExpression),
        )
      ),
      statementSource(forEachStatement.body),
    )

  private fun ifStatementSource(ifStatement: IfStatement): Source =
    spaceSeparated(
      IF_KEYWORD,
      inParentheses(expressionSource(ifStatement.conditionExpression)),
      statementSource(ifStatement.thenStatement),
      ifStatement.elseStatement?.let { spaceSeparated(ELSE_KEYWORD, statementSource(it)) }.orEmpty(),
    )

  private fun fieldDeclarationStatementSource(declaration: FieldDeclarationStatement): Source =
    declaration.fieldDescriptor.let { fieldDescriptor ->
      spaceSeparated(
        source("var"),
        assignment(
          colonSeparated(
            identifierSource(fieldDescriptor.name!!),
            nameRenderer.typeDescriptorSource(fieldDescriptor.typeDescriptor),
          ),
          expressionSource(declaration.expression),
        ),
      )
    }

  private fun labeledStatementSource(labelStatement: LabeledStatement): Source =
    spaceSeparated(
      join(nameRenderer.nameSource(labelStatement.label), AT_OPERATOR),
      labelStatement.statement.let {
        statementSource(it).letIf(it is LabeledStatement) { block(it) }
      },
    )

  private fun localClassDeclarationStatementSource(
    localClassDeclarationStatement: LocalClassDeclarationStatement
  ): Source = typeRenderer.typeSource(localClassDeclarationStatement.localClass)

  private fun returnStatementSource(returnStatement: ReturnStatement): Source =
    spaceSeparated(
      join(RETURN_KEYWORD, currentReturnLabelIdentifier?.let { labelReference(it) }.orEmpty()),
      returnStatement.expression?.let(::expressionSource).orEmpty(),
    )

  private fun switchStatementSource(switchStatement: SwitchStatement): Source =
    spaceSeparated(
      WHEN_KEYWORD,
      inParentheses(expressionSource(switchStatement.switchExpression)),
      block(
        newLineSeparated(
          // TODO(b/263161219): Represent WhenStatement as a data class, convert from
          // SwitchStatement
          // and render as Source.
          run {
            val caseExpressions = mutableListOf<Expression>()
            switchStatement.cases.map { case ->
              val caseExpression = case.caseExpression
              if (caseExpression == null) {
                // It's OK to skip empty cases, since they will fall-through to the default case,
                // and
                // since
                // these are case clauses from Java, their evaluation does never have side effects.
                caseExpressions.clear()
                infix(ELSE_KEYWORD, ARROW_OPERATOR, block(statementsSource(case.statements)))
              } else {
                caseExpressions.add(caseExpression)
                val caseStatements = case.statements
                if (caseStatements.isNotEmpty()) {
                  infix(
                      commaSeparated(caseExpressions.map(::expressionSource)),
                      ARROW_OPERATOR,
                      block(statementsSource(caseStatements)),
                    )
                    .also { caseExpressions.clear() }
                } else {
                  Source.EMPTY
                }
              }
            }
          }
        )
      ),
    )

  private fun synchronizedStatementSource(synchronizedStatement: SynchronizedStatement): Source =
    spaceSeparated(
      join(
        nameRenderer.extensionMemberQualifiedNameSource("kotlin.synchronized"),
        inAngleBrackets(nameRenderer.topLevelQualifiedNameSource("kotlin.Unit")),
        inParentheses(expressionSource(synchronizedStatement.expression)),
      ),
      statementSource(synchronizedStatement.body),
    )

  private fun whileStatementSource(whileStatement: WhileStatement): Source =
    spaceSeparated(
      WHILE_KEYWORD,
      inParentheses(expressionSource(whileStatement.conditionExpression!!)),
      statementSource(whileStatement.body),
    )

  private fun throwStatementSource(throwStatement: ThrowStatement): Source =
    spaceSeparated(THROW_KEYWORD, expressionSource(throwStatement.expression))

  private fun tryStatementSource(tryStatement: TryStatement): Source =
    spaceSeparated(
      TRY_KEYWORD,
      statementSource(tryStatement.body),
      spaceSeparated(tryStatement.catchClauses.map(::catchClauseSource)),
      tryStatement.finallyBlock
        ?.let { spaceSeparated(FINALLY_KEYWORD, statementSource(it)) }
        .orEmpty(),
    )

  private fun catchClauseSource(catchClause: CatchClause): Source =
    spaceSeparated(
      // Duplicate catch block for each type in the union, which are not available in Kotlin.
      catchClause.exceptionVariable.typeDescriptor.catchTypeDescriptors.map {
        catchClauseSource(catchClause.exceptionVariable, it, catchClause.body)
      }
    )

  private fun catchClauseSource(variable: Variable, type: TypeDescriptor, body: Block): Source =
    spaceSeparated(
      CATCH_KEYWORD,
      inParentheses(
        colonSeparated(
          nameRenderer.nameSource(variable),
          nameRenderer.typeDescriptorSource(type.toNonNullable()),
        )
      ),
      blockSource(body),
    )

  companion object {
    private val TypeDescriptor.catchTypeDescriptors
      get() =
        if (this is UnionTypeDescriptor) {
          unionTypeDescriptors
        } else {
          listOf(this)
        }
  }
}
