/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.jre.java.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** J2CL specific EmumSet test as it doesn't support getEnumConstants (b/30745420). */
public class EnumSetTest extends TestSet {
  public EnumSetTest() {
    super("EnumSetTest");
  }

  enum FalseEnum {
    Zero,
    One,
  }

  enum Numbers {
    Zero,
    One,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Ten,
    Eleven,
    Twelve,
    Thirteen,
    Fourteen,
    Fifteen,
    Sixteen,
    Seventeen,
    Eighteen,
    Nineteen,
    Twenty,
    TwentyOne,
    TwentyTwo,
    TwentyThree,
    TwentyFour,
    TwentyFive,
    TwentySix,
    TwentySeven,
    TwentyEight,
    TwentyNine,
    Thirty,
    ThirtyOne,
    ThirtyTwo,
    ThirtyThree,
    Thirtyfour,
  }

  enum ClinitRace {
    Zero,
    One,
    Two,
    Three;

    // TODO(b/30745420): Not compatible yet because of EnumSet#allOf.
    // public static final Set<ClinitRace> set = EnumSet.allOf(ClinitRace.class);
  }

  // ***********************************************************************************************
  // TestSet overrides:
  // ***********************************************************************************************

  @Override
  protected Set makeEmptySet() {
    return EnumSet.noneOf(Numbers.class);
  }

  @Override
  protected Object[] getOtherElements() {
    return new Numbers[] {Numbers.Fifteen, Numbers.Sixteen, Numbers.Ten};
  }

  @Override
  // Note that this method is badly named. It doesn't mean *all* elements.
  protected Object[] getFullElements() {
    return new Numbers[] {Numbers.One, Numbers.Two, Numbers.Three, Numbers.Four};
  }

  @Override
  protected void verify() {
    super.verify();
    assertIterationOrder(collection);
  }

  private static void assertIterationOrder(Collection<Numbers> nums) {
    int lastOrdinal = -1;
    for (Numbers n : nums) {
      assertTrue(n.ordinal() > lastOrdinal);
      lastOrdinal = n.ordinal();
    }
  }

  // ***********************************************************************************************
  // Tests.
  // ***********************************************************************************************

  // No matter what order your insertion, always iterate in ascending order.
  public void testIterator() {
    // Test iteration order when the values added with EnumSet#of
    List<Numbers> expectedOrder =
        Arrays.asList(Numbers.Eight, Numbers.Nine, Numbers.Ten, Numbers.Twenty);
    assertIteration(
        expectedOrder, EnumSet.of(Numbers.Eight, Numbers.Nine, Numbers.Ten, Numbers.Twenty));
    assertIteration(
        expectedOrder, EnumSet.of(Numbers.Nine, Numbers.Eight, Numbers.Twenty, Numbers.Ten));
    assertIteration(
        expectedOrder, EnumSet.of(Numbers.Twenty, Numbers.Eight, Numbers.Nine, Numbers.Ten));

    // Test iteration order when the values added with EnumSet#add
    EnumSet<Numbers> testSet = EnumSet.noneOf(Numbers.class);
    testSet.add(Numbers.Twenty);
    testSet.add(Numbers.Five);
    testSet.add(Numbers.Fifteen);
    testSet.add(Numbers.Twenty);
    testSet.add(Numbers.One);
    testSet.add(Numbers.Three);
    testSet.add(Numbers.Fifteen);
    List<Numbers> expectedOrder2 =
        Arrays.asList(Numbers.One, Numbers.Three, Numbers.Five, Numbers.Fifteen, Numbers.Twenty);
    assertIteration(expectedOrder2, testSet);
  }

  private static <E> void assertIteration(Collection<E> expected, Collection<E> actual) {
    Iterator<E> actualIter = actual.iterator();
    expected.forEach(e -> assertEquals(e, actualIter.next()));
    assertFalse(actualIter.hasNext());
  }

  public void testClone() {
    EnumSet<Numbers> nums = EnumSet.of(Numbers.One, Numbers.Zero);
    assertNotSame(nums, nums.clone());
  }

  // According to EnumSet Javadoc:
  // The returned iterator is weakly consistent: it will never throw ConcurrentModificationException
  // and it may or may not show the effects of any modifications to the set that occur while the
  // iteration is in progress.
  // In the J2CL implementation, we do not reflect modification to the iteration.
  public void testIterator_concurrentModification() {
    EnumSet<Numbers> partial = EnumSet.of(Numbers.One, Numbers.Two, Numbers.Three, Numbers.Five);
    Iterator<Numbers> expecteds = EnumSet.copyOf(partial).iterator();
    for (Numbers n : partial) {
      if (n == Numbers.One) {
        partial.add(Numbers.Four);
      }
      // Expect same items as we do not reflect modifications to iterator.
      assertEquals(expecteds.next(), n);
    }
    assertFalse(expecteds.hasNext());
    assertTrue(partial.contains(Numbers.Four));
  }

  // ***********************************************************************************************
  // Tests pulled from GWT EnumSetTest below. Not good coverage. Some are disabled due to b/30745420
  // ***********************************************************************************************

  /** Tests that an EnumSet can be statically initialized in an enum. */
  // TODO(b/30745420): Not compatible yet because of EnumSet#allOf
  //  public void testClinitRace() {
  //    assertEquals(4, ClinitRace.set.size());
  //    assertTrue(ClinitRace.set.contains(ClinitRace.Zero));
  //    assertTrue(ClinitRace.set.contains(ClinitRace.One));
  //    assertTrue(ClinitRace.set.contains(ClinitRace.Two));
  //    assertTrue(ClinitRace.set.contains(ClinitRace.Three));
  //  }

  /** Test failure mode from issue 3605. Previously resulted in an incorrect size. */
  public void testDuplicates() {
    EnumSet<Numbers> set = EnumSet.of(Numbers.Two, Numbers.One, Numbers.Two, Numbers.One);
    assertEquals(2, set.size());
    assertTrue(set.contains(Numbers.One));
    assertTrue(set.contains(Numbers.Two));
  }

  /** Test failure mode from issue 3605. Previously resulted in a NoSuchElementException. */
  public void testDuplicatesToArray() {
    EnumSet<Numbers> set = EnumSet.of(Numbers.Two, Numbers.One, Numbers.Two, Numbers.One);
    Numbers[] array = set.toArray(new Numbers[set.size()]);
    assertNotNull(array);
    assertEquals(2, array.length);
    if (array[0] != Numbers.One && array[1] != Numbers.One) {
      fail("Numbers.One not found");
    }
    if (array[0] != Numbers.Two && array[1] != Numbers.Two) {
      fail("Numbers.Two not found");
    }
  }

  // TODO(b/30745420): Copy the test after bug is fixed.
  // public void testNumbers() {}
}
