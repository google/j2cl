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
import static javaemul.internal.InternalPreconditions.checkConcurrentModification;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkState;
import static javaemul.internal.InternalPreconditions.isApiChecked;

import javaemul.internal.JsUtils;

/**
 * Implementation of Map interface based on a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html">[Sun
 * docs]</a>
 *
 * @param <K> key type
 * @param <V> value type
 */
abstract class AbstractHashMap<K, V> extends AbstractMap<K, V> {

  private final class EntrySet extends AbstractSet<Entry<K, V>> {

    @Override
    public void clear() {
      AbstractHashMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (o instanceof Map.Entry) {
        return containsEntry((Map.Entry<?, ?>) o);
      }
      return false;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntrySetIterator();
    }

    @Override
    public boolean remove(Object entry) {
      if (contains(entry)) {
        Object key = ((Map.Entry<?, ?>) entry).getKey();
        AbstractHashMap.this.remove(key);
        return true;
      }
      return false;
    }

    @Override
    public int size() {
      return AbstractHashMap.this.size();
    }
  }

  /**
   * Iterator for <code>EntrySet</code>.
   */
  private final class EntrySetIterator implements Iterator<Entry<K, V>> {
    private Iterator<Entry<K, V>> stringMapEntries = stringMap.iterator();
    private Iterator<Entry<K, V>> current = stringMapEntries;
    private Iterator<Entry<K, V>> last;
    private boolean hasNext = computeHasNext();
    private int lastModCount = modCount;

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    private boolean computeHasNext() {
      if (current.hasNext()) {
        return true;
      }
      if (current != stringMapEntries) {
        return false;
      }
      current = hashCodeMap.iterator();
      return current.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      checkConcurrentModification(modCount, lastModCount);
      checkElement(hasNext());

      last = current;
      Entry<K, V> rv = current.next();
      hasNext = computeHasNext();

      return rv;
    }

    @Override
    public void remove() {
      checkState(last != null);
      checkConcurrentModification(modCount, lastModCount);

      last.remove();
      last = null;
      hasNext = computeHasNext();

      lastModCount = modCount;
    }
  }

  /** A map of integral hashCodes onto entries. */
  private InternalHashCodeMap<K, V> hashCodeMap;

  /** A map of Strings onto values. */
  private InternalStringMap<K, V> stringMap;

  int modCount;

  public AbstractHashMap() {
    reset();
  }

  public AbstractHashMap(int ignored) {
    // This implementation of HashMap has no need of initial capacities.
    this(ignored, 0);
  }

  public AbstractHashMap(int ignored, float alsoIgnored) {
    // This implementation of HashMap has no need of load factors or capacities.
    checkArgument(ignored >= 0, "Negative initial capacity");
    checkArgument(alsoIgnored >= 0, "Non-positive load factor");

    reset();
  }

  public AbstractHashMap(Map<? extends K, ? extends V> toBeCopied) {
    reset();
    this.putAll(toBeCopied);
  }

  @Override
  public void clear() {
    reset();
  }

  private void reset() {
    hashCodeMap = new InternalHashCodeMap<K, V>(this);
    stringMap = new InternalStringMap<K, V>(this);
    structureChanged();
  }

  void structureChanged() {
    if (!isApiChecked()) {
      // Shouldn't be necessary but JsCompiler chokes on removing modCount so make sure we don't pay
      // cost for updating the field.
      return;
    }
    this.modCount++;
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof String
        ? stringMap.contains(JsUtils.uncheckedCast(key))
        : hashCodeMap.getEntry(key) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return containsValue(value, stringMap) || containsValue(value, hashCodeMap);
  }

  private boolean containsValue(Object value, Iterable<Entry<K, V>> entries) {
    for (Entry<K, V> entry : entries) {
      if (equals(value, entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public V get(Object key) {
    return key instanceof String
        ? stringMap.get(JsUtils.uncheckedCast(key))
        : getEntryValueOrNull(hashCodeMap.getEntry(key));
  }

  @Override
  public V put(K key, V value) {
    return key instanceof String
        ? stringMap.put(JsUtils.uncheckedCast(key), value)
        : hashCodeMap.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return key instanceof String
        ? stringMap.remove(JsUtils.uncheckedCast(key))
        : hashCodeMap.remove(key);
  }

  @Override
  public int size() {
    return hashCodeMap.size() + stringMap.size();
  }

  /**
   * Subclasses must override to return a whether or not two keys or values are
   * equal.
   */
  abstract boolean equals(Object value1, Object value2);

  /**
   * Subclasses must override to return a hash code for a given key. The key is
   * guaranteed to be non-null and not a String.
   */
  abstract int getHashCode(Object key);

}
