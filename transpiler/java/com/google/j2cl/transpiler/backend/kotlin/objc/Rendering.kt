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
import com.google.j2cl.transpiler.backend.kotlin.source.emptyLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource

/**
 * A piece of Objective-C code, which can be rendered as a source while collecting its dependencies
 * in a mutable set.
 */
class Rendering(val renderSourceAndCollectIn: (MutableSet<Dependency>) -> Source)

fun rendering(source: Source) = Rendering { source }

infix fun Source.renderingWith(dependency: Dependency) = Rendering { dependencies ->
  also { dependencies.add(dependency) }
}

val Rendering.sourceWithDependencies
  get() =
    mutableSetOf<Dependency>().let { mutableDependencies ->
      val renderedSource = renderSourceAndCollectIn(mutableDependencies)
      val dependenciesSource = source(mutableDependencies)
      emptyLineSeparated(dependenciesSource, renderedSource)
    }

fun Rendering.bindSource(fn: (Source) -> Rendering) = Rendering { dependencies ->
  fn(renderSourceAndCollectIn(dependencies)).renderSourceAndCollectIn(dependencies)
}

fun Iterable<Rendering>.bindSources(fn: (Iterable<Source>) -> Rendering) =
  Rendering { dependencies ->
    fn(map { it.renderSourceAndCollectIn(dependencies) }).renderSourceAndCollectIn(dependencies)
  }

fun Iterable<Rendering>.combineSources(fn: (Iterable<Source>) -> Source) = bindSources {
  rendering(fn(it))
}

val Rendering?.orEmpty
  get() = this ?: rendering(emptySource)
