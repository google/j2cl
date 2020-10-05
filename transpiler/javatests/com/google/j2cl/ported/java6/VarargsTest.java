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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the new JDK 1.5 varargs functionality. */
@RunWith(JUnit4.class)
@SuppressWarnings({"BoxedPrimitiveConstructor", "MethodCanBeStatic", "InexactNonVarargsCall"})
public class VarargsTest {

  private static class Foo {}

  @SuppressWarnings("all")
  @Test
  public void testNullEmpty() {
    assertThat(vararg()).isNotNull();
    assertThat(vararg(null)).isNull();
    assertThat(vararg((String) null)).isNotNull();
    assertThat(vararg((String[]) null)).isNull();
  }

  @Test
  public void testVararg() {
    String[] expected = new String[] {"1", "2", "3"};
    String[] actual = vararg("1", "2", "3");
    assertThat(Arrays.equals(expected, actual)).isTrue();

    expected = new String[] {};
    actual = vararg();
    assertThat(Arrays.equals(expected, actual)).isTrue();
  }

  @Test
  public void testVarargBoxing() {
    int[] expected = new int[] {1, 2, 3};
    int[] actual = varargUnboxed(1, 2, 3);
    assertThat(Arrays.equals(expected, actual)).isTrue();
    actual = varargUnboxed(new Integer(1), 2, new Integer(3));
    assertThat(Arrays.equals(expected, actual)).isTrue();

    expected = new int[] {};
    actual = varargUnboxed();
    assertThat(Arrays.equals(expected, actual)).isTrue();
  }

  /** Test for issue 8736. */
  @Test
  public void testNullEmptyUninstantiable_Varargs() {
    assertThat(fooVararg()).isNotNull();
  }

  @Test
  public void testNullEmptyUninstantiable_NoVarargs() {
    assertThat(fooIdent(new Foo[] {})).isNotNull();
  }

  private String[] vararg(String... args) {
    return args;
  }

  private Foo[] fooVararg(Foo... args) {
    return args;
  }

  private Foo[] fooIdent(Foo[] args) {
    return args;
  }

  private int[] varargUnboxed(int... args) {
    return args;
  }
}
