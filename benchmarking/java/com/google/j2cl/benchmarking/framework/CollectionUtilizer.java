/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.benchmarking.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Ensures multiple versions of the collections are alive to prevent special case optimizations. */
class CollectionUtilizer {
  static void dependOnAllCollections() {
    utilizeMap(new HashMap<>());
    utilizeMap(new LinkedHashMap<>());
    utilizeList(new ArrayList<>());
    utilizeList(new LinkedList<>());
    utilizeList(
        new ArrayList<String>() {
          int size = 0;

          @Override
          public boolean add(String item) {
            size++;
            return super.add(item);
          }

          @Override
          public String get(int index) {
            return super.get(index);
          }

          @Override
          public int size() {
            return size;
          }
        });
    utilizeList(
        new LinkedList<String>() {
          int size = 0;

          @Override
          public boolean add(String item) {
            size++;
            return super.add(item);
          }

          @Override
          public String get(int index) {
            return super.get(index);
          }

          @Override
          public int size() {
            return size;
          }
        });
  }

  private static void utilizeMap(Map<String, String> map) {
    if (addGet(map) && iterate(map) && remove(map)) {
      return;
    }
    throw new AssertionError();
  }

  private static boolean addGet(Map<String, String> map) {
    map.put("input", "output");
    return "output".equals(map.get("input")) && map.size() == 1;
  }

  private static boolean iterate(Map<String, String> map) {
    String result = "";
    for (Entry<String, String> entry : map.entrySet()) {
      result += entry.getKey() + entry.getValue();
    }
    return result.length() > 0;
  }

  private static boolean remove(Map<String, String> map) {
    return "output".equals(map.remove("input"));
  }

  private static void utilizeList(List<String> list) {
    if (addGet(list) && iterate(list) && remove(list)) {
      return;
    }
    throw new AssertionError();
  }

  private static boolean addGet(List<String> list) {
    list.add("input");
    return "input".equals(list.get(0)) && list.size() == 1;
  }

  private static boolean remove(List<String> list) {
    return "input".equals(list.remove(0));
  }

  private static boolean iterate(List<String> list) {
    String result = "";
    for (String entry : list) {
      result += entry;
    }
    return result.length() > 0;
  }
}
