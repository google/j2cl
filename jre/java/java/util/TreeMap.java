/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.util;

import static javaemul.internal.InternalPreconditions.checkConcurrentModification;
import static javaemul.internal.InternalPreconditions.checkCriticalArgument;
import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkState;
import static javaemul.internal.InternalPreconditions.isApiChecked;

import java.io.Serializable;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsNonNull;

/**
 * A map whose entries are sorted by their keys. All optional operations such as {@link #put} and
 * {@link #remove} are supported.
 */
public class TreeMap<K, V> extends AbstractMap<K, V>
    implements SortedMap<K, V>, NavigableMap<K, V>, Serializable {

  private Comparator<? super K> comparator;
  private Node<K, V> root;
  private int size;
  private int modCount;

  public TreeMap() {
    this((Comparator<? super K>) null);
  }

  public TreeMap(Comparator<? super K> comparator) {
    this.comparator = Comparators.nullToNaturalOrder(comparator);
  }

  public TreeMap(Map<? extends K, ? extends V> copyFrom) {
    this();
    putAll(copyFrom);
  }

  public TreeMap(SortedMap<K, ? extends V> copyFrom) {
    this(copyFrom.comparator());
    putAll(copyFrom);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public V get(Object key) {
    Entry<K, V> entry = findByObject(key);
    return entry != null ? entry.getValue() : null;
  }

  @Override
  public boolean containsKey(Object key) {
    return findByObject(key) != null;
  }

  @Override
  public V put(K key, V value) {
    return putInternal(key, value);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return putInternalIfAbsent(key, value);
  }

  @Override
  public void clear() {
    root = null;
    size = 0;
    structureChanged();
  }

  private void structureChanged() {
    if (!isApiChecked()) {
      // Shouldn't be necessary but JsCompiler chokes on removing modCount so make sure we don't pay
      // cost for updating the field.
      return;
    }
    this.modCount++;
  }

  @Override
  public V remove(Object key) {
    Node<K, V> node = removeInternalByKey(key);
    return node != null ? node.getValue() : null;
  }

  /*
   * AVL methods
   */
  @JsEnum
  private enum Relation {
    LOWER,
    FLOOR,
    CEILING,
    HIGHER,
    CREATE;

    /**
     * Returns a possibly-flipped relation for use in descending views.
     *
     * @param ascending false to flip; true to return this.
     */
    Relation forOrder(boolean ascending) {
      if (ascending) {
        return this;
      }
      switch (this) {
        case LOWER:
          return HIGHER;
        case FLOOR:
          return CEILING;
        case CEILING:
          return FLOOR;
        case HIGHER:
          return LOWER;
        default:
          throw new IllegalStateException();
      }
    }
  }

  private V putInternal(K key, V value) {
    return find(key, Relation.CREATE).setValue(value);
  }

  private V putInternalIfAbsent(K key, V value) {
    int prevSize = size;
    Node<K, V> node = find(key, Relation.CREATE);
    if (prevSize == size) {
      // The node already exists, not a new entry.
      return node.getValue();
    }
    return node.setValue(value);
  }

  /** Returns the node at or adjacent to the given key, creating it if requested. */
  private Node<K, V> find(K key, Relation relation) {
    if (root == null) {
      if (relation == Relation.CREATE) {
        root = new Node<K, V>(null, key);
        size = 1;
        structureChanged();
        return root;
      } else {
        return null;
      }
    }
    Node<K, V> nearest = root;
    while (true) {
      int comparison = comparator.compare(key, nearest.getKey());

      // Found the requested key.
      if (comparison == 0) {
        switch (relation) {
          case LOWER:
            return nearest.prev();
          case FLOOR:
          case CEILING:
            return nearest;
          case HIGHER:
            return nearest.next();
          case CREATE:
            return nearest;
        }
      }

      Node<K, V> child = (comparison < 0) ? nearest.left : nearest.right;
      if (child == null) {
        // Found a nearest node.
        // Every key not in the tree has up to two nearest nodes, one lower and one higher.
        switch (relation) {
          case LOWER:
          case FLOOR:
            return comparison < 0 ? nearest.prev() : nearest;
          case CEILING:
          case HIGHER:
            return comparison < 0 ? nearest : nearest.next();
          case CREATE:
            Node<K, V> created = new Node<K, V>(nearest, key);
            if (comparison < 0) {
              nearest.left = created;
            } else {
              nearest.right = created;
            }
            size++;
            structureChanged();
            rebalance(nearest, true);
            return created;
        }
      }
      nearest = child;
    }
  }

  private K findKey(K key, Relation relation) {
    return getKeyOrNull(find(key, relation));
  }

  private Entry<K, V> findEntry(K key, Relation relation) {
    return immutableCopy(find(key, relation));
  }

  @SuppressWarnings("unchecked")
  private Node<K, V> findByObject(Object key) {
    Node<K, V> node = root;
    while (node != null) {
      int c = comparator.compare((K) key, node.getKey());
      if (c == 0) {
        return node;
      }
      node = c < 0 ? node.left : node.right;
    }
    return null;
  }

  /**
   * Returns this map's entry that has the same key and value as {@code entry}, or null if this map
   * has no such entry.
   *
   * <p>This method uses the comparator for key equality rather than {@code equals}. If this map's
   * comparator isn't consistent with equals (such as {@code String.CASE_INSENSITIVE_ORDER}), then
   * {@code remove()} and {@code contains()} will violate the collections API.
   */
  private Node<K, V> findByEntry(Entry<?, ?> entry) {
    Node<K, V> mine = findByObject(entry.getKey());
    boolean valuesEqual = mine != null && Objects.equals(mine.getValue(), entry.getValue());
    return valuesEqual ? mine : null;
  }

  /** Removes {@code node} from this tree, rearranging the tree's structure as necessary. */
  private void removeInternal(Node<K, V> node) {
    Node<K, V> left = node.left;
    Node<K, V> right = node.right;
    Node<K, V> originalParent = node.parent;
    if (left != null && right != null) {
      /*
       * To remove a node with both left and right subtrees, move an
       * adjacent node from one of those subtrees into this node's place.
       *
       * Removing the adjacent node may change this node's subtrees. This
       * node may no longer have two subtrees once the adjacent node is
       * gone!
       */
      Node<K, V> adjacent = (left.height > right.height) ? left.last() : right.first();
      removeInternal(adjacent); // takes care of rebalance and size--
      int leftHeight = 0;
      left = node.left;
      if (left != null) {
        leftHeight = left.height;
        adjacent.left = left;
        left.parent = adjacent;
        node.left = null;
      }
      int rightHeight = 0;
      right = node.right;
      if (right != null) {
        rightHeight = right.height;
        adjacent.right = right;
        right.parent = adjacent;
        node.right = null;
      }
      adjacent.height = Math.max(leftHeight, rightHeight) + 1;
      replaceInParent(node, adjacent);
      return;
    } else if (left != null) {
      replaceInParent(node, left);
      node.left = null;
    } else if (right != null) {
      replaceInParent(node, right);
      node.right = null;
    } else {
      replaceInParent(node, null);
    }
    rebalance(originalParent, false);
    size--;
    structureChanged();
  }

  private Node<K, V> removeInternalByKey(Object key) {
    Node<K, V> node = findByObject(key);
    if (node != null) {
      removeInternal(node);
    }
    return node;
  }

  private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
    Node<K, V> parent = node.parent;
    node.parent = null;
    if (replacement != null) {
      replacement.parent = parent;
    }
    if (parent != null) {
      if (parent.left == node) {
        parent.left = replacement;
      } else {
        // assert (parent.right == node);
        parent.right = replacement;
      }
    } else {
      root = replacement;
    }
  }

  /**
   * Rebalances the tree by making any AVL rotations necessary between the newly-unbalanced node and
   * the tree's root.
   *
   * @param insert true if the node was unbalanced by an insert; false if it was by a removal.
   */
  private void rebalance(Node<K, V> unbalanced, boolean insert) {
    for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
      Node<K, V> left = node.left;
      Node<K, V> right = node.right;
      int leftHeight = left != null ? left.height : 0;
      int rightHeight = right != null ? right.height : 0;
      int delta = leftHeight - rightHeight;
      if (delta == -2) {
        Node<K, V> rightLeft = right.left;
        Node<K, V> rightRight = right.right;
        int rightRightHeight = rightRight != null ? rightRight.height : 0;
        int rightLeftHeight = rightLeft != null ? rightLeft.height : 0;
        int rightDelta = rightLeftHeight - rightRightHeight;
        if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
          rotateLeft(node); // AVL right right
        } else {
          // assert (rightDelta == 1);
          rotateRight(right); // AVL right left
          rotateLeft(node);
        }
        if (insert) {
          break; // no further rotations will be necessary
        }
      } else if (delta == 2) {
        Node<K, V> leftLeft = left.left;
        Node<K, V> leftRight = left.right;
        int leftRightHeight = leftRight != null ? leftRight.height : 0;
        int leftLeftHeight = leftLeft != null ? leftLeft.height : 0;
        int leftDelta = leftLeftHeight - leftRightHeight;
        if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
          rotateRight(node); // AVL left left
        } else {
          // assert (leftDelta == -1);
          rotateLeft(left); // AVL left right
          rotateRight(node);
        }
        if (insert) {
          break; // no further rotations will be necessary
        }
      } else if (delta == 0) {
        node.height = leftHeight + 1; // leftHeight == rightHeight
        if (insert) {
          break; // the insert caused balance, so rebalancing is done!
        }
      } else {
        // assert (delta == -1 || delta == 1);
        node.height = Math.max(leftHeight, rightHeight) + 1;
        if (!insert) {
          break; // the height hasn't changed, so rebalancing is done!
        }
      }
    }
  }

  /** Rotates the subtree so that its root's right child is the new root. */
  private void rotateLeft(Node<K, V> root) {
    Node<K, V> left = root.left;
    Node<K, V> pivot = root.right;
    Node<K, V> pivotLeft = pivot.left;
    Node<K, V> pivotRight = pivot.right;
    // move the pivot's left child to the root's right
    root.right = pivotLeft;
    if (pivotLeft != null) {
      pivotLeft.parent = root;
    }
    replaceInParent(root, pivot);
    // move the root to the pivot's left
    pivot.left = root;
    root.parent = pivot;
    // fix heights
    root.height =
        Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
    pivot.height = Math.max(root.height, pivotRight != null ? pivotRight.height : 0) + 1;
  }

  /** Rotates the subtree so that its root's left child is the new root. */
  private void rotateRight(Node<K, V> root) {
    Node<K, V> pivot = root.left;
    Node<K, V> right = root.right;
    Node<K, V> pivotLeft = pivot.left;
    Node<K, V> pivotRight = pivot.right;
    // move the pivot's right child to the root's left
    root.left = pivotRight;
    if (pivotRight != null) {
      pivotRight.parent = root;
    }
    replaceInParent(root, pivot);
    // move the root to the pivot's right
    pivot.right = root;
    root.parent = pivot;
    // fixup heights
    root.height =
        Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
    pivot.height = Math.max(root.height, pivotLeft != null ? pivotLeft.height : 0) + 1;
  }

  /*
   * Navigable methods.
   */

  /**
   * Returns an immutable version of {@param entry}. Need this because we allow entry to be null, in
   * which case we return a null SimpleImmutableEntry.
   */
  private SimpleImmutableEntry<K, V> immutableCopy(Entry<K, V> entry) {
    return entry == null ? null : new SimpleImmutableEntry<K, V>(entry);
  }

  private Node<K, V> getFirst() {
    return root == null ? null : root.first();
  }

  private Node<K, V> getLast() {
    return root == null ? null : root.last();
  }

  @Override
  public Entry<K, V> firstEntry() {
    return immutableCopy(getFirst());
  }

  private Node<K, V> internalPollFirstEntry() {
    if (root == null) {
      return null;
    }
    Node<K, V> result = root.first();
    removeInternal(result);
    return result;
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return immutableCopy(internalPollFirstEntry());
  }

  @Override
  public K firstKey() {
    checkElement(root != null);
    return root.first().getKey();
  }

  @Override
  public Entry<K, V> lastEntry() {
    return immutableCopy(getLast());
  }

  private Entry<K, V> internalPollLastEntry() {
    if (root == null) {
      return null;
    }
    Node<K, V> result = root.last();
    removeInternal(result);
    return result;
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return immutableCopy(internalPollLastEntry());
  }

  @Override
  public K lastKey() {
    checkElement(root != null);
    return root.last().getKey();
  }

  @Override
  public Entry<K, V> lowerEntry(K key) {
    return findEntry(key, Relation.LOWER);
  }

  @Override
  public K lowerKey(K key) {
    return findKey(key, Relation.LOWER);
  }

  @Override
  public Entry<K, V> floorEntry(K key) {
    return findEntry(key, Relation.FLOOR);
  }

  @Override
  public K floorKey(K key) {
    return findKey(key, Relation.FLOOR);
  }

  @Override
  public Entry<K, V> ceilingEntry(K key) {
    return findEntry(key, Relation.CEILING);
  }

  @Override
  public K ceilingKey(K key) {
    return findKey(key, Relation.CEILING);
  }

  @Override
  public Entry<K, V> higherEntry(K key) {
    return findEntry(key, Relation.HIGHER);
  }

  @Override
  public K higherKey(K key) {
    return findKey(key, Relation.HIGHER);
  }

  @Override
  public Comparator<? super K> comparator() {
    return Comparators.naturalOrderToNull(comparator);
  }

  /*
   * View factory methods.
   */

  private EntrySet entrySet;
  private KeySet keySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new EntrySet();
    }
    return entrySet;
  }

  @Override
  public @JsNonNull Set<K> keySet() {
    return navigableKeySet();
  }

  @Override
  public NavigableSet<K> navigableKeySet() {
    if (keySet == null) {
      keySet = new KeySet();
    }
    return keySet;
  }

  @Override
  public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
    Bound fromBound = fromInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
    Bound toBound = toInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
    return new BoundedMap(true, from, fromBound, to, toBound);
  }

  @Override
  public SortedMap<K, V> subMap(K fromInclusive, K toExclusive) {
    return new BoundedMap(true, fromInclusive, Bound.INCLUSIVE, toExclusive, Bound.EXCLUSIVE);
  }

  @Override
  public NavigableMap<K, V> headMap(K to, boolean inclusive) {
    Bound toBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
    return new BoundedMap(true, null, Bound.NO_BOUND, to, toBound);
  }

  @Override
  public SortedMap<K, V> headMap(K toExclusive) {
    return new BoundedMap(true, null, Bound.NO_BOUND, toExclusive, Bound.EXCLUSIVE);
  }

  @Override
  public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
    Bound fromBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
    return new BoundedMap(true, from, fromBound, null, Bound.NO_BOUND);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromInclusive) {
    return new BoundedMap(true, fromInclusive, Bound.INCLUSIVE, null, Bound.NO_BOUND);
  }

  @Override
  public NavigableMap<K, V> descendingMap() {
    return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND);
  }

  @Override
  public NavigableSet<K> descendingKeySet() {
    return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND).navigableKeySet();
  }

  private static class Node<K, V> extends SimpleEntry<K, V> {
    Node<K, V> parent;
    Node<K, V> left;
    Node<K, V> right;
    int height;

    Node(Node<K, V> parent, K key) {
      super(key, null);
      this.parent = parent;
      this.height = 1;
    }

    /**
     * Returns the next node in an inorder traversal, or null if this is the last node in the tree.
     */
    Node<K, V> next() {
      if (right != null) {
        return right.first();
      }
      Node<K, V> node = this;
      Node<K, V> parent = node.parent;
      while (parent != null) {
        if (parent.left == node) {
          return parent;
        }
        node = parent;
        parent = node.parent;
      }
      return null;
    }

    /**
     * Returns the previous node in an inorder traversal, or null if this is the first node in the
     * tree.
     */
    Node<K, V> prev() {
      if (left != null) {
        return left.last();
      }
      Node<K, V> node = this;
      Node<K, V> parent = node.parent;
      while (parent != null) {
        if (parent.right == node) {
          return parent;
        }
        node = parent;
        parent = node.parent;
      }
      return null;
    }

    /** Returns the first node in this subtree. */
    Node<K, V> first() {
      Node<K, V> node = this;
      Node<K, V> child = node.left;
      while (child != null) {
        node = child;
        child = node.left;
      }
      return node;
    }

    /** Returns the last node in this subtree. */
    Node<K, V> last() {
      Node<K, V> node = this;
      Node<K, V> child = node.right;
      while (child != null) {
        node = child;
        child = node.right;
      }
      return node;
    }
  }

  /**
   * Walk the nodes of the tree left-to-right or right-to-left. Note that in descending iterations,
   * {@code next} will return the previous node.
   */
  private abstract class MapIterator<T> implements Iterator<T> {
    protected Node<K, V> next;
    protected Node<K, V> last;
    protected int expectedModCount = modCount;

    MapIterator(Node<K, V> next) {
      this.next = next;
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    protected Node<K, V> stepForward() {
      checkElement(next != null);
      checkConcurrentModification(modCount, expectedModCount);
      last = next;
      next = next.next();
      return last;
    }

    protected Node<K, V> stepBackward() {
      checkElement(next != null);
      checkConcurrentModification(modCount, expectedModCount);
      last = next;
      next = next.prev();
      return last;
    }

    @Override
    public void remove() {
      checkState(last != null);
      removeInternal(last);
      expectedModCount = modCount;
      last = null;
    }
  }

  /*
   * View implementations.
   */

  private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new MapIterator<Entry<K, V>>(getFirst()) {
        @Override
        public Entry<K, V> next() {
          return stepForward();
        }
      };
    }

    @Override
    public boolean contains(Object o) {
      return o instanceof Entry && findByEntry((Entry<?, ?>) o) != null;
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }
      Node<K, V> node = findByEntry((Entry<?, ?>) o);
      if (node == null) {
        return false;
      }
      removeInternal(node);
      return true;
    }

    @Override
    public void clear() {
      TreeMap.this.clear();
    }
  }

  private class KeySet extends AbstractSet<K> implements NavigableSet<K> {
    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<K> iterator() {
      return new MapIterator<K>(getFirst()) {
        @Override
        public K next() {
          return stepForward().getKey();
        }
      };
    }

    @Override
    public Iterator<K> descendingIterator() {
      return new MapIterator<K>(getLast()) {
        @Override
        public K next() {
          return stepBackward().getKey();
        }
      };
    }

    @Override
    public boolean contains(Object o) {
      return containsKey(o);
    }

    @Override
    public boolean remove(Object key) {
      return removeInternalByKey(key) != null;
    }

    @Override
    public void clear() {
      TreeMap.this.clear();
    }

    @Override
    public Comparator<? super K> comparator() {
      return TreeMap.this.comparator();
    }

    /*
     * Navigable methods.
     */

    @Override
    public K first() {
      return firstKey();
    }

    @Override
    public K last() {
      return lastKey();
    }

    @Override
    public K lower(K key) {
      return lowerKey(key);
    }

    @Override
    public K floor(K key) {
      return floorKey(key);
    }

    @Override
    public K ceiling(K key) {
      return ceilingKey(key);
    }

    @Override
    public K higher(K key) {
      return higherKey(key);
    }

    @Override
    public K pollFirst() {
      return getKeyOrNull(internalPollFirstEntry());
    }

    @Override
    public K pollLast() {
      return getKeyOrNull(internalPollLastEntry());
    }

    /*
     * View factory methods.
     */

    @Override
    public NavigableSet<K> subSet(K from, boolean fromInclusive, K to, boolean toInclusive) {
      return TreeMap.this.subMap(from, fromInclusive, to, toInclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> subSet(K fromInclusive, K toExclusive) {
      return TreeMap.this.subMap(fromInclusive, true, toExclusive, false).navigableKeySet();
    }

    @Override
    public NavigableSet<K> headSet(K to, boolean inclusive) {
      return TreeMap.this.headMap(to, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> headSet(K toExclusive) {
      return TreeMap.this.headMap(toExclusive, false).navigableKeySet();
    }

    @Override
    public NavigableSet<K> tailSet(K from, boolean inclusive) {
      return TreeMap.this.tailMap(from, inclusive).navigableKeySet();
    }

    @Override
    public SortedSet<K> tailSet(K fromInclusive) {
      return TreeMap.this.tailMap(fromInclusive, true).navigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingSet() {
      return new BoundedMap(false, null, Bound.NO_BOUND, null, Bound.NO_BOUND).navigableKeySet();
    }
  }

  /*
   * Bounded views implementations.
   */

  @JsEnum
  private enum Bound {
    INCLUSIVE,
    EXCLUSIVE,
    NO_BOUND
  }

  /** A map with optional limits on its range. */
  private final class BoundedMap extends AbstractMap<K, V>
      implements NavigableMap<K, V>, Serializable {
    private final boolean ascending;
    private final K from;
    private final Bound fromBound;
    private final K to;
    private final Bound toBound;

    BoundedMap(boolean ascending, K from, Bound fromBound, K to, Bound toBound) {
      /*
       * Validate the bounds. In addition to checking that from <= to, we
       * verify that the comparator supports our bound objects.
       */
      if (fromBound != Bound.NO_BOUND && toBound != Bound.NO_BOUND) {
        checkCriticalArgument(comparator.compare(from, to) <= 0);
      } else if (fromBound != Bound.NO_BOUND) {
        int unused = comparator.compare(from, from);
      } else if (toBound != Bound.NO_BOUND) {
        int unused = comparator.compare(to, to);
      }
      this.ascending = ascending;
      this.from = from;
      this.fromBound = fromBound;
      this.to = to;
      this.toBound = toBound;
    }

    @Override
    public boolean isEmpty() {
      return endpoint(true) == null;
    }

    @Override
    public V get(Object key) {
      return isInBounds(key) ? TreeMap.this.get(key) : null;
    }

    @Override
    public boolean containsKey(Object key) {
      return isInBounds(key) && TreeMap.this.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
      checkInBounds(key, fromBound, toBound);
      return putInternal(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
      checkInBounds(key, fromBound, toBound);
      return putInternalIfAbsent(key, value);
    }

    @Override
    public V remove(Object key) {
      return isInBounds(key) ? TreeMap.this.remove(key) : null;
    }

    /** Returns true if the key is in bounds. */
    @SuppressWarnings("unchecked")
    private boolean isInBounds(Object key) {
      return isInBounds((K) key, fromBound, toBound);
    }

    /**
     * Returns true if the key is in bounds. Use this overload with Bound.NO_BOUND to skip bounds
     * checking on either end.
     */
    private boolean isInBounds(K key, Bound fromBound, Bound toBound) {
      if (fromBound == Bound.INCLUSIVE) {
        if (comparator.compare(key, from) < 0) {
          return false; // less than from
        }
      } else if (fromBound == Bound.EXCLUSIVE) {
        if (comparator.compare(key, from) <= 0) {
          return false; // less than or equal to from
        }
      }
      if (toBound == Bound.INCLUSIVE) {
        if (comparator.compare(key, to) > 0) {
          return false; // greater than 'to'
        }
      } else if (toBound == Bound.EXCLUSIVE) {
        if (comparator.compare(key, to) >= 0) {
          return false; // greater than or equal to 'to'
        }
      }
      return true;
    }

    private void checkInBounds(K key, Bound fromBound, Bound toBound) {
      if (!isInBounds(key, fromBound, toBound)) {
        checkCriticalArgument(false, key + " not in range " + from + ".." + to);
      }
    }

    /** Returns the entry if it is in bounds, or null if it is out of bounds. */
    private Node<K, V> bound(Node<K, V> node, Bound fromBound, Bound toBound) {
      return node != null && isInBounds(node.getKey(), fromBound, toBound) ? node : null;
    }

    /*
     * Navigable methods.
     */

    @Override
    public Entry<K, V> firstEntry() {
      return immutableCopy(endpoint(true));
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
      Node<K, V> result = endpoint(true);
      if (result != null) {
        removeInternal(result);
      }
      return immutableCopy(result);
    }

    @Override
    public K firstKey() {
      Entry<K, V> entry = endpoint(true);
      checkElement(entry != null);
      return entry.getKey();
    }

    @Override
    public Entry<K, V> lastEntry() {
      return immutableCopy(endpoint(false));
    }

    @Override
    public Entry<K, V> pollLastEntry() {
      Node<K, V> result = endpoint(false);
      if (result != null) {
        removeInternal(result);
      }
      return immutableCopy(result);
    }

    @Override
    public K lastKey() {
      Entry<K, V> entry = endpoint(false);
      checkElement(entry != null);
      return entry.getKey();
    }

    /**
     * @param first true for the first element, false for the last.
     */
    private Node<K, V> endpoint(boolean first) {
      Node<K, V> node = null;
      if (ascending == first) {
        switch (fromBound) {
          case NO_BOUND:
            node = getFirst();
            break;
          case INCLUSIVE:
            node = find(from, Relation.CEILING);
            break;
          case EXCLUSIVE:
            node = find(from, Relation.HIGHER);
            break;
        }
        return bound(node, Bound.NO_BOUND, toBound);
      } else {
        switch (toBound) {
          case NO_BOUND:
            node = getLast();
            break;
          case INCLUSIVE:
            node = find(to, Relation.FLOOR);
            break;
          case EXCLUSIVE:
            node = find(to, Relation.LOWER);
            break;
        }
        return bound(node, fromBound, Bound.NO_BOUND);
      }
    }

    /**
     * Performs a find on the underlying tree after constraining it to the bounds of this view.
     * Examples:
     *
     * <p>bound is (A..C) findBounded(B, FLOOR) stays source.find(B, FLOOR)
     *
     * <p>bound is (A..C) findBounded(C, FLOOR) becomes source.find(C, LOWER)
     *
     * <p>bound is (A..C) findBounded(D, LOWER) becomes source.find(C, LOWER)
     *
     * <p>bound is (A..C] findBounded(D, FLOOR) becomes source.find(C, FLOOR)
     *
     * <p>bound is (A..C] findBounded(D, LOWER) becomes source.find(C, FLOOR)
     */
    private Node<K, V> findBounded(K key, Relation relation) {
      relation = relation.forOrder(ascending);
      Bound fromBoundForCheck = fromBound;
      Bound toBoundForCheck = toBound;
      if (toBound != Bound.NO_BOUND && (relation == Relation.LOWER || relation == Relation.FLOOR)) {
        int comparison = comparator.compare(to, key);
        if (comparison <= 0) {
          key = to;
          if (toBound == Bound.EXCLUSIVE) {
            relation = Relation.LOWER; // 'to' is too high
          } else if (comparison < 0) {
            relation = Relation.FLOOR; // we already went lower
          }
        }
        toBoundForCheck = Bound.NO_BOUND; // we've already checked the upper bound
      }
      if (fromBound != Bound.NO_BOUND
          && (relation == Relation.CEILING || relation == Relation.HIGHER)) {
        int comparison = comparator.compare(from, key);
        if (comparison >= 0) {
          key = from;
          if (fromBound == Bound.EXCLUSIVE) {
            relation = Relation.HIGHER; // 'from' is too low
          } else if (comparison > 0) {
            relation = Relation.CEILING; // we already went higher
          }
        }
        fromBoundForCheck = Bound.NO_BOUND; // we've already checked the lower bound
      }
      return bound(find(key, relation), fromBoundForCheck, toBoundForCheck);
    }

    private K findBoundedKey(K key, Relation relation) {
      return getKeyOrNull(findBounded(key, relation));
    }

    private Entry<K, V> findBoundedEntry(K key, Relation relation) {
      return immutableCopy(findBounded(key, relation));
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
      return findBoundedEntry(key, Relation.LOWER);
    }

    @Override
    public K lowerKey(K key) {
      return findBoundedKey(key, Relation.LOWER);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
      return findBoundedEntry(key, Relation.FLOOR);
    }

    @Override
    public K floorKey(K key) {
      return findBoundedKey(key, Relation.FLOOR);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
      return findBoundedEntry(key, Relation.CEILING);
    }

    @Override
    public K ceilingKey(K key) {
      return findBoundedKey(key, Relation.CEILING);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
      return findBoundedEntry(key, Relation.HIGHER);
    }

    @Override
    public K higherKey(K key) {
      return findBoundedKey(key, Relation.HIGHER);
    }

    @Override
    public Comparator<? super K> comparator() {
      Comparator<? super K> forward = TreeMap.this.comparator();
      if (ascending) {
        return forward;
      } else {
        return Collections.reverseOrder(forward);
      }
    }

    /*
     * View factory methods.
     */

    private BoundedEntrySet entrySet;
    private BoundedKeySet keySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
      if (entrySet == null) {
        entrySet = new BoundedEntrySet();
      }
      return entrySet;
    }

    @Override
    public @JsNonNull Set<K> keySet() {
      return navigableKeySet();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
      if (keySet == null) {
        keySet = new BoundedKeySet();
      }
      return keySet;
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
      return new BoundedMap(!ascending, from, fromBound, to, toBound);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
      return new BoundedMap(!ascending, from, fromBound, to, toBound).navigableKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
      Bound fromBound = fromInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
      Bound toBound = toInclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
      return subMap(from, fromBound, to, toBound);
    }

    @Override
    public NavigableMap<K, V> subMap(K fromInclusive, K toExclusive) {
      return subMap(fromInclusive, Bound.INCLUSIVE, toExclusive, Bound.EXCLUSIVE);
    }

    @Override
    public NavigableMap<K, V> headMap(K to, boolean inclusive) {
      Bound toBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
      return subMap(null, Bound.NO_BOUND, to, toBound);
    }

    @Override
    public NavigableMap<K, V> headMap(K toExclusive) {
      return subMap(null, Bound.NO_BOUND, toExclusive, Bound.EXCLUSIVE);
    }

    @Override
    public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
      Bound fromBound = inclusive ? Bound.INCLUSIVE : Bound.EXCLUSIVE;
      return subMap(from, fromBound, null, Bound.NO_BOUND);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromInclusive) {
      return subMap(fromInclusive, Bound.INCLUSIVE, null, Bound.NO_BOUND);
    }

    private NavigableMap<K, V> subMap(K from, Bound fromBound, K to, Bound toBound) {
      if (!ascending) {
        K fromTmp = from;
        Bound fromBoundTmp = fromBound;
        from = to;
        fromBound = toBound;
        to = fromTmp;
        toBound = fromBoundTmp;
      }
      /*
       * If both the current and requested bounds are exclusive, the isInBounds check must be
       * inclusive. For example, to create (C..F) from (A..F), the bound 'F' is in bounds.
       */
      if (fromBound == Bound.NO_BOUND) {
        from = this.from;
        fromBound = this.fromBound;
      } else {
        Bound fromBoundToCheck = fromBound == this.fromBound ? Bound.INCLUSIVE : this.fromBound;
        checkInBounds(from, fromBoundToCheck, this.toBound);
      }
      if (toBound == Bound.NO_BOUND) {
        to = this.to;
        toBound = this.toBound;
      } else {
        Bound toBoundToCheck = toBound == this.toBound ? Bound.INCLUSIVE : this.toBound;
        checkInBounds(to, this.fromBound, toBoundToCheck);
      }
      return new BoundedMap(ascending, from, fromBound, to, toBound);
    }

    /*
     * Bounded view implementations.
     */

    private abstract class BoundedIterator<T> extends MapIterator<T> {
      protected BoundedIterator(Node<K, V> next) {
        super(next);
      }

      @Override
      protected Node<K, V> stepForward() {
        Node<K, V> result = super.stepForward();
        next = bound(next, Bound.NO_BOUND, toBound);
        return result;
      }

      @Override
      protected Node<K, V> stepBackward() {
        Node<K, V> result = super.stepBackward();
        next = bound(next, fromBound, Bound.NO_BOUND);
        return result;
      }
    }

    private final class BoundedEntrySet extends AbstractSet<Entry<K, V>> {
      @Override
      public int size() {
        int count = 0;
        for (Entry<K, V> ignored : this) {
          count++;
        }
        return count;
      }

      @Override
      public boolean isEmpty() {
        return BoundedMap.this.isEmpty();
      }

      @Override
      public Iterator<Entry<K, V>> iterator() {
        return new BoundedIterator<Entry<K, V>>(endpoint(true)) {
          @Override
          public Entry<K, V> next() {
            return ascending ? stepForward() : stepBackward();
          }
        };
      }

      @Override
      public boolean contains(Object o) {
        if (!(o instanceof Entry)) {
          return false;
        }
        Entry<?, ?> entry = (Entry<?, ?>) o;
        return isInBounds(entry.getKey()) && findByEntry(entry) != null;
      }

      @Override
      public boolean remove(Object o) {
        if (!(o instanceof Entry)) {
          return false;
        }
        Entry<?, ?> entry = (Entry<?, ?>) o;
        return isInBounds(entry.getKey()) && TreeMap.this.entrySet().remove(entry);
      }
    }

    private final class BoundedKeySet extends AbstractSet<K> implements NavigableSet<K> {
      @Override
      public int size() {
        return BoundedMap.this.size();
      }

      @Override
      public boolean isEmpty() {
        return BoundedMap.this.isEmpty();
      }

      @Override
      public Iterator<K> iterator() {
        return new BoundedIterator<K>(endpoint(true)) {
          @Override
          public K next() {
            return (ascending ? stepForward() : stepBackward()).getKey();
          }
        };
      }

      @Override
      public Iterator<K> descendingIterator() {
        return new BoundedIterator<K>(endpoint(false)) {
          @Override
          public K next() {
            return (ascending ? stepBackward() : stepForward()).getKey();
          }
        };
      }

      @Override
      public boolean contains(Object key) {
        return isInBounds(key) && findByObject(key) != null;
      }

      @Override
      public boolean remove(Object key) {
        return isInBounds(key) && removeInternalByKey(key) != null;
      }

      /*
       * Navigable methods.
       */

      @Override
      public K first() {
        return firstKey();
      }

      @Override
      public K pollFirst() {
        return getKeyOrNull(internalPollFirstEntry());
      }

      @Override
      public K last() {
        return lastKey();
      }

      @Override
      public K pollLast() {
        return getKeyOrNull(internalPollLastEntry());
      }

      @Override
      public K lower(K key) {
        return lowerKey(key);
      }

      @Override
      public K floor(K key) {
        return floorKey(key);
      }

      @Override
      public K ceiling(K key) {
        return ceilingKey(key);
      }

      @Override
      public K higher(K key) {
        return higherKey(key);
      }

      @Override
      public Comparator<? super K> comparator() {
        return BoundedMap.this.comparator();
      }

      /*
       * View factory methods.
       */

      @Override
      public NavigableSet<K> subSet(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return subMap(from, fromInclusive, to, toInclusive).navigableKeySet();
      }

      @Override
      public SortedSet<K> subSet(K fromInclusive, K toExclusive) {
        return subMap(fromInclusive, toExclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> headSet(K to, boolean inclusive) {
        return headMap(to, inclusive).navigableKeySet();
      }

      @Override
      public SortedSet<K> headSet(K toExclusive) {
        return headMap(toExclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> tailSet(K from, boolean inclusive) {
        return tailMap(from, inclusive).navigableKeySet();
      }

      @Override
      public SortedSet<K> tailSet(K fromInclusive) {
        return tailMap(fromInclusive).navigableKeySet();
      }

      @Override
      public NavigableSet<K> descendingSet() {
        return new BoundedMap(!ascending, from, fromBound, to, toBound).navigableKeySet();
      }
    }
  }

  private static <K> K getKeyOrNull(Entry<K, ?> entry) {
    return entry == null ? null : entry.getKey();
  }
}
