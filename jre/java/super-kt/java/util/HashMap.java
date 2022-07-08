/*
 * Copyright 2008 Google Inc.
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

import javaemul.internal.annotations.KtNative;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsNullable;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html">the official Java
 * API doc</a> for details.
 */
@KtNative("kotlin.collections.HashMap")
public class HashMap<K, V> implements Map<K, V> {

  public HashMap() {}

  public HashMap(int initialCapacity) {}

  public HashMap(int initialCapacity, float loadFactor) {}

  public HashMap(Map<? extends K, ? extends V> original) {}

  @Override
  public native int size();

  @Override
  public native boolean isEmpty();

  @Override
  public native boolean containsKey(@JsNonNull K key);

  @Override
  public native boolean containsValue(@JsNonNull V value);

  @Override
  public native @JsNullable V get(@JsNonNull K key);

  @Override
  public native @JsNullable V put(@JsNonNull K key, @JsNonNull V value);

  @Override
  public native @JsNullable V remove(@JsNonNull K key);

  @Override
  public native void putAll(@JsNonNull Map<? extends K, V> m);

  @Override
  public native void clear();

  @Override
  public native @JsNonNull Set<K> keySet();

  @Override
  public native @JsNonNull Collection<V> values();

  @Override
  public native @JsNonNull Set<@JsNonNull Entry<K, V>> entrySet();
}
