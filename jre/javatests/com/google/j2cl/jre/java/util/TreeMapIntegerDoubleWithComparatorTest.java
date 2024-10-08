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
package com.google.j2cl.jre.java.util;

import java.util.Comparator;
import java.util.NavigableMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Tests <code>TreeMap</code> with a <code>Comparator</code>. */
@NullMarked
public class TreeMapIntegerDoubleWithComparatorTest extends TreeMapIntegerDoubleTest {
  @Override
  protected NavigableMap<Integer, Double> createNavigableMap() {
    setComparator(
        new Comparator<@Nullable Integer>() {
          @Override
          public int compare(@Nullable Integer o1, @Nullable Integer o2) {
            if (o1 == null) {
              return o2 == null ? 0 : -1;
            }
            if (o2 == null) {
              return 1;
            }
            return o1.compareTo(o2);
          }
        });
    return super.createNavigableMap();
  }

  @Override
  public boolean useNullKey() {
    return true;
  }
}
