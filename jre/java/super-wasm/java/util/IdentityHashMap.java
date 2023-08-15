/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.util;

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkConcurrentModification;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkState;

import java.io.Serializable;

/** Wasm specific {@code IdentityHashMap} implementation based on Android. */
public class IdentityHashMap<K, V> extends AbstractMap<K, V>
    implements Map<K, V>, Serializable, Cloneable {

  private Set<K> keySet;

  private Collection<V> valuesCollection;

  /*
   * The internal data structure to hold key value pairs This array holds keys
   * and values in an alternating fashion.
   */
  private Object[] elementData;

  /* Actual number of key-value pairs. */
  private int size;

  /*
   * maximum number of elements that can be put in this map before having to
   * rehash.
   */
  private int threshold;

  /*
   * default threshold value that an IdentityHashMap created using the default
   * constructor would have.
   */
  private static final int DEFAULT_MAX_SIZE = 21;

  /* Default load factor of 0.75; */
  private static final int DEFAULT_LOAD_FACTOR = 7500;

  /*
   * modification count, to keep track of structural modifications between the
   * IdentityHashMap and the iterator
   */
  private int modCount = 0;

  /*
   * Object used to represent null keys and values. This is used to
   * differentiate a literal 'null' key value pair from an empty spot in the
   * map.
   */
  private static final Object NULL_OBJECT = new Object(); // $NON-LOCK-1$

  private static class IdentityHashMapEntry<K, V> extends MapEntry<K, V> {
    private final IdentityHashMap<K, V> map;

    IdentityHashMapEntry(IdentityHashMap<K, V> map, K theKey, V theValue) {
      super(theKey, theValue);
      this.map = map;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry) object;
        return (key == entry.getKey()) && (value == entry.getValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(key) ^ System.identityHashCode(value);
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }

    @Override
    public V setValue(V object) {
      V result = super.setValue(object);
      map.put(key, object);
      return result;
    }
  }

  static class IdentityHashMapIterator<E, KT, VT> implements Iterator<E> {
    private int position = 0; // the current position
    // the position of the entry that was last returned from next()
    private int lastPosition = 0;
    final IdentityHashMap<KT, VT> associatedMap;
    int expectedModCount;
    final MapEntry.Type<E, KT, VT> type;
    boolean canRemove = false;

    IdentityHashMapIterator(MapEntry.Type<E, KT, VT> value, IdentityHashMap<KT, VT> hm) {
      associatedMap = hm;
      type = value;
      expectedModCount = hm.modCount;
    }

    public boolean hasNext() {
      while (position < associatedMap.elementData.length) {
        // if this is an empty spot, go to the next one
        if (associatedMap.elementData[position] == null) {
          position += 2;
        } else {
          return true;
        }
      }
      return false;
    }

    public E next() {
      checkConcurrentModification(expectedModCount, associatedMap.modCount);
      checkElement(hasNext());

      IdentityHashMapEntry<KT, VT> result = associatedMap.getEntry(position);
      lastPosition = position;
      position += 2;
      canRemove = true;
      return type.get(result);
    }

    public void remove() {
      checkState(canRemove);
      checkConcurrentModification(expectedModCount, associatedMap.modCount);

      canRemove = false;
      associatedMap.remove(associatedMap.elementData[lastPosition]);
      position = lastPosition;
      expectedModCount++;
    }
  }

  static class IdentityHashMapEntrySet<KT, VT> extends AbstractSet<Map.Entry<KT, VT>> {
    private final IdentityHashMap<KT, VT> associatedMap;

    public IdentityHashMapEntrySet(IdentityHashMap<KT, VT> hm) {
      associatedMap = hm;
    }

    IdentityHashMap<KT, VT> hashMap() {
      return associatedMap;
    }

    @Override
    public int size() {
      return associatedMap.size;
    }

    @Override
    public void clear() {
      associatedMap.clear();
    }

    @Override
    public boolean remove(Object object) {
      if (contains(object)) {
        associatedMap.remove(((Map.Entry) object).getKey());
        return true;
      }
      return false;
    }

    @Override
    public boolean contains(Object object) {
      if (object instanceof Map.Entry) {
        IdentityHashMapEntry<?, ?> entry = associatedMap.getEntry(((Map.Entry) object).getKey());
        // we must call equals on the entry obtained from "this"
        return entry != null && entry.equals(object);
      }
      return false;
    }

    @Override
    public Iterator<Map.Entry<KT, VT>> iterator() {
      return new IdentityHashMapIterator<Map.Entry<KT, VT>, KT, VT>(
          new MapEntry.Type<Map.Entry<KT, VT>, KT, VT>() {
            public Map.Entry<KT, VT> get(MapEntry<KT, VT> entry) {
              return entry;
            }
          },
          associatedMap);
    }
  }

  public IdentityHashMap() {
    this(DEFAULT_MAX_SIZE);
  }

  public IdentityHashMap(int maxSize) {
    checkArgument(maxSize >= 0);
    size = 0;
    threshold = getThreshold(maxSize);
    elementData = newElementArray(computeElementArraySize());
  }

  private int getThreshold(int maxSize) {
    // assign the threshold to maxSize initially, this will change to a
    // higher value if rehashing occurs.
    return maxSize > 3 ? maxSize : 3;
  }

  private int computeElementArraySize() {
    int arraySize = (int) (((long) threshold * 10000) / DEFAULT_LOAD_FACTOR) * 2;
    // ensure arraySize is positive, the above cast from long to int type
    // leads to overflow and negative arraySize if threshold is too big
    return arraySize < 0 ? -arraySize : arraySize;
  }

  private Object[] newElementArray(int s) {
    return new Object[s];
  }

  public IdentityHashMap(Map<? extends K, ? extends V> map) {
    this(map.size() < 6 ? 11 : map.size() * 2);
    putAllImpl(map);
  }

  @SuppressWarnings("unchecked")
  private V massageValue(Object value) {
    return (V) ((value == NULL_OBJECT) ? null : value);
  }

  @Override
  public void clear() {
    size = 0;
    for (int i = 0; i < elementData.length; i++) {
      elementData[i] = null;
    }
    modCount++;
  }

  @Override
  public boolean containsKey(Object key) {
    if (key == null) {
      key = NULL_OBJECT;
    }
    int index = findIndex(key, elementData);
    return elementData[index] == key;
  }

  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      value = NULL_OBJECT;
    }
    for (int i = 1; i < elementData.length; i = i + 2) {
      if (elementData[i] == value) {
        return true;
      }
    }
    return false;
  }

  @Override
  public V get(Object key) {
    if (key == null) {
      key = NULL_OBJECT;
    }
    int index = findIndex(key, elementData);
    if (elementData[index] == key) {
      Object result = elementData[index + 1];
      return massageValue(result);
    }
    return null;
  }

  private IdentityHashMapEntry<K, V> getEntry(Object key) {
    if (key == null) {
      key = NULL_OBJECT;
    }
    int index = findIndex(key, elementData);
    if (elementData[index] == key) {
      return getEntry(index);
    }
    return null;
  }

  /** Convenience method for getting the IdentityHashMapEntry without the NULL_OBJECT elements */
  @SuppressWarnings("unchecked")
  private IdentityHashMapEntry<K, V> getEntry(int index) {
    Object key = elementData[index];
    Object value = elementData[index + 1];
    if (key == NULL_OBJECT) {
      key = null;
    }
    if (value == NULL_OBJECT) {
      value = null;
    }
    return new IdentityHashMapEntry<K, V>(this, (K) key, (V) value);
  }

  /**
   * Returns the index where the key is found at, or the index of the next empty spot if the key is
   * not found in this table.
   */
  private int findIndex(Object key, Object[] array) {
    int length = array.length;
    int index = getModuloHash(key, length);
    int last = (index + length - 2) % length;
    while (index != last) {
      if (array[index] == key || (array[index] == null)) {
        // Found the key, or the next empty spot (which means key is not in the table)
        break;
      }
      index = (index + 2) % length;
    }
    return index;
  }

  private int getModuloHash(Object key, int length) {
    return ((HashMap.secondaryHash(System.identityHashCode(key)) & 0x7FFFFFFF) % (length / 2)) * 2;
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return putImpl(key, value, /* onlyIfAbsent= */ true);
  }

  @Override
  public V put(K key, V value) {
    return putImpl(key, value, /* onlyIfAbsent= */ false);
  }

  private V putImpl(K k, V v, boolean onlyIfAbsent) {
    Object key = k == null ? NULL_OBJECT : k;
    Object value = v == null ? NULL_OBJECT : v;
    int index = findIndex(key, elementData);
    // if the key doesn't exist in the table
    if (elementData[index] != key) {
      modCount++;
      if (++size > threshold) {
        rehash();
        index = findIndex(key, elementData);
      }
      // insert the key and assign the value to null initially
      elementData[index] = key;
      elementData[index + 1] = value;
      return null;
    }

    // insert value to where it needs to go, return the old value
    Object result = elementData[index + 1];
    if (!onlyIfAbsent || result == NULL_OBJECT) {
      elementData[index + 1] = value;
    }
    return massageValue(result);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    putAllImpl(map);
  }

  private void rehash() {
    int newlength = elementData.length * 2;
    if (newlength == 0) {
      newlength = 1;
    }
    Object[] newData = newElementArray(newlength);
    for (int i = 0; i < elementData.length; i = i + 2) {
      Object key = elementData[i];
      if (key != null) {
        // if not empty
        int index = findIndex(key, newData);
        newData[index] = key;
        newData[index + 1] = elementData[i + 1];
      }
    }
    elementData = newData;
    computeMaxSize();
  }

  private void computeMaxSize() {
    threshold = (int) ((long) (elementData.length / 2) * DEFAULT_LOAD_FACTOR / 10000);
  }

  @Override
  public V remove(Object key) {
    if (key == null) {
      key = NULL_OBJECT;
    }
    boolean hashedOk;
    int index, next, hash;
    Object result, object;
    index = next = findIndex(key, elementData);
    if (elementData[index] != key) {
      return null;
    }
    // store the value for this key
    result = elementData[index + 1];
    // shift the following elements up if needed
    // until we reach an empty spot
    int length = elementData.length;
    while (true) {
      next = (next + 2) % length;
      object = elementData[next];
      if (object == null) {
        break;
      }
      hash = getModuloHash(object, length);
      hashedOk = hash > index;
      if (next < index) {
        hashedOk = hashedOk || (hash <= next);
      } else {
        hashedOk = hashedOk && (hash <= next);
      }
      if (!hashedOk) {
        elementData[index] = object;
        elementData[index + 1] = elementData[next + 1];
        index = next;
      }
    }
    size--;
    modCount++;
    // clear both the key and the value
    elementData[index] = null;
    elementData[index + 1] = null;
    return massageValue(result);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new IdentityHashMapEntrySet<K, V>(this);
  }

  @Override
  public Set<K> keySet() {
    if (keySet == null) {
      keySet =
          new AbstractSet<K>() {
            @Override
            public boolean contains(Object object) {
              return containsKey(object);
            }

            @Override
            public int size() {
              return IdentityHashMap.this.size();
            }

            @Override
            public void clear() {
              IdentityHashMap.this.clear();
            }

            @Override
            public boolean remove(Object key) {
              if (containsKey(key)) {
                IdentityHashMap.this.remove(key);
                return true;
              }
              return false;
            }

            @Override
            public Iterator<K> iterator() {
              return new IdentityHashMapIterator<K, K, V>(
                  new MapEntry.Type<K, K, V>() {
                    public K get(MapEntry<K, V> entry) {
                      return entry.key;
                    }
                  },
                  IdentityHashMap.this);
            }
          };
    }
    return keySet;
  }

  @Override
  public Collection<V> values() {
    if (valuesCollection == null) {
      valuesCollection =
          new AbstractCollection<V>() {
            @Override
            public boolean contains(Object object) {
              return containsValue(object);
            }

            @Override
            public int size() {
              return IdentityHashMap.this.size();
            }

            @Override
            public void clear() {
              IdentityHashMap.this.clear();
            }

            @Override
            public Iterator<V> iterator() {
              return new IdentityHashMapIterator<V, K, V>(
                  new MapEntry.Type<V, K, V>() {
                    public V get(MapEntry<K, V> entry) {
                      return entry.value;
                    }
                  },
                  IdentityHashMap.this);
            }

            @Override
            public boolean remove(Object object) {
              Iterator<?> it = iterator();
              while (it.hasNext()) {
                if (object == it.next()) {
                  it.remove();
                  return true;
                }
              }
              return false;
            }
          };
    }
    return valuesCollection;
  }

  @Override
  public boolean equals(Object object) {
    /*
     * We need to override the equals method in AbstractMap because
     * AbstractMap.equals will call ((Map) object).entrySet().contains() to
     * determine equality of the entries, so it will defer to the argument
     * for comparison, meaning that reference-based comparison will not take
     * place. We must ensure that all comparison is implemented by methods
     * in this class (or in one of our inner classes) for reference-based
     * comparison to take place.
     */
    if (this == object) {
      return true;
    }
    if (object instanceof Map) {
      Map<?, ?> map = (Map) object;
      if (size() != map.size()) {
        return false;
      }
      Set<Map.Entry<K, V>> set = entrySet();
      // ensure we use the equals method of the set created by "this"
      return set.equals(map.entrySet());
    }
    return false;
  }

  public Object clone() {
    return new IdentityHashMap<K, V>(this);
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  private void putAllImpl(Map<? extends K, ? extends V> map) {
    if (map.entrySet() != null) {
      super.putAll(map);
    }
  }

  private static class MapEntry<K, V> implements Map.Entry<K, V>, Cloneable {

    K key;
    V value;

    interface Type<RT, KT, VT> {
      RT get(MapEntry<KT, VT> entry);
    }

    MapEntry(K theKey) {
      key = theKey;
    }

    MapEntry(K theKey, V theValue) {
      key = theKey;
      value = theValue;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
        return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
            && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
      }
      return false;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    public V setValue(V object) {
      V result = value;
      value = object;
      return result;
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }
  }
}
