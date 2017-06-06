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
package com.google.j2cl.transpiler.regression.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for types that are devirtualized. */
@RunWith(JUnit4.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class NativeDevirtualizationTest {

  public static final String HELLO_WORLD = "Hello, World!";

  @Test
  public void testStringDevirtualization() {
    String x = "hi";
    assertThat(x).isEqualTo("hi");
    assertThat(x.toString()).isEqualTo("hi");
    x = new String();
    assertThat(x).isEqualTo("");
    x = new String(x);
    assertThat(x).isEqualTo("");
    x = new String("hi");
    assertThat(x).isEqualTo("hi");
    assertThat(x.charAt(1)).isEqualTo('i');
    assertThat(x.concat("yay")).isEqualTo("hiyay");
    assertThat(x + x).isEqualTo("hihi");

    assertThat(
            ("blah" + String.valueOf(new char[] {'a', 'b', 'c'}) + true + false + null + 'c' + 27))
        .isEqualTo("blahabctruefalsenullc27");
    Object s = HELLO_WORLD;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) s.getClass()).isEqualTo(String.class);
    assertThat(s.toString()).isEqualTo(HELLO_WORLD);

    assertThat(s.equals(HELLO_WORLD)).isTrue();
    assertThat(HELLO_WORLD.equals(s)).isTrue();
    assertThat(s.equals("")).isFalse();
    assertThat("".equals(s)).isFalse();
    assertThat(s.toString()).isEqualTo(HELLO_WORLD);
    assertThat(s instanceof String).isTrue();

    Comparable b = HELLO_WORLD;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) b.getClass()).isEqualTo(String.class);
    assertThat(b.toString()).isEqualTo(HELLO_WORLD);
    assertThat(b.hashCode()).isEqualTo(HELLO_WORLD.hashCode());

    assertThat(((Comparable) HELLO_WORLD).compareTo(b) == 0).isTrue();
    assertThat(b.compareTo(HELLO_WORLD) == 0).isTrue();
    assertThat(((Comparable) "A").compareTo(b) < 0).isTrue();
    assertThat(b.compareTo("A") > 0).isTrue();
    assertThat(((Comparable) "Z").compareTo(b) > 0).isTrue();
    assertThat(b.compareTo("Z") < 0).isTrue();
    assertThat(b instanceof String).isTrue();

    CharSequence c = HELLO_WORLD;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) c.getClass()).isEqualTo(String.class);
    assertThat(c.toString()).isEqualTo(HELLO_WORLD);
    assertThat(c.hashCode()).isEqualTo(HELLO_WORLD.hashCode());

    assertThat(c.charAt(1)).isEqualTo('e');
    assertThat(c.length()).isEqualTo(13);
    assertThat(c.subSequence(1, 5)).isEqualTo("ello");
    boolean b1 = c instanceof String;
    assertThat(b1).isTrue();
  }

  @Test
  public void testBooleanDevirtualization() {
    final Boolean FALSE = false;
    Object o = FALSE;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) o.getClass()).isEqualTo(Boolean.class);
    assertThat(o.toString()).isEqualTo("false");

    assertThat((Boolean) o).isFalse();
    assertThat(o instanceof Boolean).isTrue();
    assertThat(o instanceof Number).isFalse();
    assertThat(o instanceof Comparable).isTrue();

    Comparable b = FALSE;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) b.getClass()).isEqualTo(Boolean.class);
    assertThat(b.toString()).isEqualTo("false");
    assertThat(b.hashCode()).isEqualTo(FALSE.hashCode());
  }

  @Test
  public void testDoubleDevirtualization() {
    final Double ONE_POINT_ONE = 1.1d;
    Object o = ONE_POINT_ONE;
    assertThat((Object) o.getClass()).isEqualTo(Double.class);
    assertThat(o.toString()).isEqualTo("1.1");

    assertThat(o).isEqualTo(1.1d);
    assertThat(o instanceof Double).isTrue();
    assertThat(o instanceof Number).isTrue();
    assertThat(o instanceof Comparable).isTrue();

    Comparable b = ONE_POINT_ONE;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) b.getClass()).isEqualTo(Double.class);
    assertThat(b.toString()).isEqualTo("1.1");
    assertThat(b.hashCode()).isEqualTo(ONE_POINT_ONE.hashCode());
    assertThat(b.compareTo((Double) 1.2d) < 0).isTrue();

    Number c = ONE_POINT_ONE;
    // TODO(b/30126552): remove cast from getClass() to Object once GwtIncompatible is handled
    // correctly.
    assertThat((Object) c.getClass()).isEqualTo(Double.class);
    assertThat(c.toString()).isEqualTo("1.1");
    assertThat(c.hashCode()).isEqualTo(ONE_POINT_ONE.hashCode());
    assertThat(c.intValue()).isEqualTo(1);
  }
}
