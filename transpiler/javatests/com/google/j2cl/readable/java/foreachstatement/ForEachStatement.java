/*
 * Copyright 2017 Google Inc.
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
package foreachstatement;

import java.util.Iterator;
import jsinterop.annotations.JsNonNull;

public class ForEachStatement {
  public void test(Iterable<Throwable> iterable) {
    for (Throwable t : iterable) {
      t.toString();
    }

    for (Throwable t : new Throwable[10]) {
      t.toString();
    }
  }

  static class Exception1 extends Exception implements Iterable<String> {
    public Iterator<String> iterator() {
      return null;
    }
  }

  static class Exception2 extends Exception implements Iterable<Object> {
    public Iterator<Object> iterator() {
      return null;
    }
  }

  private void testMulticatch() throws Exception {
    try {
      throw new Exception();
    } catch (Exception1 | Exception2 e) {
      for (Object o : e) {}
    }
  }

  static class IterableReturningTypeVariable<U, T extends @JsNonNull Iterator<Integer>>
      implements Iterable<Integer> {
    public T iterator() {
      return null;
    }
  }

  private <T extends Object & Iterable<String>, U extends T, V extends Object & Iterable<Integer>>
      void testTypeVariable() {
    U iterable = null;
    for (String s : iterable) {}

    IterableReturningTypeVariable<?, ?> anotherIterable = null;
    for (int s : anotherIterable) {}

    // This is an auto-unboxing via an intersection iterable type.
    V integerIterable = null;
    for (int i : integerIterable) {}

    // This is an auto-unboxing and widening via an intersection iterable type.
    for (long i : integerIterable) {}
  }
}
