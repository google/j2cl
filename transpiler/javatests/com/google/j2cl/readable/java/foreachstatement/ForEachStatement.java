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
import org.jspecify.annotations.Nullable;

public class ForEachStatement {
  public void test(Iterable<Throwable> iterable) {
    for (Throwable t : iterable) {
      t.toString();
    }

    for (Throwable t : new Throwable[10]) {
      t.toString();
    }
  }

  static class Exception1 extends Exception implements Iterable<Number> {
    public Iterator<Number> iterator() {
      return null;
    }
  }

  static class Exception2 extends Exception implements Iterable<@Nullable Integer> {
    public Iterator<@Nullable Integer> iterator() {
      return null;
    }
  }

  static class Exception3 extends Exception implements Iterable<@JsNonNull Integer> {
    public Iterator<@JsNonNull Integer> iterator() {
      return null;
    }
  }

  static class Exception4 extends Exception implements Iterable {
    public Iterator iterator() {
      return null;
    }
  }

  private void testMulticatch() throws Exception {
    try {
      throw new Exception();
    } catch (Exception1 | Exception2 e) {
      // No common element type.
      for (Number o : e) {}
    } catch (Exception3 | Exception4 e) {
      // raw types
      for (Object o : e) {}
    }
    try {
      throw new Exception();
    } catch (Exception2 | Exception3 e) {
      // unboxing and widening
      for (long o : e) {}
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

    Iterable<Character> charIterable = null;
    for (int c : charIterable) {}
  }

  private void testSideEffects() {
    Iterable<Integer> iterable = null;
    int[] primitiveArray = null;
    for (Integer i : iterable) {
      // Modify the iteration variable general case.
      i = 4;
    }

    for (int i : iterable) {
      // Modify the iteration variable when there is a conversion.
      i += 4;
    }

    for (int i : primitiveArray) {
      // Modify the iteration variable primitive value;
      i += 4;
    }

    for (int i : primitiveArray) {
      // Modify the iteration variable unary operation;
      i++;
    }
  }
}
