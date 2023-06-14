/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
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

// BEGIN android-note
// Completely different implementation from harmony.  Runs much faster.
// BEGIN android-note

package java.util;

import static javaemul.internal.InternalPreconditions.checkConcurrentModification;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkState;

/**
 * LinkedHashMap is an implementation of {@link Map} that guarantees iteration order. All optional
 * operations are supported.
 */
public class LinkedHashMap<K, V> extends HashMap<K, V> {

  /**
   * A dummy entry in the circular linked list of entries in the map. The first real entry is
   * header.nxt, and the last is header.prv. If the map is empty, header.nxt == header && header.prv
   * == header.
   */
  private LinkedEntry<K, V> header = new LinkedEntry<K, V>();

  /** True if access ordered, false if insertion ordered. */
  private final boolean accessOrder;

  /** Constructs a new empty {@code LinkedHashMap} instance. */
  public LinkedHashMap() {
    accessOrder = false;
  }

  public LinkedHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  public LinkedHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, false);
  }

  public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
  }

  public LinkedHashMap(Map<? extends K, ? extends V> map) {
    this(capacityForInitSize(map.size()));
    constructorPutAll(map);
  }

  /** LinkedEntry adds nxt/prv double-links to plain HashMapEntry. */
  private static class LinkedEntry<K, V> extends HashMapEntry<K, V> {
    LinkedEntry<K, V> nxt;
    LinkedEntry<K, V> prv;
    /** Create the header entry */
    LinkedEntry() {
      super(null, null, 0, null);
      nxt = prv = this;
    }
    /** Create a normal entry */
    LinkedEntry(
        K key,
        V value,
        int hash,
        HashMapEntry<K, V> next,
        LinkedEntry<K, V> nxt,
        LinkedEntry<K, V> prv) {
      super(key, value, hash, next);
      this.nxt = nxt;
      this.prv = prv;
    }
  }

  /**
   * Evicts eldest entry if instructed, creates a new entry and links it in as head of linked list.
   * This method should call constructorNewEntry (instead of duplicating code) if the performance of
   * your VM permits.
   *
   * <p>It may seem strange that this method is tasked with adding the entry to the hash table
   * (which is properly the province of our superclass). The alternative of passing the "next" link
   * in to this method and returning the newly created element does not work! If we remove an
   * (eldest) entry that happens to be the first entry in the same bucket as the newly created
   * entry, the "next" link would become invalid, and the resulting hash table corrupt.
   */
  @Override
  void addNewEntry(K key, V value, int hash, int index) {
    LinkedEntry<K, V> header = this.header;
    // Remove eldest entry if instructed to do so.
    LinkedEntry<K, V> eldest = header.nxt;
    if (eldest != header && removeEldestEntry(eldest)) {
      remove(eldest.key);
    }
    // Create new entry, link it on to list, and put it into table
    LinkedEntry<K, V> oldTail = header.prv;
    LinkedEntry<K, V> newTail =
        new LinkedEntry<K, V>(key, value, hash, table[index], header, oldTail);
    table[index] = oldTail.nxt = header.prv = newTail;
  }

  @Override
  void addNewEntryForNullKey(V value) {
    LinkedEntry<K, V> header = this.header;
    // Remove eldest entry if instructed to do so.
    LinkedEntry<K, V> eldest = header.nxt;
    if (eldest != header && removeEldestEntry(eldest)) {
      remove(eldest.key);
    }
    // Create new entry, link it on to list, and put it into table
    LinkedEntry<K, V> oldTail = header.prv;
    LinkedEntry<K, V> newTail = new LinkedEntry<K, V>(null, value, 0, null, header, oldTail);
    entryForNullKey = oldTail.nxt = header.prv = newTail;
  }

  /** As above, but without eviction. */
  @Override
  HashMapEntry<K, V> constructorNewEntry(K key, V value, int hash, HashMapEntry<K, V> next) {
    LinkedEntry<K, V> header = this.header;
    LinkedEntry<K, V> oldTail = header.prv;
    LinkedEntry<K, V> newTail = new LinkedEntry<K, V>(key, value, hash, next, header, oldTail);
    return oldTail.nxt = header.prv = newTail;
  }

  @Override
  public V get(Object key) {
    /*
     * This method is overridden to eliminate the need for a polymorphic
     * invocation in superclass at the expense of code duplication.
     */
    if (key == null) {
      HashMapEntry<K, V> e = entryForNullKey;
      if (e == null) {
        return null;
      }
      if (accessOrder) {
        makeTail((LinkedEntry<K, V>) e);
      }
      return e.value;
    }
    int hash = secondaryHash(key.hashCode());
    HashMapEntry<K, V>[] tab = table;
    for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)]; e != null; e = e.next) {
      K eKey = e.key;
      if (eKey == key || (e.hash == hash && key.equals(eKey))) {
        if (accessOrder) {
          makeTail((LinkedEntry<K, V>) e);
        }
        return e.value;
      }
    }
    return null;
  }

  /**
   * Relinks the given entry to the tail of the list. Under access ordering, this method is invoked
   * whenever the value of a pre-existing entry is read by Map.get or modified by Map.put.
   */
  private void makeTail(LinkedEntry<K, V> e) {
    // Unlink e
    e.prv.nxt = e.nxt;
    e.nxt.prv = e.prv;
    // Relink e as tail
    LinkedEntry<K, V> header = this.header;
    LinkedEntry<K, V> oldTail = header.prv;
    e.nxt = header;
    e.prv = oldTail;
    oldTail.nxt = header.prv = e;
    modCount++;
  }

  @Override
  void preModify(HashMapEntry<K, V> e) {
    if (accessOrder) {
      makeTail((LinkedEntry<K, V>) e);
    }
  }

  @Override
  void postRemove(HashMapEntry<K, V> e) {
    LinkedEntry<K, V> le = (LinkedEntry<K, V>) e;
    le.prv.nxt = le.nxt;
    le.nxt.prv = le.prv;
    le.nxt = le.prv = null; // Help the GC (for performance)
  }

  /**
   * This override is done for LinkedHashMap performance: iteration is cheaper via LinkedHashMap nxt
   * links.
   */
  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      for (LinkedEntry<K, V> header = this.header, e = header.nxt; e != header; e = e.nxt) {
        if (e.value == null) {
          return true;
        }
      }
      return false;
    }

    // value is non-null
    for (LinkedEntry<K, V> header = this.header, e = header.nxt; e != header; e = e.nxt) {
      if (value.equals(e.value)) {
        return true;
      }
    }
    return false;
  }

  public void clear() {
    super.clear();
    // Clear all links to help GC
    LinkedEntry<K, V> header = this.header;
    for (LinkedEntry<K, V> e = header.nxt; e != header; ) {
      LinkedEntry<K, V> nxt = e.nxt;
      e.nxt = e.prv = null;
      e = nxt;
    }
    header.nxt = header.prv = header;
  }

  private abstract class LinkedHashIterator<T> implements Iterator<T> {
    LinkedEntry<K, V> next = header.nxt;
    LinkedEntry<K, V> lastReturned = null;
    int expectedModCount = modCount;

    public final boolean hasNext() {
      return next != header;
    }

    final LinkedEntry<K, V> nextEntry() {
      checkConcurrentModification(modCount, expectedModCount);
      checkElement(next != header);

      LinkedEntry<K, V> e = next;
      next = e.nxt;
      return lastReturned = e;
    }

    public final void remove() {
      checkState(lastReturned != null);
      checkConcurrentModification(modCount, expectedModCount);

      LinkedHashMap.this.remove(lastReturned.key);
      lastReturned = null;
      expectedModCount = modCount;
    }
  }

  private final class KeyIterator extends LinkedHashIterator<K> {
    public final K next() {
      return nextEntry().key;
    }
  }

  private final class ValueIterator extends LinkedHashIterator<V> {
    public final V next() {
      return nextEntry().value;
    }
  }

  private final class EntryIterator extends LinkedHashIterator<Map.Entry<K, V>> {
    public final Map.Entry<K, V> next() {
      return nextEntry();
    }
  }

  // Override view iterator methods to generate correct iteration order

  @Override
  Iterator<K> newKeyIterator() {
    return new KeyIterator();
  }

  @Override
  Iterator<V> newValueIterator() {
    return new ValueIterator();
  }

  @Override
  Iterator<Map.Entry<K, V>> newEntryIterator() {
    return new EntryIterator();
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return false;
  }

  @Override
  public Object clone() {
    return new LinkedHashMap<K, V>(this);
  }
}
