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

import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.combine
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.dependent
import com.google.j2cl.transpiler.backend.kotlin.objc.Dependent.Companion.flatten
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

fun dependentSource(string: String): Dependent<Source> = dependent(source(string))

fun Dependent<Source>.ifEmpty(fn: () -> Dependent<Source>): Dependent<Source> = bind { source ->
  if (source.isEmpty()) fn() else dependent(source)
}

fun Dependent<Source>.ifNotEmpty(fn: (Dependent<Source>) -> Dependent<Source>): Dependent<Source> =
  bind { source ->
    if (source.isNotEmpty()) fn(dependent(source)) else dependent(source)
  }

operator fun Dependent<Source>.plus(dependent: Dependent<Source>): Dependent<Source> =
  combine(this, dependent) { lhs, rhs -> lhs + rhs }

operator fun Dependent<Source>.plus(source: Source): Dependent<Source> = plus(dependent(source))

fun Dependent<Source>.plusSemicolon(): Dependent<Source> = plus(Source.SEMICOLON)

fun Dependent<Source>.plusComma(): Dependent<Source> = plus(Source.COMMA)

fun Dependent<Source>.toPointer() = plus(Source.STAR)

fun Dependent<Source>.plusAssignment(rhs: Dependent<Source>): Dependent<Source> =
  spaceSeparated(this, dependentSource("="), rhs)

fun parameter(name: Dependent<Source>, value: Dependent<Source>): Dependent<Source> =
  join(name, dependent(Source.COLON), value)

fun block(dependent: Dependent<Source>): Dependent<Source> = dependent.map { block(it) }

fun inParentheses(dependent: Dependent<Source>): Dependent<Source> =
  dependent.map { inParentheses(it) }

fun inSquareBrackets(dependent: Dependent<Source>): Dependent<Source> =
  dependent.map { inSquareBrackets(it) }

fun join(first: Dependent<Source>, vararg rest: Dependent<Source>): Dependent<Source> =
  listOf(first, *rest).flatten().map { join(it) }

fun Iterable<Dependent<Source>>.newLineSeparated(): Dependent<Source> =
  flatten().map { newLineSeparated(it) }

fun Iterable<Dependent<Source>>.spaceSeparated(): Dependent<Source> =
  flatten().map { spaceSeparated(it) }

fun Iterable<Dependent<Source>>.commaSeparated(): Dependent<Source> =
  flatten().map { commaSeparated(it) }

fun newLineSeparated(first: Dependent<Source>, vararg rest: Dependent<Source>): Dependent<Source> =
  listOf(first, *rest).newLineSeparated()

fun spaceSeparated(first: Dependent<Source>, vararg rest: Dependent<Source>): Dependent<Source> =
  listOf(first, *rest).flatten().map { spaceSeparated(it) }

fun emptyLineSeparated(
  first: Dependent<Source>,
  vararg rest: Dependent<Source>,
): Dependent<Source> = listOf(first, *rest).flatten().map { emptyLineSeparated(it) }

fun commaSeparated(first: Dependent<Source>, vararg rest: Dependent<Source>): Dependent<Source> =
  listOf(first, *rest).flatten().map { commaSeparated(it) }

fun dotSeparated(first: Dependent<Source>, vararg rest: Dependent<Source>): Dependent<Source> =
  listOf(first, *rest).flatten().map { dotSeparated(it) }

fun dependentNsObjCRuntimeSource(string: String): Dependent<Source> =
  dependentSource(string) with nsObjCRuntimeDependency

fun dependentMathSource(string: String): Dependent<Source> =
  dependentSource(string) with mathDependency

fun dependentFloatSource(string: String): Dependent<Source> =
  dependentSource(string) with floatDependency

val nsObjCRuntimeDependency = Dependency.of(Import.system("Foundation/NSObjCRuntime.h"))
val mathDependency = Dependency.of(Import.system("math.h"))
val floatDependency = Dependency.of(Import.system("float.h"))

val nsEnum: Dependent<Source> = dependentNsObjCRuntimeSource("NS_ENUM")

val nsInline: Dependent<Source> = dependentNsObjCRuntimeSource("NS_INLINE")

val nsAssumeNonnullBegin: Dependent<Source> =
  dependentNsObjCRuntimeSource("NS_ASSUME_NONNULL_BEGIN")

val nsAssumeNonnullEnd: Dependent<Source> = dependentNsObjCRuntimeSource("NS_ASSUME_NONNULL_END")

val nullable: Dependent<Source> = dependentSource("_Nullable") // clang/gcc attribute

val id: Dependent<Source> = dependentNsObjCRuntimeSource("id")

val nsCopying: Dependent<Source> = ForwardDeclaration.ofProtocol("NSCopying").dependentName

val nsObject: Dependent<Source> = ForwardDeclaration.ofClass("NSObject").dependentName

val nsNumber: Dependent<Source> = ForwardDeclaration.ofClass("NSNumber").dependentName

val nsString: Dependent<Source> = ForwardDeclaration.ofClass("NSString").dependentName

val nsMutableArray: Dependent<Source> = ForwardDeclaration.ofClass("NSMutableArray").dependentName

val nsMutableSet: Dependent<Source> = ForwardDeclaration.ofClass("NSMutableSet").dependentName

val nsMutableDictionary: Dependent<Source> =
  ForwardDeclaration.ofClass("NSMutableDictionary").dependentName

private val ForwardDeclaration.dependentName: Dependent<Source>
  get() = dependentSource(name) with Dependency.of(this)

fun className(name: String): Dependent<Source> = ForwardDeclaration.ofClass(name).dependentName

fun protocolName(name: String): Dependent<Source> =
  ForwardDeclaration.ofProtocol(name).dependentName

fun typedef(name: String, type: Dependent<Source>): Dependent<Source> =
  spaceSeparated(dependentSource("typedef"), dependentSource(name), type).plusSemicolon()

fun functionDeclaration(
  modifiers: List<Dependent<Source>> = listOf(),
  returnType: Dependent<Source>,
  name: String,
  parameters: List<Dependent<Source>> = listOf(),
  statements: List<Dependent<Source>> = listOf(),
): Dependent<Source> =
  spaceSeparated(
    modifiers.spaceSeparated(),
    returnType,
    join(
      dependentSource(name),
      inParentheses(parameters.commaSeparated().ifEmpty { dependentSource("void") }),
    ),
    block(statements.newLineSeparated()),
  )

fun methodCall(
  target: Dependent<Source>,
  name: String,
  arguments: List<Dependent<Source>> = listOf(),
): Dependent<Source> =
  inSquareBrackets(
    spaceSeparated(
      target,
      if (arguments.isEmpty()) {
        dependentSource(name)
      } else {
        name
          .dropLast(1)
          .split(":")
          .zip(arguments)
          .map { parameter(dependentSource(it.first), it.second) }
          .spaceSeparated()
      },
    )
  )

fun getProperty(target: Dependent<Source>, name: String): Dependent<Source> =
  dotSeparated(target, dependentSource(name))

fun block(statements: List<Dependent<Source>> = listOf()): Dependent<Source> =
  block(statements.newLineSeparated())

fun returnStatement(expression: Dependent<Source>): Dependent<Source> =
  spaceSeparated(dependentSource("return"), expression).plusSemicolon()

fun expressionStatement(expression: Dependent<Source>): Dependent<Source> =
  expression.plusSemicolon()

fun nsAssumeNonnull(body: Dependent<Source>): Dependent<Source> =
  body.ifNotEmpty { emptyLineSeparated(nsAssumeNonnullBegin, it, nsAssumeNonnullEnd) }

fun Dependent<Source>.toNullable(): Dependent<Source> = spaceSeparated(this, nullable)
