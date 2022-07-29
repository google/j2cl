/*
 * Copyright 2022 Google Inc.
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
package j2ktjre;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jsinterop.annotations.JsNonNull;

public class Collections {

  public static void testCollections() {
    testJavaMapSignatures();
    testAbstractMapSubclass_bridgedOverridesAreCalled();
  }

  private static void testJavaMapSignatures() {
    // Map and HashMap are mapped to separate native types, we test bridged methods with each type
    // as the target because they are dispatched statically.
    HashMap<String, Integer> hashMap = new HashMap<>();
    Map<String, Integer> map = hashMap;
    map.put("a", 1);
    hashMap.put("b", 2);

    assertEquals((Object) 1, map.get("a"));
    assertEquals((Object) 1, hashMap.get("a"));
    assertEquals((Object) 2, map.get((Object) "b"));
    assertEquals((Object) 2, hashMap.get((Object) "b"));
    assertEquals(null, map.get("c"));
    assertEquals(null, hashMap.get("c"));
    assertEquals(null, map.get(new Object()));
    assertEquals(null, hashMap.get(new Object()));

    assertTrue(map.containsKey("b"));
    assertTrue(hashMap.containsKey("b"));
    assertTrue(map.containsKey((Object) "a"));
    assertTrue(hashMap.containsKey((Object) "a"));
    assertFalse(map.containsKey("c"));
    assertFalse(hashMap.containsKey("c"));
    assertFalse(map.containsKey(new Object()));
    assertFalse(hashMap.containsKey(new Object()));

    assertTrue(map.containsValue(1));
    assertTrue(hashMap.containsValue(1));
    assertTrue(map.containsValue((Object) 2));
    assertTrue(hashMap.containsValue((Object) 2));
    assertFalse(map.containsValue(3));
    assertFalse(hashMap.containsValue(3));
    assertFalse(map.containsValue(new Object()));
    assertFalse(hashMap.containsValue(new Object()));

    assertEquals(null, map.remove(new Object()));
    assertEquals(null, hashMap.remove(new Object()));

    assertEquals((Object) 1, map.remove("a"));
    assertEquals(1, map.size());
    map.put("a", 1);
    assertEquals((Object) 1, hashMap.remove("a"));
    assertEquals(1, map.size());

    assertEquals((Object) 2, map.remove((Object) "b"));
    assertEquals(0, map.size());
    map.put("b", 2);
    assertEquals((Object) 2, hashMap.remove((Object) "b"));
    assertEquals(0, hashMap.size());
  }

  private static class TestMap<K, V> extends AbstractMap<K, V> {

    private final Set<@JsNonNull Entry<K, V>> entrySet = new HashSet<>();

    public int getCalls = 0;
    public int containsKeyCalls = 0;
    public int containsValueCalls = 0;
    public int removeCalls = 0;

    @Override
    public Set<@JsNonNull Entry<K, V>> entrySet() {
      return entrySet;
    }

    @Override
    public V get(Object key) {
      getCalls++;
      return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
      containsKeyCalls++;
      return super.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      containsValueCalls++;
      return super.containsValue(value);
    }

    @Override
    public V remove(Object key) {
      removeCalls++;
      return super.remove(key);
    }
  }

  private static void testAbstractMapSubclass_bridgedOverridesAreCalled() {
    TestMap<Integer, String> testMap = new TestMap<>();
    Map<Integer, String> map = testMap; // This will be typed as plain MutableMap in Kotlin output

    assertEquals(null, map.get(1));
    assertEquals(1, testMap.getCalls);

    assertEquals(null, map.get((Object) 2));
    assertEquals(2, testMap.getCalls);

    assertEquals(false, map.containsKey(3));
    assertEquals(1, testMap.containsKeyCalls);

    assertEquals(false, map.containsKey((Object) 4));
    assertEquals(2, testMap.containsKeyCalls);

    assertEquals(false, map.containsValue(5));
    assertEquals(1, testMap.containsValueCalls);

    assertEquals(false, map.containsValue((Object) 6));
    assertEquals(2, testMap.containsValueCalls);

    assertEquals(null, map.remove(7));
    assertEquals(1, testMap.removeCalls);

    assertEquals(null, map.remove((Object) 8));
    assertEquals(2, testMap.removeCalls);
  }
}
