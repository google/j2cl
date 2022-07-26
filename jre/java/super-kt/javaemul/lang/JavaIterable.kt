/*
 * Copyright 2022 Google Inc.
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
package javaemul.lang

import java.util.Spliterator
import java.util.function.Consumer

interface JavaIterable<T> : MutableIterable<T> {
  fun java_forEach(consumer: Consumer<in T?>?) {
    default_forEach(consumer)
  }

  fun spliterator(): Spliterator<T>? = default_spliterator()
}

fun <T> MutableIterable<T>.java_forEach(consumer: Consumer<in T?>?) {
  if (this is JavaIterable) java_forEach(consumer) else default_forEach(consumer)
}

private fun <T> kotlin.collections.MutableIterable<T>.default_forEach(consumer: Consumer<in T?>?) {
  requireNotNull(consumer)
  for (t in this) consumer.accept(t)
}

fun <T> kotlin.collections.MutableIterable<T>.spliterator(): Spliterator<T>? =
  if (this is JavaIterable) spliterator() else default_spliterator()

// TODO (b/237650063): Add implementation after Spliterator is supported
private fun <T> kotlin.collections.Iterable<T>.default_spliterator(): Spliterator<T>? = null
