/*
 * Copyright 2007 Google Inc.
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
package java.util;

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtPropagateNullability;
import javaemul.internal.annotations.KtProperty;
import jsinterop.annotations.JsNonNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Map.html">the official Java API
 * doc</a> for details.
 */
@KtNative("kotlin.collections.MutableMap")
public interface Map<K, V> {

  /** Represents an individual map entry. */
  @KtNative("kotlin.collections.MutableMap.MutableEntry")
  interface Entry<K, V> {
    @KtProperty
    @KtPropagateNullability
    @JsNonNull
    K getKey();

    @KtProperty
    @KtPropagateNullability
    @JsNonNull
    V getValue();

    @JsNonNull
    @KtPropagateNullability
    V setValue(@JsNonNull V value);

    static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K,V>> comparingByKey() {
      // native interface method
      throw new IllegalStateException();
    }

    static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
      // native interface method
      throw new IllegalStateException();
    }

    static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> comparingByValue() {
      // native interface method
      throw new IllegalStateException();
    }

    static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
      // native interface method
      throw new IllegalStateException();
    }
  }

  void clear();

  @KtPropagateNullability
  boolean containsKey(@JsNonNull K key);

  @KtPropagateNullability
  boolean containsValue(@JsNonNull V value);

  @JsNonNull
  @KtPropagateNullability
  @KtProperty
  @KtName("entries")
  Set<Entry<K, V>> entrySet();

  @KtPropagateNullability
  V get(@JsNonNull K key);

  boolean isEmpty();

  @JsNonNull
  @KtPropagateNullability
  @KtProperty
  @KtName("keys")
  Set<K> keySet();

  @KtPropagateNullability
  V put(@JsNonNull K key, @JsNonNull V value);

  @KtPropagateNullability
  void putAll(@JsNonNull Map<? extends K, V> t);

  @KtPropagateNullability
  V remove(@JsNonNull K key);

  @KtProperty
  int size();

  @JsNonNull
  @KtProperty
  @KtPropagateNullability
  Collection<V> values();
}
