/*
 * Copyright 2014 Google Inc.
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
package com.google.j2cl.jre;

import com.google.j2cl.jre.java.util.ArrayDequeTest;
import com.google.j2cl.jre.java.util.ArrayListTest;
import com.google.j2cl.jre.java.util.ArraysDoubleSemanticsTest;
import com.google.j2cl.jre.java.util.ArraysFloatSemanticsTest;
import com.google.j2cl.jre.java.util.ArraysTest;
import com.google.j2cl.jre.java.util.BitSetTest;
import com.google.j2cl.jre.java.util.CollectionsTest;
import com.google.j2cl.jre.java.util.EnumMapTest;
import com.google.j2cl.jre.java.util.EnumSetTest;
import com.google.j2cl.jre.java.util.HashMapSmokeTest;
import com.google.j2cl.jre.java.util.HashMapTest;
import com.google.j2cl.jre.java.util.HashSetTest;
import com.google.j2cl.jre.java.util.IdentityHashMapTest;
import com.google.j2cl.jre.java.util.LinkedHashMapTest;
import com.google.j2cl.jre.java.util.LinkedHashSetTest;
import com.google.j2cl.jre.java.util.LinkedListTest;
import com.google.j2cl.jre.java.util.PriorityQueueTest;
import com.google.j2cl.jre.java.util.StackTest;
import com.google.j2cl.jre.java.util.TreeMapIntegerDoubleTest;
import com.google.j2cl.jre.java.util.TreeMapIntegerDoubleWithComparatorTest;
import com.google.j2cl.jre.java.util.TreeMapStringStringTest;
import com.google.j2cl.jre.java.util.TreeMapStringStringWithComparatorTest;
import com.google.j2cl.jre.java.util.TreeSetIntegerTest;
import com.google.j2cl.jre.java.util.TreeSetIntegerWithComparatorTest;
import com.google.j2cl.jre.java.util.VectorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE Collections emulation. */
@RunWith(Suite.class)
@SuiteClasses({
  ArrayDequeTest.class,
  ArrayListTest.class,
  ArraysDoubleSemanticsTest.class,
  ArraysFloatSemanticsTest.class,
  ArraysTest.class,
  BitSetTest.class,
  CollectionsTest.class,
  EnumMapTest.class,
  EnumSetTest.class,
  HashMapTest.class,
  HashSetTest.class,
  IdentityHashMapTest.class,
  LinkedHashMapTest.class,
  LinkedHashSetTest.class,
  LinkedListTest.class,
  PriorityQueueTest.class,
  StackTest.class,
  VectorTest.class,
  TreeMapStringStringTest.class,
  TreeMapStringStringWithComparatorTest.class,
  TreeMapIntegerDoubleTest.class,
  TreeMapIntegerDoubleWithComparatorTest.class,
  TreeSetIntegerTest.class,
  TreeSetIntegerWithComparatorTest.class,

  // Put last to reduce number of times the test framework switches modules
  HashMapSmokeTest.class,
})
public class CollectionsSuite {}
