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

import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.flatten
import com.google.j2cl.transpiler.backend.kotlin.objc.Renderer.Companion.rendererOf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.block
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inParentheses
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.join
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.source
import com.google.j2cl.transpiler.backend.kotlin.source.Source.Companion.spaceSeparated

fun sourceRenderer(string: String): Renderer<Source> = rendererOf(source(string))

fun Renderer<Source>.ifEmpty(fn: () -> Renderer<Source>): Renderer<Source> = bind { source ->
  if (source.isEmpty()) fn() else rendererOf(source)
}

fun Renderer<Source>.ifNotEmpty(fn: (Renderer<Source>) -> Renderer<Source>): Renderer<Source> =
  bind { source ->
    if (source.isNotEmpty()) fn(rendererOf(source)) else rendererOf(source)
  }

operator fun Renderer<Source>.plus(renderer: Renderer<Source>): Renderer<Source> =
  combine(this, renderer) { lhs, rhs -> lhs + rhs }

operator fun Renderer<Source>.plus(source: Source): Renderer<Source> = plus(rendererOf(source))

fun Renderer<Source>.plusSemicolon(): Renderer<Source> = plus(Source.SEMICOLON)

fun Renderer<Source>.plusComma(): Renderer<Source> = plus(Source.COMMA)

fun Renderer<Source>.toPointer() = plus(Source.STAR)

fun Renderer<Source>.plusAssignment(rhs: Renderer<Source>): Renderer<Source> =
  spaceSeparated(this, sourceRenderer("="), rhs)

fun parameter(name: Renderer<Source>, value: Renderer<Source>): Renderer<Source> =
  join(name, rendererOf(Source.COLON), value)

fun block(renderer: Renderer<Source>): Renderer<Source> = renderer.map { block(it) }

fun inParentheses(renderer: Renderer<Source>): Renderer<Source> = renderer.map { inParentheses(it) }

fun inSquareBrackets(renderer: Renderer<Source>): Renderer<Source> =
  renderer.map { inSquareBrackets(it) }

fun join(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).flatten().map { join(it) }

fun Iterable<Renderer<Source>>.newLineSeparated(): Renderer<Source> =
  flatten().map { newLineSeparated(it) }

fun Iterable<Renderer<Source>>.spaceSeparated(): Renderer<Source> =
  flatten().map { spaceSeparated(it) }

fun Iterable<Renderer<Source>>.commaSeparated(): Renderer<Source> =
  flatten().map { commaSeparated(it) }

fun newLineSeparated(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).newLineSeparated()

fun spaceSeparated(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).flatten().map { spaceSeparated(it) }

fun emptyLineSeparated(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).flatten().map { emptyLineSeparated(it) }

fun commaSeparated(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).flatten().map { commaSeparated(it) }

fun dotSeparated(first: Renderer<Source>, vararg rest: Renderer<Source>): Renderer<Source> =
  listOf(first, *rest).flatten().map { dotSeparated(it) }

fun nsObjCRuntimeSourceRenderer(string: String): Renderer<Source> =
  sourceRenderer(string) with nsObjCRuntimeDependency

val nsObjCRuntimeDependency = Dependency.of(Import.system("Foundation/NSObjCRuntime.h"))

val nsEnum: Renderer<Source> = nsObjCRuntimeSourceRenderer("NS_ENUM")

val nsInline: Renderer<Source> = nsObjCRuntimeSourceRenderer("NS_INLINE")

val nsAssumeNonnullBegin: Renderer<Source> = nsObjCRuntimeSourceRenderer("NS_ASSUME_NONNULL_BEGIN")

val nsAssumeNonnullEnd: Renderer<Source> = nsObjCRuntimeSourceRenderer("NS_ASSUME_NONNULL_END")

val nullable: Renderer<Source> = sourceRenderer("_Nullable") // clang/gcc attribute

val id: Renderer<Source> = nsObjCRuntimeSourceRenderer("id")

val nsCopying: Renderer<Source> = ForwardDeclaration.ofProtocol("NSCopying").nameRenderer

val nsObject: Renderer<Source> = ForwardDeclaration.ofClass("NSObject").nameRenderer

val nsNumber: Renderer<Source> = ForwardDeclaration.ofClass("NSNumber").nameRenderer

val nsString: Renderer<Source> = ForwardDeclaration.ofClass("NSString").nameRenderer

val nsMutableArray: Renderer<Source> = ForwardDeclaration.ofClass("NSMutableArray").nameRenderer

val nsMutableSet: Renderer<Source> = ForwardDeclaration.ofClass("NSMutableSet").nameRenderer

val nsMutableDictionary: Renderer<Source> =
  ForwardDeclaration.ofClass("NSMutableDictionary").nameRenderer

private val ForwardDeclaration.nameRenderer: Renderer<Source>
  get() = sourceRenderer(name) with Dependency.of(this)

fun className(name: String): Renderer<Source> = ForwardDeclaration.ofClass(name).nameRenderer

fun protocolName(name: String): Renderer<Source> = ForwardDeclaration.ofProtocol(name).nameRenderer

fun nsEnumTypedef(name: String, type: Renderer<Source>, values: List<String>): Renderer<Source> =
  spaceSeparated(
      sourceRenderer("typedef"),
      join(nsEnum, inParentheses(commaSeparated(type, sourceRenderer(name)))),
      block(
        values
          .mapIndexed { index, name ->
            sourceRenderer(name).plusAssignment(sourceRenderer("$index")).plus(sourceRenderer(","))
          }
          .newLineSeparated()
      ),
    )
    .plusSemicolon()

fun functionDeclaration(
  modifiers: List<Renderer<Source>> = listOf(),
  returnType: Renderer<Source>,
  name: String,
  parameters: List<Renderer<Source>> = listOf(),
  statements: List<Renderer<Source>> = listOf(),
): Renderer<Source> =
  spaceSeparated(
    modifiers.spaceSeparated(),
    returnType,
    join(
      sourceRenderer(name),
      inParentheses(parameters.commaSeparated().ifEmpty { sourceRenderer("void") }),
    ),
    block(statements.newLineSeparated()),
  )

fun methodCall(
  target: Renderer<Source>,
  name: String,
  arguments: List<Renderer<Source>> = listOf(),
): Renderer<Source> =
  inSquareBrackets(
    spaceSeparated(
      target,
      if (arguments.isEmpty()) {
        sourceRenderer(name)
      } else {
        name
          .dropLast(1)
          .split(":")
          .zip(arguments)
          .map { parameter(sourceRenderer(it.first), it.second) }
          .spaceSeparated()
      },
    )
  )

fun getProperty(target: Renderer<Source>, name: String): Renderer<Source> =
  dotSeparated(target, sourceRenderer(name))

fun block(statements: List<Renderer<Source>> = listOf()): Renderer<Source> =
  block(statements.newLineSeparated())

fun returnStatement(expression: Renderer<Source>): Renderer<Source> =
  spaceSeparated(sourceRenderer("return"), expression).plusSemicolon()

fun expressionStatement(expression: Renderer<Source>): Renderer<Source> = expression.plusSemicolon()

fun nsAssumeNonnull(body: Renderer<Source>): Renderer<Source> =
  body.ifNotEmpty { emptyLineSeparated(nsAssumeNonnullBegin, it, nsAssumeNonnullEnd) }

fun Renderer<Source>.toNullable(): Renderer<Source> = spaceSeparated(this, nullable)
