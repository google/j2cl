/*
 * Copyright 2025 Google Inc.
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
package kotlinjavainterop

import java.util.Spliterator
import java.util.function.Consumer
import java.util.stream.Stream

class KotlinSet<V> : AbstractSet<V>() {
  override val size: Int
    get() = TODO("Not yet implemented")

  override fun isEmpty(): Boolean {
    return super.isEmpty()
  }

  override fun containsAll(elements: Collection<V>): Boolean {
    return super.containsAll(elements)
  }

  override fun contains(element: V): Boolean {
    return super.contains(element)
  }

  override fun forEach(action: Consumer<in V>?) {
    super.forEach(action)
  }

  override fun iterator(): Iterator<V> {
    TODO("Not yet implemented")
  }

  override fun parallelStream(): Stream<V> {
    return super.parallelStream()
  }

  override fun spliterator(): Spliterator<V> {
    return super<AbstractSet>.spliterator()
  }

  override fun stream(): Stream<V> {
    return super.stream()
  }
}
