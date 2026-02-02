/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.jre.java.util;

import static com.google.j2cl.jre.testing.TestUtils.isWasm;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import com.google.j2cl.jre.testing.TestUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Tests ArrayDeque class. */
@NullMarked
public class ArrayDequeTest extends TestCollection {

  public void testAdd() throws Exception {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertTrue(deque.add(o1));
    checkDequeSizeAndContent(deque, o1);
    assertTrue(deque.add(o2));
    checkDequeSizeAndContent(deque, o1, o2);
    assertTrue(deque.add(o3));
    checkDequeSizeAndContent(deque, o1, o2, o3);

    assertThrows(NullPointerException.class, () -> deque.add(null));
  }

  public void testAddAll() throws Exception {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertTrue(deque.addAll(asList(o1, o2)));
    checkDequeSizeAndContent(deque, o1, o2);
    assertThrows(NullPointerException.class, () -> deque.addAll(asList(o1, null, o2)));
  }

  public void testAddFirst() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    deque.addFirst(o1);
    checkDequeSizeAndContent(deque, o1);
    deque.addFirst(o2);
    checkDequeSizeAndContent(deque, o2, o1);
    deque.addFirst(o3);
    checkDequeSizeAndContent(deque, o3, o2, o1);

    assertThrows(NullPointerException.class, () -> deque.addFirst(null));
  }

  public void testAddLast() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    deque.addLast(o1);
    checkDequeSizeAndContent(deque, o1);
    deque.addLast(o2);
    checkDequeSizeAndContent(deque, o1, o2);
    deque.addLast(o3);
    checkDequeSizeAndContent(deque, o1, o2, o3);

    assertThrows(NullPointerException.class, () -> deque.addLast(null));
  }

  public void testConstructorFromCollection() {
    assertEquals(0, new ArrayDeque<>(new ArrayList<>()).size());

    Collection<Object> collection = new ArrayList<>(asList(getFullNonNullElements()));
    checkDequeSizeAndContent(new ArrayDeque<>(collection), getFullNonNullElements());
  }

  @J2ktIncompatible // Not nullable according to Jspecify
  public void testConstructorFromCollection_null() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    assertThrows(NullPointerException.class, () -> new ArrayDeque<>(null));

    assertThrows(
        NullPointerException.class,
        () -> new ArrayDeque<>(asList(new Object(), null, new Object())));
  }

  public void testContains() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertFalse(deque.contains(o3));
    assertFalse(deque.contains(null));
    assertTrue(deque.add(o1));
    assertTrue(deque.add(o2));
    assertTrue(deque.add(o1));
    assertTrue(deque.add(o3));
    assertTrue(deque.contains(o1));
    assertTrue(deque.contains(o2));
    assertTrue(deque.contains(o3));
    assertFalse(deque.contains(null));

    deque.clear();
    assertTrue(deque.isEmpty());
    assertFalse(deque.contains(o1));
    assertFalse(deque.contains(o2));
    assertFalse(deque.contains(o3));
  }

  public void testDescendingIterator() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    var it1 = deque.descendingIterator();
    assertFalse(it1.hasNext());
    assertThrows(NoSuchElementException.class, () -> it1.next());

    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    var it2 = deque.descendingIterator();
    assertTrue(it2.hasNext());
    assertEquals(o3, it2.next());
    assertTrue(it2.hasNext());
    assertEquals(o2, it2.next());
    assertTrue(it2.hasNext());
    assertEquals(o1, it2.next());
    assertFalse(it2.hasNext());
    assertThrows(NoSuchElementException.class, () -> it2.next());
    checkDequeSizeAndContent(deque, o1, o2, o3);

    deque = new ArrayDeque<>();
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    var it3 = deque.descendingIterator();
    assertTrue(it3.hasNext());
    assertEquals(o3, it3.next());
    it3.remove();
    assertEquals(2, deque.size());
    assertTrue(it3.hasNext());
    assertEquals(o2, it3.next());
    assertTrue(it3.hasNext());
    assertEquals(o1, it3.next());
    it3.remove();
    checkDequeSizeAndContent(deque, o2);
  }

  public void testElement() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.element());

    deque.add(o1);
    assertEquals(o1, deque.element());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o1, deque.element());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertThrows(NoSuchElementException.class, () -> deque.element());
  }

  public void testFailFastIterator() {
    ArrayDeque<Object> deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    Iterator<Object> it = deque.iterator();
    it.next();
    deque.removeFirst();
    it.next();
    deque.removeLast();
    try {
      it.next();
      if (TestUtils.getJdkVersion() < 11) {
        fail();
      }
    } catch (ConcurrentModificationException expected) {
    }

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    it = deque.iterator();
    it.next();
    deque.clear();
    var i = it;
    assertThrows(ConcurrentModificationException.class, () -> i.next());

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    it = deque.iterator();
    it.next();
    deque.addFirst(new Object());
    it.next();
    deque.addLast(new Object());
    try {
      it.next();
      if (TestUtils.getJdkVersion() < 11) {
        fail();
      }
    } catch (ConcurrentModificationException expected) {
    }

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    it = deque.iterator();
    it.next();
    it.next();
    deque.removeFirst();
    it.remove();

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    it = deque.iterator();
    it.next();
    it.next();
    deque.removeFirst();
    deque.removeFirst();
    try {
      it.remove();
      if (TestUtils.getJdkVersion() < 11) {
        fail();
      }
    } catch (ConcurrentModificationException expected) {
    }
  }

  public void testFailFastDescendingIterator() {
    ArrayDeque<Object> deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    Iterator<Object> it1 = deque.descendingIterator();
    it1.next();
    deque.removeLast();
    it1.next();
    deque.removeFirst();
    if (TestUtils.isJvm()) {
      it1.next();
    } else {
      assertThrows(ConcurrentModificationException.class, () -> it1.next());
    }

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    var it2 = deque.descendingIterator();
    it2.next();
    deque.clear();
    assertThrows(ConcurrentModificationException.class, () -> it2.next());

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    var it3 = deque.descendingIterator();
    it3.next();
    deque.addLast(new Object());
    if (TestUtils.isJvm()) {
      assertThrows(ConcurrentModificationException.class, () -> it3.next());
    } else {
      it3.next();
    }
    deque.addFirst(new Object());
    assertThrows(ConcurrentModificationException.class, () -> it3.next());

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    var it4 = deque.descendingIterator();
    it4.next();
    it4.next();
    deque.removeLast();
    it4.remove();

    deque = new ArrayDeque<>(asList(getFullNonNullElements()));
    var it5 = deque.descendingIterator();
    it5.next();
    it5.next();
    deque.removeLast();
    deque.removeLast();
    if (TestUtils.isJvm()) {
      it5.remove();
    } else {
      assertThrows(ConcurrentModificationException.class, () -> it5.remove());
    }
  }

  public void testGetFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.getFirst());

    deque.add(o1);
    assertEquals(o1, deque.getFirst());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o1, deque.getFirst());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertThrows(NoSuchElementException.class, () -> deque.getFirst());
  }

  public void testGetLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.getLast());

    deque.add(o1);
    assertEquals(o1, deque.getLast());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o2, deque.getLast());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertThrows(NoSuchElementException.class, () -> deque.getLast());
  }

  public void testOffer() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertTrue(deque.offer(o1));
    checkDequeSizeAndContent(deque, o1);
    assertTrue(deque.offer(o2));
    checkDequeSizeAndContent(deque, o1, o2);
    assertTrue(deque.offer(o3));
    checkDequeSizeAndContent(deque, o1, o2, o3);

    assertThrows(NullPointerException.class, () -> deque.offer(null));
  }

  public void testOfferFirst() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertTrue(deque.offerFirst(o1));
    checkDequeSizeAndContent(deque, o1);
    assertTrue(deque.offerFirst(o2));
    checkDequeSizeAndContent(deque, o2, o1);
    assertTrue(deque.offerFirst(o3));
    checkDequeSizeAndContent(deque, o3, o2, o1);

    assertThrows(NullPointerException.class, () -> deque.offerFirst(null));
  }

  public void testOfferLast() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertTrue(deque.offerLast(o1));
    checkDequeSizeAndContent(deque, o1);
    assertTrue(deque.offerLast(o2));
    checkDequeSizeAndContent(deque, o1, o2);
    assertTrue(deque.offerLast(o3));
    checkDequeSizeAndContent(deque, o1, o2, o3);

    assertThrows(NullPointerException.class, () -> deque.offerLast(null));
  }

  public void testPeek() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.peek());

    deque.add(o1);
    assertEquals(o1, deque.peek());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o1, deque.peek());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.addFirst(o3);
    assertEquals(o3, deque.peek());
    checkDequeSizeAndContent(deque, o3, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertNull(deque.peek());
  }

  public void testPeekFirst() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.peekFirst());

    deque.add(o1);
    assertEquals(o1, deque.peekFirst());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o1, deque.peekFirst());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.addFirst(o3);
    assertEquals(o3, deque.peekFirst());
    checkDequeSizeAndContent(deque, o3, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertNull(deque.peekFirst());
  }

  public void testPeekLast() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.peekLast());

    deque.add(o1);
    assertEquals(o1, deque.peekLast());
    checkDequeSizeAndContent(deque, o1);

    deque.add(o2);
    assertEquals(o2, deque.peekLast());
    checkDequeSizeAndContent(deque, o1, o2);

    deque.addFirst(o3);
    assertEquals(o2, deque.peekLast());
    checkDequeSizeAndContent(deque, o3, o1, o2);

    deque.clear();
    assertTrue(deque.isEmpty());
    assertNull(deque.peekLast());
  }

  public void testPoll() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.poll());

    deque.add(o1);
    assertEquals(o1, deque.poll());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o1, deque.poll());
    checkDequeSizeAndContent(deque, o2);
    assertEquals(o2, deque.poll());
    assertTrue(deque.isEmpty());
    assertNull(deque.poll());
  }

  public void testPollFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.pollFirst());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    assertEquals(o1, deque.pollFirst());
    assertTrue(deque.isEmpty());
    assertNull(deque.pollFirst());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o1, deque.pollFirst());
    checkDequeSizeAndContent(deque, o2);
    assertEquals(o2, deque.pollFirst());
    assertTrue(deque.isEmpty());
    assertNull(deque.pollFirst());
  }

  public void testPollLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertNull(deque.pollLast());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    assertEquals(o1, deque.pollLast());
    assertTrue(deque.isEmpty());
    assertNull(deque.pollFirst());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o2, deque.pollLast());
    checkDequeSizeAndContent(deque, o1);
    assertEquals(o1, deque.pollLast());
    assertTrue(deque.isEmpty());
    assertNull(deque.pollLast());
  }

  public void testPop() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.pop());

    deque.add(o1);
    assertEquals(o1, deque.pop());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o1, deque.pop());
    checkDequeSizeAndContent(deque, o2);
    assertEquals(o2, deque.pop());
    assertTrue(deque.isEmpty());
    assertThrows(NoSuchElementException.class, () -> deque.pop());
  }

  public void testPush() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    deque.push(o1);
    checkDequeSizeAndContent(deque, o1);
    deque.push(o2);
    checkDequeSizeAndContent(deque, o2, o1);
    deque.push(o3);
    checkDequeSizeAndContent(deque, o3, o2, o1);

    assertThrows(NullPointerException.class, () -> deque.push(null));
  }

  public void testRemove() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.remove());

    deque.add(o1);
    assertEquals(o1, deque.remove());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o1, deque.remove());
    checkDequeSizeAndContent(deque, o2);
    assertEquals(o2, deque.removeFirst());
    assertTrue(deque.isEmpty());

    assertThrows(NoSuchElementException.class, () -> deque.remove());
  }

  public void testRemoveElement() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertFalse(deque.remove(o1));

    deque.add(o1);
    assertTrue(deque.remove(o1));
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertTrue(deque.remove(o1));
    checkDequeSizeAndContent(deque, o2);
    assertTrue(deque.remove(o2));
    assertTrue(deque.isEmpty());

    assertFalse(deque.remove(null));
  }

  public void testRemoveFirst() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.removeFirst());

    deque.add(o1);
    assertEquals(o1, deque.removeFirst());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o1, deque.removeFirst());
    checkDequeSizeAndContent(deque, o2);
    assertEquals(o2, deque.removeFirst());
    assertTrue(deque.isEmpty());

    assertThrows(NoSuchElementException.class, () -> deque.removeFirst());
  }

  public void testRemoveFirstOccurrence() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertFalse(deque.removeFirstOccurrence(o1));

    deque.add(o1);
    assertTrue(deque.removeFirstOccurrence(o1));
    assertTrue(deque.isEmpty());

    deque = new ArrayDeque<>();
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    assertTrue(deque.removeFirstOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o3);

    deque = new ArrayDeque<>();
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    assertTrue(deque.removeFirstOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o3, o1, o2, o3);
    assertTrue(deque.removeFirstOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o3, o1, o3);
    assertTrue(deque.removeFirstOccurrence(o1));
    checkDequeSizeAndContent(deque, o3, o1, o3);
    assertTrue(deque.removeFirstOccurrence(o1));
    checkDequeSizeAndContent(deque, o3, o3);
    assertFalse(deque.removeFirstOccurrence(o1));

    assertFalse(deque.removeFirstOccurrence(null));
  }

  public void testRemoveLast() {
    Object o1 = new Object();
    Object o2 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertThrows(NoSuchElementException.class, () -> deque.removeLast());

    deque.add(o1);
    assertEquals(o1, deque.removeLast());
    assertTrue(deque.isEmpty());

    deque.add(o1);
    deque.add(o2);
    assertEquals(o2, deque.removeLast());
    checkDequeSizeAndContent(deque, o1);
    assertEquals(o1, deque.removeLast());
    assertEquals(0, deque.size());

    assertThrows(NoSuchElementException.class, () -> deque.removeLast());
  }

  public void testRemoveLastOccurrence() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object o3 = new Object();

    ArrayDeque<Object> deque = new ArrayDeque<>();
    assertFalse(deque.removeLastOccurrence(o1));

    deque.add(o1);
    assertTrue(deque.removeLastOccurrence(o1));
    assertTrue(deque.isEmpty());

    deque = new ArrayDeque<>();
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    assertTrue(deque.removeLastOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o3);

    deque = new ArrayDeque<>();
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    deque.add(o1);
    deque.add(o2);
    deque.add(o3);
    assertTrue(deque.removeLastOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o2, o3, o1, o3);
    assertTrue(deque.removeLastOccurrence(o2));
    checkDequeSizeAndContent(deque, o1, o3, o1, o3);
    assertTrue(deque.removeLastOccurrence(o3));
    checkDequeSizeAndContent(deque, o1, o3, o1);
    assertTrue(deque.removeLastOccurrence(o3));
    checkDequeSizeAndContent(deque, o1, o1);
    assertFalse(deque.removeLastOccurrence(o3));

    assertFalse(deque.removeLastOccurrence(null));
  }

  public void testRolloverInvariants() {
    ArrayDeque<Integer> deque = new ArrayDeque<>();

    assertTrue(deque.add(1));
    assertEquals(1, (int) deque.removeFirst());
    for (int i = 0; i < 100; i++) {
      assertTrue(deque.add(i));
    }
    assertNotNull(deque.peek());
    assertFalse(deque.isEmpty());

    Iterator<Integer> it = deque.iterator();
    for (int i = 0; i < 100; i++) {
      assertTrue(it.hasNext());
      assertEquals(i, (int) it.next());
      it.remove();
    }
    assertFalse(it.hasNext());
    assertNull(deque.peek());
    assertTrue(deque.isEmpty());
  }

  /** Null elements are prohibited in ArrayDeque. */
  @Override
  protected @Nullable Object[] getFullElements() {
    return getFullNonNullElements();
  }

  @Override
  protected Collection makeConfirmedCollection() {
    return new ArrayList<>();
  }

  @Override
  protected Collection makeConfirmedFullCollection() {
    return new ArrayList<>(asList(getFullElements()));
  }

  @Override
  protected Collection makeCollection() {
    return new ArrayDeque<>();
  }

  private void checkDequeSizeAndContent(Deque<?> deque, Object... expected) {
    assertEquals(expected.length, deque.size());
    int i = 0;
    for (Object e : deque) {
      assertEquals(expected[i++], e);
    }
    assertEquals(expected.length, i);
  }
}
