/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.backend.kotlin.ast.Visibility
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.COLON
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaAndNewLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inNewLine
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParenthesesIfNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.indented
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.infix
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated

/** Kotlin related functions dealing with [Source]s. */
internal object KotlinSource {
  val AND_OPERATOR = source("&&")
  val ARROW_OPERATOR = source("->")
  val ASSIGN_OPERATOR = source("=")
  val AT_OPERATOR = source("@")
  val DECREMENT_OPERATOR = source("--")
  val DIVIDE_OPERATOR = source("/")
  val DOUBLE_COLON_OPERATOR = source("::")
  val EQUAL_OPERATOR = source("==")
  val GREATER_OPERATOR = source(">")
  val GREATER_EQUAL_OPERATOR = source(">=")
  val INCREMENT_OPERATOR = source("++")
  val INTERSECTION_OPERATOR = source("&")
  val LESS_OPERATOR = source("<")
  val LESS_EQUAL_OPERATOR = source("<=")
  val MINUS_OPERATOR = source("-")
  val NEGATE_OPERATOR = source("!")
  val NOT_EQUAL_OPERATOR = source("!=")
  val NOT_NULL_OPERATOR = source("!!")
  val NOT_SAME_OPERATOR = source("!==")
  val NULLABLE_OPERATOR = source("?")
  val OR_OPERATOR = source("||")
  val PLUS_OPERATOR = source("+")
  val REMAINDER_OPERATOR = source("%")
  val SAME_OPERATOR = source("===")
  val STAR_OPERATOR = source("*")
  val TIMES_OPERATOR = source("*")

  val ABSTRACT_KEYWORD = source("abstract")
  val AS_KEYWORD = source("as")
  val BREAK_KEYWORD = source("break")
  val CAPTURE_KEYWORD = source("capture")
  val CATCH_KEYWORD = source("catch")
  val CLASS_KEYWORD = source("class")
  val COMPANION_KEYWORD = source("companion")
  val CONTINUE_KEYWORD = source("continue")
  val CONST_KEYWORD = source("const")
  val CONSTRUCTOR_KEYWORD = source("constructor")
  val DO_KEYWORD = source("do")
  val ELSE_KEYWORD = source("else")
  val ENUM_KEYWORD = source("enum")
  val EXTERNAL_KEYWORD = source("external")
  val FINAL_KEYWORD = source("final")
  val FINALLY_KEYWORD = source("finally")
  val FILE_KEYWORD = source("file")
  val FOR_KEYWORD = source("for")
  val FUN_KEYWORD = source("fun")
  val GET_KEYWORD = source("get")
  val IF_KEYWORD = source("if")
  val IMPORT_KEYWORD = source("import")
  val IN_KEYWORD = source("in")
  val INIT_KEYWORD = source("init")
  val INNER_KEYWORD = source("inner")
  val INTERFACE_KEYWORD = source("interface")
  val INTERNAL_KEYWORD = source("internal")
  val IS_KEYWORD = source("is")
  val IT_KEYWORD = source("it")
  val LATEINIT_KEYWORD = source("lateinit")
  val NATIVE_KEYWORD = source("native")
  val NULL_KEYWORD = source("null")
  val OBJECT_KEYWORD = source("object")
  val OF_KEYWORD = source("of")
  val OPEN_KEYWORD = source("open")
  val OUT_KEYWORD = source("out")
  val OVERRIDE_KEYWORD = source("override")
  val PACKAGE_KEYWORD = source("package")
  val PRIVATE_KEYWORD = source("private")
  val PROTECTED_KEYWORD = source("protected")
  val PUBLIC_KEYWORD = source("public")
  val RETURN_KEYWORD = source("return")
  val SUPER_KEYWORD = source("super")
  val THIS_KEYWORD = source("this")
  val THROW_KEYWORD = source("throw")
  val TRY_KEYWORD = source("try")
  val VAL_KEYWORD = source("val")
  val VAR_KEYWORD = source("var")
  val VARARG_KEYWORD = source("vararg")
  val WHEN_KEYWORD = source("when")
  val WHERE_KEYWORD = source("where")
  val WHILE_KEYWORD = source("while")

  val SIZE_IDENTIFIER = source("size")
  val TODO_IDENTIFIER = source("TODO")

  fun literal(it: Boolean): Source = source("$it")

  fun literal(it: Char): Source = source(it.literalString)

  fun literal(it: String): Source = source(it.literalString)

  fun literal(it: Int): Source = source("$it")

  fun literal(it: Long): Source = source("${it}L")

  fun literal(it: Float): Source =
    if (it.isNaN()) inParentheses(infix(literal(0f), DIVIDE_OPERATOR, literal(0f)))
    else
      when (it) {
        Float.NEGATIVE_INFINITY -> inParentheses(infix(literal(-1f), DIVIDE_OPERATOR, literal(0f)))
        Float.POSITIVE_INFINITY -> inParentheses(infix(literal(1f), DIVIDE_OPERATOR, literal(0f)))
        else -> source("${it}f")
      }

  fun literal(it: Double): Source =
    if (it.isNaN()) inParentheses(infix(literal(0.0), DIVIDE_OPERATOR, literal(0.0)))
    else
      when (it) {
        Double.NEGATIVE_INFINITY ->
          inParentheses(infix(literal(-1.0), DIVIDE_OPERATOR, literal(0.0)))
        Double.POSITIVE_INFINITY ->
          inParentheses(infix(literal(1.0), DIVIDE_OPERATOR, literal(0.0)))
        else -> source("$it")
      }

  fun assignment(lhs: Source, rhs: Source): Source = infix(lhs, ASSIGN_OPERATOR, rhs)

  fun initializer(expr: Source): Source = expr.ifNotEmpty { spaceSeparated(ASSIGN_OPERATOR, expr) }

  fun at(source: Source): Source = join(AT_OPERATOR, source)

  fun labelReference(name: String): Source = at(identifierSource(name))

  fun classLiteral(type: Source): Source = join(type, DOUBLE_COLON_OPERATOR, CLASS_KEYWORD)

  fun nonNull(type: Source): Source = join(type, NOT_NULL_OPERATOR)

  fun asExpression(lhs: Source, rhs: Source): Source = infix(lhs, AS_KEYWORD, rhs)

  fun isExpression(lhs: Source, rhs: Source): Source = infix(lhs, IS_KEYWORD, rhs)

  fun todo(source: Source): Source = join(TODO_IDENTIFIER, inParentheses(source))

  fun annotation(name: Source): Source = join(at(name))

  fun annotation(name: Source, parameter: Source, vararg parameters: Source): Source =
    join(at(name), inParenthesesIfNotEmpty(commaSeparated(parameter, *parameters)))

  fun annotation(name: Source, parameters: List<Source>) =
    join(
      at(name),
      inParentheses(
        if (parameters.size <= 2) {
          commaSeparated(parameters)
        } else {
          indented(inNewLine(commaAndNewLineSeparated(parameters)))
        }
      ),
    )

  fun fileAnnotation(name: Source, parameters: List<Source>): Source =
    annotation(join(FILE_KEYWORD, COLON, name), parameters)

  fun blockComment(source: Source): Source = spaceSeparated(source("/*"), source, source("*/"))
}

internal val Visibility.source: Source
  get() =
    when (this) {
      Visibility.PUBLIC -> KotlinSource.PUBLIC_KEYWORD
      Visibility.PROTECTED -> KotlinSource.PROTECTED_KEYWORD
      Visibility.INTERNAL -> KotlinSource.INTERNAL_KEYWORD
      Visibility.PRIVATE -> KotlinSource.PRIVATE_KEYWORD
    }
