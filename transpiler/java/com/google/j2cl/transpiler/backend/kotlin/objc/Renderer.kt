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

/**
 * Renderer of [V] and its dependencies.
 *
 * Renderers are designed to be combined using [map], [bind], [combine] and [flatten] functions.
 * Dependencies can be added with [plus] operator.
 *
 * The final rendering can be executed using [renderWithDependencies] function, which will return
 * the rendered [V] and a set containing all its dependencies.
 */
class Renderer<out V>(
  /** A function which renders [V] adding its dependencies to a mutable set. */
  private val renderAddingDependenciesTo: (MutableSet<Dependency>) -> V
) {
  /** Returns a pair with rendered [V] and a set of its dependencies. */
  fun renderWithDependencies(): Pair<V, Set<Dependency>> =
    mutableSetOf<Dependency>().let { dependencies ->
      renderAddingDependenciesTo(dependencies) to dependencies.toSet()
    }

  /** Returns renderer of the same value including the given dependency. */
  operator fun plus(dependency: Dependency): Renderer<V> = Renderer { dependencies ->
    renderAddingDependenciesTo(dependencies).also { dependencies.add(dependency) }
  }

  /**
   * Rerturns renderer which maps value of this renderer using [fn], while keeping its dependencies.
   */
  infix fun <O> map(fn: (V) -> O): Renderer<O> = Renderer { dependencies ->
    fn(renderAddingDependenciesTo(dependencies))
  }

  /**
   * Returns renderer which maps value of this renderer using [fn] and adds dependencies from both
   * this and the mapped renderer.
   */
  infix fun <O> bind(fn: (V) -> Renderer<O>): Renderer<O> = Renderer { dependencies ->
    fn(renderAddingDependenciesTo(dependencies)).renderAddingDependenciesTo(dependencies)
  }

  companion object {
    /** Returns renderer of [value] with no dependencies. */
    fun <V> rendererOf(value: V): Renderer<V> = Renderer { value }

    /**
     * Returns renderer which combines values rendered by [renderer1] and [renderer2] using [fn]
     * adding all of their dependencies.
     */
    fun <I1, I2, O> combine(
      renderer1: Renderer<I1>,
      renderer2: Renderer<I2>,
      fn: (I1, I2) -> O,
    ): Renderer<O> = Renderer { dependencies ->
      fn(
        renderer1.renderAddingDependenciesTo(dependencies),
        renderer2.renderAddingDependenciesTo(dependencies),
      )
    }

    /**
     * Returns renderer which combines values rendered by [renderer1], [renderer2] and [renderer3]
     * using [fn] adding all of their dependencies.
     */
    fun <I1, I2, I3, O> combine(
      renderer1: Renderer<I1>,
      renderer2: Renderer<I2>,
      renderer3: Renderer<I3>,
      fn: (I1, I2, I3) -> O,
    ): Renderer<O> = Renderer { dependencies ->
      fn(
        renderer1.renderAddingDependenciesTo(dependencies),
        renderer2.renderAddingDependenciesTo(dependencies),
        renderer3.renderAddingDependenciesTo(dependencies),
      )
    }

    /**
     * Returns renderer which combines values rendered by [renderer1], [renderer2], [renderer3] and
     * [renderer4] using [fn] adding all of their dependencies.
     */
    fun <I1, I2, I3, I4, O> combine(
      renderer1: Renderer<I1>,
      renderer2: Renderer<I2>,
      renderer3: Renderer<I3>,
      renderer4: Renderer<I4>,
      fn: (I1, I2, I3, I4) -> O,
    ): Renderer<O> = Renderer { dependencies ->
      fn(
        renderer1.renderAddingDependenciesTo(dependencies),
        renderer2.renderAddingDependenciesTo(dependencies),
        renderer3.renderAddingDependenciesTo(dependencies),
        renderer4.renderAddingDependenciesTo(dependencies),
      )
    }

    /** Flattens renderers of [V] into a renderer of [V]'s. */
    fun <V> Iterable<Renderer<V>>.flatten(): Renderer<List<V>> = Renderer { dependencies ->
      map { it.renderAddingDependenciesTo(dependencies) }
    }
  }
}
