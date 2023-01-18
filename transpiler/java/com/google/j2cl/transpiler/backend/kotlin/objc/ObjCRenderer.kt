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
package com.google.j2cl.transpiler.backend.kotlin.objc

import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.block
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource
import com.google.j2cl.transpiler.backend.kotlin.source.ifEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.plusComma
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

val empty: Renderer<Source>
  get() = rendererOf(emptySource)

private fun foundation(string: String): Renderer<Source> =
  source(string) rendererWith dependency(systemImport("Foundation/Foundation.h"))

val nsEnum: Renderer<Source> = foundation("NS_ENUM")
val nsInline: Renderer<Source> = foundation("NS_INLINE")

val id: Renderer<Source> = foundation("id")
val nsUInteger: Renderer<Source> = foundation("NSUInteger")
val nsCopying: Renderer<Source> = foundation("NSCopying")
val nsObject: Renderer<Source> = foundation("NSObject")
val nsNumber: Renderer<Source> = foundation("NSNumber")
val nsString: Renderer<Source> = foundation("NSString")

val nsMutableArray: Renderer<Source> = foundation("NSMutableArray")
val nsMutableSet: Renderer<Source> = foundation("NSMutableSet")
val nsMutableDictionary: Renderer<Source> = foundation("NSMutableDictionary")

private val ForwardDeclaration.nameRenderer: Renderer<Source>
  get() = source(name) rendererWith dependency(this)

fun className(name: String): Renderer<Source> = classForwardDeclaration(name).nameRenderer

fun protocolName(name: String): Renderer<Source> = protocolForwardDeclaration(name).nameRenderer

fun nsEnumTypedef(name: String, type: Renderer<Source>, values: List<String>): Renderer<Source> =
  map2(nsEnum, type) { nsEnumSource, typeSource ->
    semicolonEnded(
      spaceSeparated(
        source("typedef"),
        join(nsEnumSource, inRoundBrackets(commaSeparated(typeSource, source(name)))),
        block(
          newLineSeparated(
            values.mapIndexed { index, name ->
              infix(source(name), "=", source("$index")).plusComma
            }
          )
        )
      )
    )
  }

fun functionDeclaration(
  modifiers: List<Renderer<Source>> = listOf(),
  returnType: Renderer<Source>,
  name: String,
  parameters: List<Renderer<Source>> = listOf(),
  statements: List<Renderer<Source>> = listOf()
): Renderer<Source> =
  map4(modifiers.flatten, returnType, parameters.flatten, statements.flatten) {
    modifierSources,
    returnTypeSource,
    parameterSources,
    statementSources ->
    spaceSeparated(
      spaceSeparated(modifierSources),
      returnTypeSource,
      join(
        source(name),
        inRoundBrackets(commaSeparated(parameterSources).ifEmpty { source("void") })
      ),
      block(newLineSeparated(statementSources))
    )
  }

fun methodCall(
  target: Renderer<Source>,
  name: String,
  arguments: List<Renderer<Source>> = listOf()
): Renderer<Source> =
  map2(target, arguments.flatten) { targetSource, argumentSources ->
    inSquareBrackets(
      spaceSeparated(
        targetSource,
        if (argumentSources.isEmpty()) source(name)
        else
          spaceSeparated(
            name.dropLast(1).split(":").zip(argumentSources).map { (name, argumentSource) ->
              join(source(name), source(":"), argumentSource)
            }
          )
      )
    )
  }

fun block(statements: List<Renderer<Source>> = listOf()): Renderer<Source> =
  statements.flatten.map { block(newLineSeparated(it)) }

fun returnStatement(expression: Renderer<Source>): Renderer<Source> =
  expression.map { semicolonEnded(spaceSeparated(source("return"), it)) }
