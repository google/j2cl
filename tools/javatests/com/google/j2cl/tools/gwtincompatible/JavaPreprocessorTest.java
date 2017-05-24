/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.tools.gwtincompatible;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JavaPreprocessor}. */
@RunWith(JUnit4.class)
public class JavaPreprocessorTest {

  @Test
  public void testNoProcess() {
    String content = "public class Foo {}";
    assertEquals(content, JavaPreprocessor.processFile(content));
  }

  @Test
  public void testNoProcessString() {
    String content = "public class Foo {String a = \"@GwtIncompatible\");}";
    assertEquals(content, JavaPreprocessor.processFile(content));
  }

  @Test
  public void testProcessClass() {
    String before =
        Joiner.on("\n")
            .join(
                "import a.b.X;",
                "@GwtIncompatible",
                "public class Foo {",
                "  public X m() {return null;}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "/*import a.b.X;*/",
                "/*@GwtIncompatible",
                "public class Foo {",
                "  public X m() {return null;}",
                "}*/");
    assertEquals(after, JavaPreprocessor.processFile(before));
  }

  @Test
  public void testProcessMethod() {
    String before =
        Joiner.on("\n")
            .join(
                "import a.b.C;",
                "import a.b.D;",
                "public class Foo {",
                "  public C m() {}",
                "  @Nullable",
                "  @GwtIncompatible",
                "  public D n() {}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "import a.b.C;",
                "/*import a.b.D;*/",
                "public class Foo {",
                "  public C m() {}",
                "  /*@Nullable",
                "  @GwtIncompatible",
                "  public D n() {}*/",
                "}");
    assertEquals(after, JavaPreprocessor.processFile(before));
  }

  @Test
  public void testProcessField() {
    String before =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public String a;",
                "  @GwtIncompatible",
                "  public String b;",
                "  public String c;",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public String a;",
                "  /*@GwtIncompatible",
                "  public String b;*/",
                "  public String c;",
                "}");
    assertEquals(after, JavaPreprocessor.processFile(before));
  }

  @Test
  public void testProcessMultiple() {
    String before =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  @GwtIncompatible",
                "  public void m();",
                "  public String c;",
                "  @GwtIncompatible",
                "  public void n();",
                "  public void o();",
                "  @GwtIncompatible",
                "  private static class X {",
                "    void q() {}",
                "    @GwtIncompatible",
                "    void r() {}",
                "  }",
                "  String s;",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  /*@GwtIncompatible",
                "  public void m();*/",
                "  public String c;",
                "  /*@GwtIncompatible",
                "  public void n();*/",
                "  public void o();",
                "  /*@GwtIncompatible",
                "  private static class X {",
                "    void q() {}",
                "    @GwtIncompatible",
                "    void r() {}",
                "  }*/",
                "  String s;",
                "}");
    assertEquals(after, JavaPreprocessor.processFile(before));
  }

  @Test
  public void testNestedComment() {
    String before =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public void m() {}",
                "  @GwtIncompatible",
                "  public void n() {foo(x /* the value of x */);}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public void m() {}",
                "  /*@GwtIncompatible",
                "  public void n() {foo(x /* the value of x **);}*/",
                "}");
    assertEquals(after, JavaPreprocessor.processFile(before));
  }
}
