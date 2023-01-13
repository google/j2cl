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

import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated

/** Rendering dependency: an import or a forward declaration. */
sealed class Dependency {
  data class WithImport(val import: Import) : Dependency()
  data class WithForwardDeclaration(val forwardDeclaration: ForwardDeclaration) : Dependency()
}

fun dependency(it: Import): Dependency = Dependency.WithImport(it)

fun dependency(it: ForwardDeclaration): Dependency = Dependency.WithForwardDeclaration(it)

val Iterable<Dependency>.imports
  get() = filterIsInstance<Dependency.WithImport>().map { it.import }

val Iterable<Dependency>.forwardDeclarations
  get() = filterIsInstance<Dependency.WithForwardDeclaration>().map { it.forwardDeclaration }

fun source(dependencies: Iterable<Dependency>) =
  emptyLineSeparated(source(dependencies.imports), source(dependencies.forwardDeclarations))
