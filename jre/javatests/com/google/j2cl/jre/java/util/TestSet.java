// CHECKSTYLE_OFF: Copyrighted to ASF
/*
 * Copyright 1999-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// CHECKSTYLE_ON
package com.google.j2cl.jre.java.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * Tests base {@link Set} methods and contracts.
 *
 * <p>Since {@link Set} doesn't stipulate much new behavior that isn't already found in {@link
 * Collection}, this class basically just adds tests for {@link Set#equals()} and {@link
 * Set#hashCode()} along with an updated {@link #verify()} that ensures elements do not appear more
 * than once in the set.
 *
 * <p>To use, subclass and override the {@link #makeEmptySet()} method. You may have to override
 * other protected methods if your set is not modifiable, or if your set restricts what kinds of
 * elements may be added; see {@link TestCollection} for more details.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@NullMarked
abstract class TestSet extends TestCollection {

  /**
   * Constructor.
   *
   * @param name name for test
   */
  public TestSet(String name) {}

  /**
   * Makes an empty collection by invoking {@link #makeEmptySet()}.
   *
   * @return an empty collection
   */
  @Override
  protected final Collection makeCollection() {
    return makeEmptySet();
  }

  /**
   * Makes a full collection by invoking {@link #makeFullSet()}.
   *
   * @return a full collection
   */
  @Override
  protected final Collection makeFullCollection() {
    return makeFullSet();
  }

  /** Return the {@link TestCollection#collection} fixture, but cast as a Set. */
  protected Set getSet() {
    return (Set) collection;
  }

  /**
   * Returns an empty {@link HashSet} for use in modification testing.
   *
   * @return a confirmed empty collection
   */
  @Override
  protected Collection makeConfirmedCollection() {
    return new HashSet();
  }

  /**
   * Returns a full {@link HashSet} for use in modification testing.
   *
   * @return a confirmed full collection
   */
  @Override
  protected Collection makeConfirmedFullCollection() {
    HashSet set = new HashSet();
    set.addAll(Arrays.asList(getFullElements()));
    return set;
  }

  /** Return the {@link TestCollection#confirmed} fixture, but cast as a Set. */
  protected Set getConfirmedSet() {
    return (Set) confirmed;
  }

  /**
   * Makes an empty set. The returned set should have no elements.
   *
   * @return an empty set
   */
  protected abstract Set makeEmptySet();

  /**
   * Makes a full set by first creating an empty set and then adding all the elements returned by
   * {@link #getFullElements()}.
   *
   * <p>Override if your set does not support the add operation.
   *
   * @return a full set
   */
  protected Set makeFullSet() {
    Set set = makeEmptySet();
    set.addAll(Arrays.asList(getFullElements()));
    return set;
  }

  /** Tests {@link Set#equals(Object)}. */
  public void testSetEquals() {
    resetEmpty();
    assertEquals("Empty sets should be equal", getSet(), getConfirmedSet());
    verify();

    HashSet set2 = new HashSet();
    set2.add("foo");
    assertFalse("Empty set shouldn't equal nonempty set", getSet().equals(set2));

    resetFull();
    assertEquals("Full sets should be equal", getSet(), getConfirmedSet());
    verify();

    set2.clear();
    set2.addAll(Arrays.asList(getOtherElements()));
    assertTrue("Sets with different contents shouldn't be equal", !getSet().equals(set2));
  }

  /** Tests {@link Set#hashCode()}. */
  public void testSetHashCode() {
    resetEmpty();
    assertEquals(
        "Empty sets have equal hashCodes", getSet().hashCode(), getConfirmedSet().hashCode());

    resetFull();
    assertEquals(
        "Equal sets have equal hashCodes", getSet().hashCode(), getConfirmedSet().hashCode());
  }

  /** Provides additional verifications for sets. */
  @Override
  protected void verify() {
    super.verify();
    assertEquals("Sets should be equal", confirmed, collection);
    assertEquals("Sets should have equal hashCodes", confirmed.hashCode(), collection.hashCode());
    HashSet set = new HashSet();
    Iterator iterator = collection.iterator();
    while (iterator.hasNext()) {
      assertTrue("Set.iterator should only return unique elements", set.add(iterator.next()));
    }
  }
}
