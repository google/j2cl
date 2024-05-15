/*
 * Copyright 2024 Google Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import jsinterop.annotations.JsType;
import junit.framework.TestCase;

/** Testing JS contract of Collection interface. */
public final class JsCollectionTest extends TestCase {

  public void testToArray() {
    ArrayList<String> list = new ArrayList<>();
    list.add("x");
    list.add("y");
    list.add("z");
    String[] array = ((JsStringCollection) list).toArray();
    assertEquals("x", array[0]);
    assertEquals("y", array[1]);
    assertEquals("z", array[2]);
    assertJsStringArray(array);

    // Ensure the default bridge method doesn't add checks.
    MyCollection collection = new MyCollection();
    String[] array2 = ((JsStringCollection) collection).toArray();
    assertEquals(0, array2.length);
    assertJsStringArray(array2);
  }

  // Should be a separate method with String[] argument to ensure the type is checked by JsCompiler.
  private static void assertJsStringArray(String[] array) {
    assertSame(Object[].class, array.getClass());
  }

  @JsType(isNative = true, namespace = "java.util", name = "Collection")
  private interface JsStringCollection {
    String[] toArray();
  }

  // Note that implements Collection<String> to force 'String[] toArray()' bridge method.
  static class MyCollection implements Collection<String> {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean add(String e) {
      return false;
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
      return false;
    }

    @Override
    public void clear() {}

    @Override
    public boolean contains(Object o) {
      return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Iterator<String> iterator() {
      return null;
    }

    @Override
    public boolean remove(Object o) {
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return false;
    }

    @Override
    public Object[] toArray() {
      return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return null;
    }
  }
}
