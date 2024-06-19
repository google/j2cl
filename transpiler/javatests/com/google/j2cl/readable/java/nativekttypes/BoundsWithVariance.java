/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nativekttypes;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javaemul.internal.annotations.KtIn;
import javaemul.internal.annotations.KtOut;
import org.jspecify.annotations.NullMarked;

interface FnWithoutVariance<I, O> {}

interface FnWithVariance<@KtIn I, @KtOut O> {}

@NullMarked
final class BoundsWithVariance {

  <I, O> void accept(FnWithoutVariance<? super I, ? extends O> o) {}

  <I, O> void accept(FnWithVariance<? super I, ? extends O> o) {}

  // Tests for Native JRE types

  <E> void accept(Comparable<? super E> o) {}

  <E> void accept(Iterable<? extends E> o) {}

  <E> void accept(Iterator<? extends E> o) {}

  <E> void accept(ListIterator<? extends E> o) {}

  <E> void accept(Collection<? extends E> o) {}

  <E> void accept(List<? extends E> o) {}

  <E> void accept(Set<? extends E> o) {}

  <E> void accept(AbstractCollection<? extends E> o) {}

  <E> void accept(AbstractList<? extends E> o) {}

  <E> void accept(AbstractSet<? extends E> o) {}

  <K, V> void accept(Map<? super K, ? extends V> o) {}

  <K, V> void accept(AbstractMap<? super K, ? extends V> o) {}

  <K, V> void accept(Map.Entry<? extends K, ? extends V> o) {}
}
