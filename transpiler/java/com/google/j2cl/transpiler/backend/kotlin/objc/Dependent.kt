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
 * Value of type [V] and its dependencies.
 *
 * Instances of this class can be combined using [map], [bind], [combine] and [flatten] functions.
 * Dependencies can be added using [with] function.
 *
 * The value and a set containing all its dependencies can be retrieved using [getWithDependencies]
 * function.
 */
class Dependent<out V>(
  /** A function which returns [V] and adds its dependencies to a mutable set. */
  private val getAddingDependenciesTo: (MutableSet<Dependency>) -> V
) {
  /** Returns a pair with value [V] and a set of its dependencies. */
  fun getWithDependencies(): Pair<V, Set<Dependency>> =
    mutableSetOf<Dependency>().let { dependencies ->
      getAddingDependenciesTo(dependencies) to dependencies.toSet()
    }

  /** Returns dependent value including the given dependency. */
  infix fun with(dependency: Dependency): Dependent<V> = Dependent { dependencies ->
    getAddingDependenciesTo(dependencies).also { dependencies.add(dependency) }
  }

  /**
   * Returns dependent which maps value of this dependent using [fn], while keeping its
   * dependencies.
   */
  infix fun <O> map(fn: (V) -> O): Dependent<O> = Dependent { dependencies ->
    fn(getAddingDependenciesTo(dependencies))
  }

  /**
   * Returns dependent which maps value of this dependent using [fn] and adds dependencies from both
   * this and the mapped dependent.
   */
  infix fun <O> bind(fn: (V) -> Dependent<O>): Dependent<O> = Dependent { dependencies ->
    fn(getAddingDependenciesTo(dependencies)).getAddingDependenciesTo(dependencies)
  }

  companion object {
    /** Returns dependent [value] with no dependencies. */
    fun <V> dependent(value: V): Dependent<V> = Dependent { value }

    /**
     * Returns dependent which combines values in [dependent1] and [dependent2] using [fn] adding
     * all of their dependencies.
     */
    fun <I1, I2, O> combine(
      dependent1: Dependent<I1>,
      dependent2: Dependent<I2>,
      fn: (I1, I2) -> O,
    ): Dependent<O> = Dependent { dependencies ->
      fn(
        dependent1.getAddingDependenciesTo(dependencies),
        dependent2.getAddingDependenciesTo(dependencies),
      )
    }

    /**
     * Returns dependent which combines values in [dependent1], [dependent2] and [dependent3] using
     * [fn] adding all of their dependencies.
     */
    fun <I1, I2, I3, O> combine(
      dependent1: Dependent<I1>,
      dependent2: Dependent<I2>,
      dependent3: Dependent<I3>,
      fn: (I1, I2, I3) -> O,
    ): Dependent<O> = Dependent { dependencies ->
      fn(
        dependent1.getAddingDependenciesTo(dependencies),
        dependent2.getAddingDependenciesTo(dependencies),
        dependent3.getAddingDependenciesTo(dependencies),
      )
    }

    /**
     * Returns dependent which combines values in [dependent1], [dependent2], [dependent3] and
     * [dependent4] using [fn] adding all of their dependencies.
     */
    fun <I1, I2, I3, I4, O> combine(
      dependent1: Dependent<I1>,
      dependent2: Dependent<I2>,
      dependent3: Dependent<I3>,
      dependent4: Dependent<I4>,
      fn: (I1, I2, I3, I4) -> O,
    ): Dependent<O> = Dependent { dependencies ->
      fn(
        dependent1.getAddingDependenciesTo(dependencies),
        dependent2.getAddingDependenciesTo(dependencies),
        dependent3.getAddingDependenciesTo(dependencies),
        dependent4.getAddingDependenciesTo(dependencies),
      )
    }

    /** Flattens dependent [V] into a dependent [V]'s. */
    fun <V> Iterable<Dependent<V>>.flatten(): Dependent<List<V>> = Dependent { dependencies ->
      map { it.getAddingDependenciesTo(dependencies) }
    }
  }
}
