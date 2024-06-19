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
package com.google.j2cl.jre.java.util;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

/** Tests EnumMap. */
@NullMarked
public class EnumMapTest extends TestCase {

  enum Numbers {
    ZERO,
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    ELEVEN,
    TWELVE,
    THIRTEEN,
    FOURTEEN,
    FIFTEEN,
    SIXTEEN,
    SEVENTEEN,
    EIGHTEEN,
    NINETEEN,
    TWENTY,
    TWENTY_ONE,
    TWENTY_TWO,
    TWENTY_THREE,
    TWENTY_FOUR,
    TWENTY_FIVE,
    TWENTY_SIX,
    TWENTY_SEVEN,
    TWENTY_EIGHT,
    TWENTY_NINE,
    THIRTY,
  }

  private static <E extends Enum<E>> void enumTests(Class<E> e, E[] enums) {
    EnumMap<E, Integer> numbers = new EnumMap<>(e);
    HashMap<E, Integer> numbersMap = new HashMap<>();
    assertEquals(numbersMap, numbers);

    numbers.put(enums[1], 1);
    numbersMap.put(enums[1], 1);
    numbers.put(enums[2], 2);
    numbersMap.put(enums[2], 2);
    assertEquals(numbersMap, numbers);

    numbers.put(enums[23], 23);
    numbersMap.put(enums[23], 23);
    assertEquals(numbersMap, numbers);

    numbers.remove(enums[1]);
    numbersMap.remove(enums[1]);
    assertEquals(numbersMap, numbers);

    // Attempt an add at the beginning
    numbers.put(enums[0], 0);
    numbersMap.put(enums[0], 0);
    assertEquals(numbersMap, numbers);

    // Attempt an add at the end
    numbers.put(enums[enums.length - 1], enums.length - 1);
    numbersMap.put(enums[enums.length - 1], enums.length - 1);
    assertEquals(numbersMap, numbers);

    // Attempt to remove something bogus
    numbers.remove(enums[15]);
    numbersMap.remove(enums[15]);
    assertEquals(numbersMap, numbers);

    // Attempt to add a duplicate value
    numbers.put(enums[23], 23);
    numbersMap.put(enums[23], 23);
    assertEquals(numbersMap, numbers);

    numbers.clear();
    numbersMap.clear();
    for (E val : enums) {
      numbers.put(val, val.ordinal());
      numbersMap.put(val, val.ordinal());
    }
    assertEquals(numbersMap, numbers);

    assertEquals(numbersMap, numbers.clone());
    assertEquals(numbersMap, new EnumMap<E, Integer>(numbersMap));

    // Test entrySet, keySet, and values
    // Make sure that modifications through these views works correctly
    Set<Map.Entry<E, Integer>> numbersEntrySet = numbers.entrySet();
    Set<Map.Entry<E, Integer>> mapEntrySet = numbersMap.entrySet();
    assertEquals(mapEntrySet, numbersEntrySet);

    final Map.Entry<E, Integer> entry = numbers.entrySet().iterator().next();
    /*
     * Copy entry because it is no longer valid after
     * numbersEntrySet.remove(entry).
     */
    Map.Entry<E, Integer> entryCopy =
        new Map.Entry<E, Integer>() {
          final E key = entry.getKey();
          Integer value = entry.getValue();

          @Override
          public E getKey() {
            return key;
          }

          @Override
          public Integer getValue() {
            return value;
          }

          @Override
          public Integer setValue(Integer value) {
            Integer oldValue = this.value;
            this.value = value;
            return oldValue;
          }
        };
    numbersEntrySet.remove(entry);
    mapEntrySet.remove(entryCopy);
    assertEquals(mapEntrySet, numbersEntrySet);
    assertEquals(numbersMap, numbers);

    Set<E> numbersKeySet = numbers.keySet();
    Set<E> mapKeySet = numbersMap.keySet();
    assertEquals(mapKeySet, numbersKeySet);
    numbersKeySet.remove(enums[2]);
    mapKeySet.remove(enums[2]);
    assertEquals(mapKeySet, numbersKeySet);
    assertEquals(numbersMap, numbers);

    Collection<Integer> numbersValues = numbers.values();
    Collection<Integer> mapValues = numbersMap.values();
    assertEquals(sort(mapValues), sort(numbersValues));
    numbersValues.remove(23);
    mapValues.remove(23);
    assertEquals(sort(mapValues), sort(numbersValues));
    assertEquals(numbersMap, numbers);
  }

  private static <T extends Comparable<T>> List<T> sort(Collection<T> col) {
    ArrayList<T> list = new ArrayList<T>(col);
    Collections.sort(list);
    return list;
  }

  public void testBasics() {
    enumTests(Numbers.class, Numbers.values());
  }

  @J2ktIncompatible
  public void testNulls() {
    EnumMap<Numbers, Integer> numbers = new EnumMap<>(Numbers.class);

    assertFalse("Should not contain null value", numbers.containsValue(null));
    assertFalse("Should not contain null key", numbers.containsKey(null));

    numbers.put(Numbers.TWO, null);
    assertTrue("Should contain a null value", numbers.containsValue(null));

    try {
      numbers.put(null, 3);
      fail("Should not be able to insert a null key.");
    } catch (NullPointerException expected) {
    }
  }

  public void testOrdering() {
    EnumMap<Numbers, Integer> numbers = new EnumMap<>(Numbers.class);
    Numbers[] enums = Numbers.values();

    for (int i = enums.length - 1; i >= 0; --i) {
      numbers.put(enums[i], i);
    }

    int lastOrdinal = -1;
    for (Map.Entry<Numbers, Integer> val : numbers.entrySet()) {
      int newOrdinal = val.getKey().ordinal();
      assertTrue("EnumMap must maintain Enums in order", lastOrdinal < newOrdinal);
      lastOrdinal = newOrdinal;
    }
  }

  public void testConstructorSucceedsGivenEmptyEnumMap() {
    var unused = new EnumMap<>(new EnumMap<Numbers, Integer>(Numbers.class));
  }

  public void testConstructorThrowsGivenEmptyOtherMap() {
    try {
      new EnumMap<>(new HashMap<Numbers, Integer>());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
