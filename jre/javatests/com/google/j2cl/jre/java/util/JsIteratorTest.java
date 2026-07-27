/*
 * Copyright 2026 Google Inc.
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

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import javaemul.internal.JsIterableHelper.JsIterable;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import junit.framework.TestCase;

/** Testing JS contract of Iterator interface. */
public class JsIteratorTest extends TestCase {

  public void testEmpty() {
    assertArrayEquals(new String[0], arrayFrom(Collections.emptyIterator()));
    assertArrayEquals(new String[0], arrayFrom(Collections.emptyList()));
  }

  public void testIterator() {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    set.add("x");
    set.add("y");
    set.add("z");
    assertArrayEquals(new String[] {"x", "y", "z"}, arrayFrom(set));
    assertArrayEquals(new String[] {"x", "y", "z"}, arrayFrom(set.iterator()));

    ArrayList<String> list = new ArrayList<>();
    list.add("x");
    list.add("y");
    list.add("z");
    assertArrayEquals(new String[] {"x", "y", "z"}, arrayFrom(list));
    assertArrayEquals(new String[] {"x", "y", "z"}, arrayFrom(list.iterator()));
  }

  private static class IterableIterator implements Iterable<String>, Iterator<String> {
    private final ArrayList<String> list;

    public IterableIterator() {
      list = new ArrayList<>();
      list.add("x");
      list.add("y");
      list.add("z");
    }

    @Override
    public Iterator<String> iterator() {
      return list.iterator();
    }

    @Override
    public boolean hasNext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String next() {
      throw new UnsupportedOperationException();
    }
  }

  public void testIterableIterator() {
    IterableIterator iterableIterator = new IterableIterator();
    // java.lang.Iterable behavior has precedence over java.util.Iterator.
    assertArrayEquals(new String[] {"x", "y", "z"}, arrayFrom((Iterator<String>) iterableIterator));
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Array.from")
  private static native String[] arrayFrom(JsIterable<String> jsIterable);
}
