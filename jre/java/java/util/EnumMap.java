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

import static javaemul.internal.InternalPreconditions.checkArgument;

/**
 * A {@link java.util.Map} of {@link Enum}s. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/EnumMap.html">[Sun docs]</a>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements Cloneable {

  private TreeMap<K, V> map = new TreeMap<K, V>();

  EnumMap() {}

  public EnumMap(Class<K> type) {}

  public EnumMap(Map<K, ? extends V> m) {
    putAll(m);
    checkArgument(m instanceof EnumMap || !isEmpty());
  }

  @Override
  public void clear() {
    map.clear();
  }

  public EnumMap<K, V> clone() {
    return new EnumMap<K, V>(this);
  }

  @Override
  public boolean containsKey(Object key) {
    return getEntry(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public V get(Object key) {
    Entry<K, V> entry = getEntry(key);
    return entry != null ? entry.getValue() : null;
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    Entry<K, V> entry = getEntry(key);
    return entry != null ? map.remove(key) : null;
  }

  @Override
  public int size() {
    return map.size();
  }

  /**
   * Returns the entry in the backing map for the given key (or {@code null} if there is no such
   * entry).
   *
   * <p>The difference between this method and a straight call to the backing map is twofold:
   *
   * <ul>
   *   <li>The backing map uses a natural-order comparator, so it throws NPE for nulls and CCE for
   *       non-enum values. (Contrast to the JDK implementatation, which catches such exceptions.)
   *       This method returns {@code null}.
   *   <li>The J2CL {@link Enum#compareTo} does not check that the two enum constants are from the
   *       same enum. This method handles that by calling {@code equals} on the result.
   * </ul>
   *
   * <p>We use this method to implement correct behavior for the most common methods in the most
   * common cases. However, less common operations can still produce incorrect behavior. TODO:
   * b/376045993 - Fix more of them.
   */
  private Entry<K, V> getEntry(Object key) {
    if (!(key instanceof Enum)) {
      return null;
    }
    Entry<K, V> entry = map.findByObject(key);
    return entry != null && entry.getKey().equals(key) ? entry : null;
  }
}
