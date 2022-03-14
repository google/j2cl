/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import javaemul.internal.annotations.KtNative;

@KtNative("kotlin.collections.MutableIterable")
public interface Iterable<T> {

  Iterator<T> iterator();

  // TODO(b/222269323): Java 8 methods below do not exist in Kotlin iterables.
  default void forEach(Consumer<? super T> action) {}

  default Spliterator<T> spliterator() {
    return null;
  }
}
