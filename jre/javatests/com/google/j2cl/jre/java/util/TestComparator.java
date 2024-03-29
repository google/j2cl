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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
abstract class TestComparator extends TestObject {

  public abstract Comparator makeComparator();

  public abstract List getComparableObjectsOrdered();

  @Override
  public Object makeObject() {
    return makeComparator();
  }

  public void reverseObjects(List list) {
    Collections.reverse(list);
  }

  /**
   * Sort object according to the given Comparator.
   *
   * @param list List to sort
   * @param comparator sorting comparator
   */
  public void sortObjects(List list, Comparator comparator) {

    Collections.sort(list, comparator);
  }

  @Override
  public boolean supportsEmptyCollections() {
    return false;
  }

  @Override
  public boolean supportsFullCollections() {
    return false;
  }

  public void testEmptyListSort() {
    List list = new ArrayList();
    sortObjects(list, makeComparator());

    List list2 = new ArrayList();

    assertTrue("Comparator cannot sort empty lists", list2.equals(list));
  }

  public void testReverseListSort() {
    Comparator comparator = makeComparator();

    List randomList = getComparableObjectsOrdered();
    reverseObjects(randomList);
    sortObjects(randomList, comparator);

    List orderedList = getComparableObjectsOrdered();

    assertTrue("Comparator did not reorder the List correctly", orderedList.equals(randomList));
  }

  public String getCanonicalComparatorName(Object object) {
    StringBuilder retval = new StringBuilder();
    retval.append("data/test/");
    String colName = object.getClass().getName();
    colName = colName.substring(colName.lastIndexOf(".") + 1);
    retval.append(colName);
    retval.append(".obj");
    return retval.toString();
  }

  /** Compare the current serialized form of the Comparator against the canonical version in CVS. */
  public void testComparatorCompatibility() {
    Comparator comparator = null;

    // make sure the canonical form produces the ordering we currently
    // expect
    List randomList = getComparableObjectsOrdered();
    reverseObjects(randomList);
    sortObjects(randomList, comparator);

    List orderedList = getComparableObjectsOrdered();

    assertTrue(
        "Comparator did not reorder the List correctly:" + randomList,
        orderedList.equals(randomList));
  }
}
