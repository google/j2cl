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
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource

/** Renders an object of type {@code T} collecting its dependencies in a mutable set. */
class Renderer<out V>(val renderAddingDependencies: (MutableSet<Dependency>) -> V)

/** Renderer of the given value with no dependencies. */
fun <V> rendererOf(value: V): Renderer<V> = Renderer { value }

/** Renderer of this value with the given dependency. */
infix fun <V> V.rendererWith(dependency: Dependency): Renderer<V> = Renderer { dependencies ->
  also { dependencies.add(dependency) }
}

/**
 * Renderer which maps value of this renderer using given function and adds dependencies from both
 * this and the mapped renderer.
 */
fun <I, O> Renderer<I>.bind(fn: (I) -> Renderer<O>): Renderer<O> = Renderer { dependencies ->
  fn(renderAddingDependencies(dependencies)).renderAddingDependencies(dependencies)
}

/** Renderer which maps value of this renderer keeping its dependencies. */
fun <I, O> Renderer<I>.map(fn: (I) -> O): Renderer<O> = bind { rendererOf(fn(it)) }

fun <I1, I2, O> map2(
  renderer1: Renderer<I1>,
  renderer2: Renderer<I2>,
  fn: (I1, I2) -> O
): Renderer<O> = renderer1.bind { source1 -> renderer2.map { source2 -> fn(source1, source2) } }

fun <I1, I2, I3, O> map3(
  renderer1: Renderer<I1>,
  renderer2: Renderer<I2>,
  renderer3: Renderer<I3>,
  fn: (I1, I2, I3) -> O
): Renderer<O> =
  renderer1.bind { source1 ->
    renderer2.bind { source2 -> renderer3.map { source3 -> fn(source1, source2, source3) } }
  }

fun <I1, I2, I3, I4, O> map4(
  renderer1: Renderer<I1>,
  renderer2: Renderer<I2>,
  renderer3: Renderer<I3>,
  renderer4: Renderer<I4>,
  fn: (I1, I2, I3, I4) -> O
): Renderer<O> =
  renderer1.bind { source1 ->
    renderer2.bind { source2 ->
      renderer3.bind { source3 ->
        renderer4.map { source4 -> fn(source1, source2, source3, source4) }
      }
    }
  }

val <V> Iterable<Renderer<V>>.flatten: Renderer<List<V>>
  get() = Renderer { dependencies -> map { it.renderAddingDependencies(dependencies) } }

val emptyRenderer: Renderer<Source>
  get() = rendererOf(emptySource)

fun Renderer<Source>.ifNotEmpty(fn: (Source) -> Renderer<Source>): Renderer<Source> = bind {
  if (it.isEmpty) emptyRenderer else fn(it)
}
