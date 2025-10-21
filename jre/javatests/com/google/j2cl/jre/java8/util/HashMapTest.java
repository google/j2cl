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
package com.google.j2cl.jre.java8.util;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Tests for java.util.HashMap Java 8 API emulation. */
public class HashMapTest extends AbstractJava8MapTest {

  @Override
  protected Map<String, String> createMap() {
    return new HashMap<>();
  }

  public void testComputeIfAbsent_nullKey() {
    Map<@Nullable String, String> map = createMap();

    String value = map.computeIfAbsent(null, k -> "A");
    assertEquals("A", value);
    assertTrue(map.containsKey(null));
    assertEquals("A", map.get(null));
  }
}
