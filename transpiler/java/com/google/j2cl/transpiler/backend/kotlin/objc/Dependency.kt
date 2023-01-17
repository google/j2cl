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

/** Renderer dependency: an import or a forward declaration. */
sealed class Dependency {
  data class WithImport(val import: Import) : Dependency()
  data class WithForwardDeclaration(val forwardDeclaration: ForwardDeclaration) : Dependency()
}

/** Forward declaration: @class or @protocol. */
data class ForwardDeclaration(val kind: Kind, val name: String) {
  enum class Kind {
    CLASS,
    PROTOCOL
  }
}

data class Import(val path: String, val isLocal: Boolean)

fun dependency(it: Import): Dependency = Dependency.WithImport(it)

fun dependency(it: ForwardDeclaration): Dependency = Dependency.WithForwardDeclaration(it)

val Iterable<Dependency>.imports: List<Import>
  get() = filterIsInstance<Dependency.WithImport>().map { it.import }

val Iterable<Dependency>.forwardDeclarations: List<ForwardDeclaration>
  get() = filterIsInstance<Dependency.WithForwardDeclaration>().map { it.forwardDeclaration }

fun systemImport(path: String): Import = Import(path, isLocal = false)

fun localImport(path: String): Import = Import(path, isLocal = true)

fun classForwardDeclaration(name: String): ForwardDeclaration =
  ForwardDeclaration(ForwardDeclaration.Kind.CLASS, name)

fun protocolForwardDeclaration(name: String): ForwardDeclaration =
  ForwardDeclaration(ForwardDeclaration.Kind.PROTOCOL, name)
