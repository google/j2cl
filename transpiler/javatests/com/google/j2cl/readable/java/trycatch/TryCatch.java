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
package trycatch;

public class TryCatch {

  public void testMultiCatch() {
    try {
      throw new ClassCastException();
    } catch (NullPointerException | ClassCastException e) {
      throw e;
    } catch (RuntimeException r) {
      r = null; // used to show exception variable is transpiled correctly.
    }
  }

  public void testEmptyThrowableCatch() throws Throwable {
    try {
      throw new ClassCastException();
    } catch (Throwable e) {
      // expected empty body.
    }

    try {
      throw new ClassCastException();
    } catch (Exception e) {
      // expected empty body.
    } catch (Throwable e) {
      // expected empty body.
    }
  }

  public void testEmptyThrowableRethrow() throws Throwable {
    try {
      throw new ClassCastException();
    } catch (Throwable e) {
      throw e;
    }
  }

  public void testFinally() {
    try {
      assert true;
    } finally {
    }
  }

  static class ClosableThing implements AutoCloseable {
    @Override
    public void close() {}
  }

  public void testTryWithResource() {
    try (ClosableThing thing = new ClosableThing();
        ClosableThing thing2 = new ClosableThing()) {
      throw new Exception();
    } catch (Exception e) {
      // expected empty body.
    }
  }

  public void testTryWithResourceJava9() {
    ClosableThing thing = new ClosableThing();
    ClosableThing thing2 = new ClosableThing();
    try (thing; thing2) {
      throw new Exception();
    } catch (Exception e) {
      // expected empty body.
    }
  }

  private static final ClosableThing closableThing = new ClosableThing();

  public void testTryWithResouceOnStaticField() {
    try (closableThing) {
      throw new Exception();
    } catch (Exception e) {
      // expected empty body
    }
  }

  public void testNestedTryCatch() {
    try {
      throw new Exception();
    } catch (Exception ae) {
      try {
        throw new Exception();
      } catch (Exception ie) {
        // expected empty body.
      }
    }
  }

  public void testThrowGenerics() throws Throwable {
    throw getT(new Exception());
  }

  private <T> T getT(T t) {
    return t;
  }

  public void testThrowBoundGenerics() throws Throwable {
    throw getThrowable();
  }

  private <T extends Throwable> T getThrowable() {
    return null;
  }
}

