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

import javaemul.internal.annotations.KtNative;
import jsinterop.annotations.JsNonNull;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/AbstractMap.html">the official
 * Java API doc</a> for details.
 */
@KtNative(
    value = "kotlin.collections.AbstractMutableMap",
    bridgeWith = "javaemul.lang.JavaAbstractMap")
public abstract class AbstractMap<K, V> implements Map<K, V> {

  protected AbstractMap() {}

  @Override
  public native void clear();

  @Override
  public native boolean containsKey(Object key);

  @Override
  public native boolean containsValue(Object value);

  @Override
  public native V get(Object key);

  @Override
  public abstract @JsNonNull Set<Entry<K, V>> entrySet();

  @Override
  public native boolean isEmpty();

  @Override
  public native @JsNonNull Set<K> keySet();

  @Override
  public native V put(@JsNonNull K key, @JsNonNull V value);

  @Override
  public native void putAll(@JsNonNull Map<? extends K, V> map);

  @Override
  public native V remove(Object key);

  @JsNonNull
  @Override
  public native int size();

  @Override
  public native @JsNonNull Collection<V> values();
}
