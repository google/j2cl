/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.generator.visitors;

import com.google.common.collect.Lists;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility functions for Import.
 */
public class ImportUtils {
  @SuppressWarnings("unchecked")
  public static List<Import> getSortedImports(Map<ImportCategory, Set<Import>> importsByCategory) {
    return sortedList(
        union(
            importsByCategory.get(ImportCategory.EAGER),
            importsByCategory.get(ImportCategory.LAZY)));
  }

  public static <T extends Comparable<T>> List<T> sortedList(Set<T> set) {
    List<T> sortedList = Lists.newArrayList(set);
    Collections.sort(sortedList);
    return sortedList;
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> union(Set<T>... elements) {
    HashSet<T> union = new HashSet<>();
    for (Set<T> element : elements) {
      union.addAll(element);
    }
    return union;
  }
}
