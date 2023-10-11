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
  /** Dependency with import. */
  data class WithImport(val import: Import) : Dependency()

  /** Dependency with forward declaration. */
  data class WithForwardDeclaration(val forwardDeclaration: ForwardDeclaration) : Dependency()

  companion object {
    /** Returns dependency with the given import. */
    fun of(import: Import): Dependency = WithImport(import)

    /** Returns dependency with the given forward declaration. */
    fun of(forwardDeclaration: ForwardDeclaration): Dependency =
      WithForwardDeclaration(forwardDeclaration)
  }
}

/**
 * Forward declaration.
 *
 * @property kind the [Kind] of this forward declaration.
 * @property name the name of the forward-declared entity.
 */
data class ForwardDeclaration(val kind: Kind, val name: String) {
  /** The kind of forward declaration: a class or a protocol. */
  enum class Kind {
    CLASS,
    PROTOCOL
  }

  companion object {
    /** Returns class forward declaration with a given name. */
    fun ofClass(name: String): ForwardDeclaration = ForwardDeclaration(Kind.CLASS, name)

    /** Returns protocol forward declaration with a given name. */
    fun ofProtocol(name: String): ForwardDeclaration = ForwardDeclaration(Kind.PROTOCOL, name)
  }
}

/**
 * Import declaration.
 *
 * @property path the string containing import path.
 * @property isLocal true for local import, or false for system import.
 */
data class Import(val path: String, val isLocal: Boolean) {
  companion object {
    /** Returns system import with a given path. */
    fun system(path: String): Import = Import(path, isLocal = false)

    /** Returns local import with a given path. */
    fun local(path: String): Import = Import(path, isLocal = true)
  }
}

/** Returns a list of all imports from the given dependencies. */
internal fun Iterable<Dependency>.imports(): List<Import> = mapNotNull {
  (it as? Dependency.WithImport)?.import
}

/** Returns a list of all forward declarations from the given dependencies. */
internal fun Iterable<Dependency>.forwardDeclarations(): List<ForwardDeclaration> = mapNotNull {
  (it as? Dependency.WithForwardDeclaration)?.forwardDeclaration
}
