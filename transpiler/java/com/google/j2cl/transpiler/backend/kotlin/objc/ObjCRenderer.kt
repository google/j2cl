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
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.ifEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.plusComma
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

val nsObjCRuntimeDependency = dependency(systemImport("Foundation/NSObjCRuntime.h"))

val nsEnum: Renderer<Source> = source("NS_ENUM") rendererWith nsObjCRuntimeDependency

val nsInline: Renderer<Source> = source("NS_INLINE") rendererWith nsObjCRuntimeDependency

val nsAssumeNonnullBegin: Renderer<Source> =
  source("NS_ASSUME_NONNULL_BEGIN") rendererWith nsObjCRuntimeDependency

val nsAssumeNonnullEnd: Renderer<Source> =
  source("NS_ASSUME_NONNULL_END") rendererWith nsObjCRuntimeDependency

val nullable: Renderer<Source> = rendererOf(source("_Nullable")) // clang/gcc attribute

val id: Renderer<Source> = source("id") rendererWith nsObjCRuntimeDependency

val nsCopying: Renderer<Source> = protocolForwardDeclaration("NSCopying").nameRenderer

val nsObject: Renderer<Source> = classForwardDeclaration("NSObject").nameRenderer

val nsNumber: Renderer<Source> = classForwardDeclaration("NSNumber").nameRenderer

val nsString: Renderer<Source> = classForwardDeclaration("NSString").nameRenderer

val nsMutableArray: Renderer<Source> = classForwardDeclaration("NSMutableArray").nameRenderer

val nsMutableSet: Renderer<Source> = classForwardDeclaration("NSMutableSet").nameRenderer

val nsMutableDictionary: Renderer<Source> =
  classForwardDeclaration("NSMutableDictionary").nameRenderer

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
              assignment(source(name), source("$index")).plusComma
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
            name.dropLast(1).split(":").zip(argumentSources).map {
              parameter(source(it.first), it.second)
            }
          )
      )
    )
  }

fun getProperty(target: Renderer<Source>, name: String): Renderer<Source> =
  target.map { dotSeparated(it, source(name)) }

fun block(statements: List<Renderer<Source>> = listOf()): Renderer<Source> =
  statements.flatten.map { block(newLineSeparated(it)) }

fun returnStatement(expression: Renderer<Source>): Renderer<Source> =
  expression.map { semicolonEnded(spaceSeparated(source("return"), it)) }

fun expressionStatement(expression: Renderer<Source>): Renderer<Source> =
  expression.map { semicolonEnded(it) }

fun nsAssumeNonnull(body: Renderer<Source>): Renderer<Source> =
  body.ifNotEmpty {
    map2(nsAssumeNonnullBegin, nsAssumeNonnullEnd) { begin, end ->
      emptyLineSeparated(begin, it, end)
    }
  }

fun Renderer<Source>.toNullable(): Renderer<Source> =
  map2(this, nullable) { thisSource, nullableSource -> spaceSeparated(thisSource, nullableSource) }
