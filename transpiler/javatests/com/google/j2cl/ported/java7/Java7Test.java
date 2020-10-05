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
package com.google.j2cl.ported.java7;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests Java 7 features. */
@RunWith(JUnit4.class)
public class Java7Test {
  // new style class literals
  int million = 1_000_000;
  int five = 0b101;

  @Test
  public void testNewStyleLiterals() {
    assertThat(million).isEqualTo(1000000);
    assertThat(five).isEqualTo(5);
  }

  @Test
  public void testSwitchOnString() {

    String s = "AA";
    int result = -1;
    switch (s) {
      case "BB":
        result = 0;
        break;
      case "CC":
      case "AA":
        result = 1;
        break;
      default:
        break;
    }
    assertThat(result).isEqualTo(1);
  }

  final List<String> log = new ArrayList<String>();

  enum ThrowsWhen {
    NEVER,
    ONCONSTRUCTOR,
    ONCLOSE;
  }

  class Resource implements AutoCloseable {

    final String name;
    final ThrowsWhen throwsWhen;

    public Resource(String name) throws E1 {
      this(name, ThrowsWhen.NEVER);
    }

    public Resource(String name, ThrowsWhen throwsWhen) throws E1 {
      this.name = name;
      this.throwsWhen = throwsWhen;
      log.add("Open " + name);
      if (throwsWhen == ThrowsWhen.ONCONSTRUCTOR) {
        throwException("ExceptionOnConstructor");
      }
    }

    public void doSomething() {
      log.add("doSomething " + name);
    }

    public void throwException(String text) throws E1 {
      throw new E1(text + " in " + name);
    }

    @Override
    public void close() throws Exception {
      log.add("Close " + name);
      if (throwsWhen == ThrowsWhen.ONCLOSE) {
        throwException("ExceptionOnClose");
      }
    }
  }

  private void logException(Exception e) {
    log.add(e.getMessage());
    for (Throwable t : e.getSuppressed()) {
      log.add("Suppressed: " + t.getMessage());
    }
  }

  @Test
  public void testTryWithResources_noExceptions() throws Exception {
    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new Resource("B");
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    }

    assertContentsInOrder(
        log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close C",
        "Close B",
        "Close A");
  }

  @Test
  public void testTryWithResources_exceptions() throws Exception {
    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new Resource("B", ThrowsWhen.ONCLOSE);
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    } catch (Exception e) {
      log.add(e.getMessage());
    } finally {
      log.add("finally");
    }

    assertContentsInOrder(
        log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close C",
        "Close B",
        "Close A",
        "ExceptionOnClose in B",
        "finally");
  }

  @Test
  public void testTryWithResources_suppressedExceptions() throws Exception {
    log.clear();
    try (Resource rA = new Resource("A", ThrowsWhen.ONCLOSE);
        Resource rB = new Resource("B");
        Resource rC = new Resource("C", ThrowsWhen.ONCLOSE)) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    } catch (Exception e) {
      logException(e);
    }

    assertContentsInOrder(
        log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "doSomething B",
        "doSomething C",
        "Close C",
        "Close B",
        "Close A",
        "ExceptionOnClose in C",
        "Suppressed: ExceptionOnClose in A");

    log.clear();
    try (Resource rA = new Resource("A");
        Resource rB = new Resource("B", ThrowsWhen.ONCLOSE);
        Resource rC = new Resource("C")) {

      rA.doSomething();
      rB.throwException("E1 here");
      rC.doSomething();
    } catch (Exception e) {
      logException(e);
    } finally {
      log.add("finally");
    }

    assertContentsInOrder(
        log,
        "Open A",
        "Open B",
        "Open C",
        "doSomething A",
        "Close C",
        "Close B",
        "Close A",
        "E1 here in B",
        "Suppressed: ExceptionOnClose in B",
        "finally");
  }

  @Test
  public void testTryWithResources_exceptionInAcquisition() {
    log.clear();
    try (Resource rA = new Resource("A", ThrowsWhen.ONCLOSE);
        Resource rB = new Resource("B", ThrowsWhen.ONCONSTRUCTOR);
        Resource rC = new Resource("C", ThrowsWhen.ONCLOSE)) {

      rA.doSomething();
      rB.doSomething();
      rC.doSomething();
    } catch (Exception e) {
      logException(e);
    }

    assertContentsInOrder(
        log,
        "Open A",
        "Open B",
        "Close A",
        "ExceptionOnConstructor in B",
        "Suppressed: ExceptionOnClose in A");
  }

  @Test
  public void testAddSuppressedExceptions() {
    Throwable throwable = new Throwable("primary");
    assertThat(throwable.getSuppressed()).isNotNull();
    assertThat(throwable.getSuppressed().length).isEqualTo(0);
    Throwable suppressed1 = new Throwable("suppressed1");
    throwable.addSuppressed(suppressed1);
    assertThat(throwable.getSuppressed().length).isEqualTo(1);
    assertThat((Object) throwable.getSuppressed()[0]).isEqualTo(suppressed1);
    Throwable suppressed2 = new Throwable("suppressed2");
    throwable.addSuppressed(suppressed2);
    assertThat(throwable.getSuppressed().length).isEqualTo(2);
    assertThat((Object) throwable.getSuppressed()[0]).isEqualTo(suppressed1);
    assertThat((Object) throwable.getSuppressed()[1]).isEqualTo(suppressed2);
  }

  static class E1 extends Exception {
    String name;

    public E1(String name) {
      this.name = name;
    }

    public int methodE1() {
      return 0;
    }

    @Override
    public String getMessage() {
      return name;
    }
  }

  static class E2 extends E1 {
    public E2(String name) {
      super(name);
    }

    public int methodE2() {
      return 1;
    }
  }

  static class E3 extends E1 {
    public E3(String name) {
      super(name);
    }

    public int methodE3() {
      return 2;
    }
  }

  @Test
  public void testMultiExceptions() {

    int choose = 0;

    try {
      if (choose == 0) {
        throw new E1("e1");
      } else if (choose == 1) {
        throw new E2("e2");
      }

      fail("Exception was not trown");
    } catch (E2 | E3 x) {
      // The compiler will assign x a common supertype/superinterface of E2 and E3.
      // Here we make sure that this clause is not entered when the supertype is thrown.
      fail("Caught E1 instead of E2|E3");
    } catch (E1 expected) {
    }
  }

  private static Object unoptimizableId(Object o) {
    if (Math.random() > -10) {
      return o;
    }
    return null;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPrimitiveCastsFromObject() {
    Object o = unoptimizableId((byte) 2);
    assertThat((int) (byte) o).isEqualTo((int) (byte) 2);
    o = unoptimizableId((short) 3);
    assertThat((int) (short) o).isEqualTo((int) (short) 3);
    o = unoptimizableId(1);
    assertThat((int) o).isEqualTo(1);
    o = unoptimizableId(1L);
    assertThat((Object) (long) o).isEqualTo(1L);
    o = unoptimizableId(0.1f);
    assertThat((Object) (float) o).isEqualTo(0.1f);
    o = unoptimizableId(0.1);
    assertThat((Object) (double) o).isEqualTo(0.1);
    o = unoptimizableId(true);
    assertThat((Object) (boolean) o).isEqualTo(true);
    o = unoptimizableId('a');
    assertThat((int) (char) o).isEqualTo((int) 'a');
    // Test cast from supers.
    Number n = (Number) unoptimizableId(5);
    assertThat((int) n).isEqualTo(5);
    Serializable s = (Serializable) unoptimizableId(6);
    assertThat((int) s).isEqualTo(6);
    Comparable<Integer> c = (Comparable<Integer>) unoptimizableId(7);
    assertThat((int) c).isEqualTo(7);

    // Failing casts.
    assertThrows(
        ClassCastException.class,
        () -> {
          boolean unused = (boolean) unoptimizableId('a');
        });
    assertThrows(
        ClassCastException.class,
        () -> {
          int unused = (int) unoptimizableId("string");
        });
  }

  private static void assertContentsInOrder(Iterable<String> contents, String... elements) {
    assertThat((Object) contents.toString()).isEqualTo(Arrays.asList(elements).toString());
  }
}
