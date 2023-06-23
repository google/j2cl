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

import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.commaAndNewLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.inNewLine
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.indented
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.source

fun literalSource(it: Boolean): Source = source("$it")

fun literalSource(it: Char): Source = source(it.literalString)

fun literalSource(it: String): Source = source(it.literalString)

fun literalSource(it: Int): Source = source("$it")

fun literalSource(it: Long): Source =
  when (it) {
    // Long.MIN_VALUE can not be represented as a literal in Kotlin.
    Long.MIN_VALUE -> inRoundBrackets(infix(literalSource(Long.MAX_VALUE), "+", literalSource(1L)))
    else -> source("${it}L")
  }

fun literalSource(it: Float): Source =
  if (it.isNaN()) inRoundBrackets(infix(literalSource(0f), "/", literalSource(0f)))
  else
    when (it) {
      Float.NEGATIVE_INFINITY -> inRoundBrackets(infix(literalSource(-1f), "/", literalSource(0f)))
      Float.POSITIVE_INFINITY -> inRoundBrackets(infix(literalSource(1f), "/", literalSource(0f)))
      else -> source("${it}f")
    }

fun literalSource(it: Double): Source =
  if (it.isNaN()) inRoundBrackets(infix(literalSource(0.0), "/", literalSource(0.0)))
  else
    when (it) {
      Double.NEGATIVE_INFINITY ->
        inRoundBrackets(infix(literalSource(-1.0), "/", literalSource(0.0)))
      Double.POSITIVE_INFINITY ->
        inRoundBrackets(infix(literalSource(1.0), "/", literalSource(0.0)))
      else -> source("$it")
    }

fun assignment(lhs: Source, rhs: Source): Source = infix(lhs, "=", rhs)

fun at(source: Source) = join(source("@"), source)

fun labelReference(name: String) = at(identifierSource(name))

fun Source.functionCall(name: String, vararg args: Source) =
  dotSeparated(this, join(source(name), inRoundBrackets(commaSeparated(args.toList()))))

fun classLiteral(type: Source) = join(type, source("::class"))

fun nonNull(type: Source) = join(type, source("!!"))

fun asExpression(lhs: Source, rhs: Source) = infix(lhs, "as", rhs)

fun isExpression(lhs: Source, rhs: Source) = infix(lhs, "is", rhs)

fun itSource() = source("it")

fun todo(source: Source) = join(source("TODO"), inRoundBrackets(source))

fun annotation(name: Source) = join(at(name))

fun annotation(name: Source, parameter: Source, vararg parameters: Source) =
  join(at(name), inRoundBrackets(commaSeparated(parameter, *parameters)))

fun annotation(name: Source, parameters: List<Source>) =
  join(
    at(name),
    inRoundBrackets(
      if (parameters.size <= 2) commaSeparated(parameters)
      else indented(inNewLine(commaAndNewLineSeparated(parameters)))
    )
  )

fun fileAnnotation(name: Source, parameters: List<Source>) =
  annotation(join(source("file:"), name), parameters)
