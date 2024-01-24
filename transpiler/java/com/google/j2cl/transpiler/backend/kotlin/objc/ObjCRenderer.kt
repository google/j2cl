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

val nsObjCRuntimeDependency = Dependency.of(Import.system("Foundation/NSObjCRuntime.h"))

val nsEnum: Renderer<Source> = rendererOf(source("NS_ENUM")) + nsObjCRuntimeDependency

val nsInline: Renderer<Source> = rendererOf(source("NS_INLINE")) + nsObjCRuntimeDependency

val nsAssumeNonnullBegin: Renderer<Source> =
  rendererOf(source("NS_ASSUME_NONNULL_BEGIN")) + nsObjCRuntimeDependency

val nsAssumeNonnullEnd: Renderer<Source> =
  rendererOf(source("NS_ASSUME_NONNULL_END")) + nsObjCRuntimeDependency

val nullable: Renderer<Source> = rendererOf(source("_Nullable")) // clang/gcc attribute

val id: Renderer<Source> = rendererOf(source("id")) + nsObjCRuntimeDependency

val nsCopying: Renderer<Source> = ForwardDeclaration.ofProtocol("NSCopying").nameRenderer

val nsObject: Renderer<Source> = ForwardDeclaration.ofClass("NSObject").nameRenderer

val nsNumber: Renderer<Source> = ForwardDeclaration.ofClass("NSNumber").nameRenderer

val nsString: Renderer<Source> = ForwardDeclaration.ofClass("NSString").nameRenderer

val nsMutableArray: Renderer<Source> = ForwardDeclaration.ofClass("NSMutableArray").nameRenderer

val nsMutableSet: Renderer<Source> = ForwardDeclaration.ofClass("NSMutableSet").nameRenderer

val nsMutableDictionary: Renderer<Source> =
  ForwardDeclaration.ofClass("NSMutableDictionary").nameRenderer

private val ForwardDeclaration.nameRenderer: Renderer<Source>
  get() = rendererOf(source(name)) + Dependency.of(this)

fun className(name: String): Renderer<Source> = ForwardDeclaration.ofClass(name).nameRenderer

fun protocolName(name: String): Renderer<Source> = ForwardDeclaration.ofProtocol(name).nameRenderer

fun nsEnumTypedef(name: String, type: Renderer<Source>, values: List<String>): Renderer<Source> =
  combine(nsEnum, type) { nsEnumSource, typeSource ->
    semicolonEnded(
      spaceSeparated(
        source("typedef"),
        join(nsEnumSource, inParentheses(commaSeparated(typeSource, source(name)))),
        block(
          newLineSeparated(
            values.mapIndexed { index, name ->
              assignment(source(name), source("$index")) + Source.COMMA
            }
          )
        ),
      )
    )
  }

fun functionDeclaration(
  modifiers: List<Renderer<Source>> = listOf(),
  returnType: Renderer<Source>,
  name: String,
  parameters: List<Renderer<Source>> = listOf(),
  statements: List<Renderer<Source>> = listOf(),
): Renderer<Source> =
  combine(modifiers.flatten(), returnType, parameters.flatten(), statements.flatten()) {
    modifierSources,
    returnTypeSource,
    parameterSources,
    statementSources ->
    spaceSeparated(
      spaceSeparated(modifierSources),
      returnTypeSource,
      join(
        source(name),
        inParentheses(commaSeparated(parameterSources).ifEmpty { source("void") }),
      ),
      block(newLineSeparated(statementSources)),
    )
  }

fun methodCall(
  target: Renderer<Source>,
  name: String,
  arguments: List<Renderer<Source>> = listOf(),
): Renderer<Source> =
  combine(target, arguments.flatten()) { targetSource, argumentSources ->
    inSquareBrackets(
      spaceSeparated(
        targetSource,
        if (argumentSources.isEmpty()) source(name)
        else
          spaceSeparated(
            name.dropLast(1).split(":").zip(argumentSources).map {
              parameter(source(it.first), it.second)
            }
          ),
      )
    )
  }

fun getProperty(target: Renderer<Source>, name: String): Renderer<Source> =
  target.map { dotSeparated(it, source(name)) }

fun block(statements: List<Renderer<Source>> = listOf()): Renderer<Source> =
  statements.flatten().map { block(newLineSeparated(it)) }

fun returnStatement(expression: Renderer<Source>): Renderer<Source> =
  expression.map { semicolonEnded(spaceSeparated(source("return"), it)) }

fun expressionStatement(expression: Renderer<Source>): Renderer<Source> =
  expression.map { semicolonEnded(it) }

fun nsAssumeNonnull(body: Renderer<Source>): Renderer<Source> =
  body.bind {
    if (it.isEmpty()) {
      rendererOf(Source.EMPTY)
    } else {
      combine(nsAssumeNonnullBegin, nsAssumeNonnullEnd) { begin, end ->
        emptyLineSeparated(begin, it, end)
      }
    }
  }

fun Renderer<Source>.toNullable(): Renderer<Source> =
  combine(this, nullable) { thisSource, nullableSource ->
    spaceSeparated(thisSource, nullableSource)
  }
