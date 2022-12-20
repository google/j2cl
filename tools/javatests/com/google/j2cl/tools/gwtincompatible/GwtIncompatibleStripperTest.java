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
import com.google.common.base.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JavaPreprocessor}. */
@RunWith(JUnit4.class)
public class GwtIncompatibleStripperTest {

  @Test
  public void testNoProcess() {
    String content = "public class Foo {}";
    assertEquals(content, GwtIncompatibleStripper.strip(content, "GwtIncompatible"));
  }

  @Test
  public void testNoProcessOtherIncompatible() {
    String content = "@J2kIncompatible\npublic class Foo {}";
    assertEquals(content, GwtIncompatibleStripper.strip(content, "GwtIncompatible"));
  }

  @Test
  public void testNoProcessString() {
    String content = "public class Foo {String a = \"@GwtIncompatible\");}";
    assertEquals(content, GwtIncompatibleStripper.strip(content, "GwtIncompatible"));
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
                Strings.repeat(" ", "import a.b.X;".length()),
                Strings.repeat(" ", "@GwtIncompatible".length()),
                Strings.repeat(" ", "public class Foo {".length()),
                Strings.repeat(" ", "  public X m() {return null;}".length()),
                Strings.repeat(" ", "}".length()));
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
  }

  @Test
  public void testProcessJ2ktIncompatibleClass() {
    String before =
        Joiner.on("\n")
            .join(
                "import a.b.X;",
                "@J2ktIncompatible",
                "public class Foo {",
                "  public X m() {return null;}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                Strings.repeat(" ", "import a.b.X;".length()),
                Strings.repeat(" ", "@J2ktIncompatible".length()),
                Strings.repeat(" ", "public class Foo {".length()),
                Strings.repeat(" ", "  public X m() {return null;}".length()),
                Strings.repeat(" ", "}".length()));
    assertEquals(after, GwtIncompatibleStripper.strip(before, "J2ktIncompatible"));
  }

  @Test
  public void testProcessMultipleAnnotatons() {
    String before =
        Joiner.on("\n")
            .join(
                "import a.b.X;",
                "@GwtIncompatible",
                "@J2ktIncompatible",
                "public class Foo {",
                "  public X m() {return null;}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                Strings.repeat(" ", "import a.b.X;".length()),
                Strings.repeat(" ", "@GwtIncompatible".length()),
                Strings.repeat(" ", "@J2ktIncompatible".length()),
                Strings.repeat(" ", "public class Foo {".length()),
                Strings.repeat(" ", "  public X m() {return null;}".length()),
                Strings.repeat(" ", "}".length()));
    assertEquals(after, GwtIncompatibleStripper.strip(before, "J2ktIncompatible"));
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
                Strings.repeat(" ", "import a.b.D;".length()),
                "public class Foo {",
                "  public C m() {}",
                Strings.repeat(" ", "  @Nullable".length()),
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  public D n() {}".length()),
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
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
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  public String b;".length()),
                "  public String c;",
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
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
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  public void m();".length()),
                "  public String c;",
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  public void n();".length()),
                "  public void o();",
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  private static class X {".length()),
                Strings.repeat(" ", "    void q() {}".length()),
                Strings.repeat(" ", "    @GwtIncompatible".length()),
                Strings.repeat(" ", "    void r() {}".length()),
                Strings.repeat(" ", "  }".length()),
                "  String s;",
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
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
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  public void n() {foo(x /* the value of x */);}".length()),
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
  }

  @Test
  public void testTabs() {
    String before =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public A m() {}",
                "  @GwtIncompatible",
                "  \tpublic B n() {}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public A m() {}",
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                "  \t" + Strings.repeat(" ", "public B n() {}".length()),
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
  }

  @Test
  public void testMultibyteChars() {
    String before =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public A m() {}",
                "  @GwtIncompatible",
                "  //மெ.பை.",
                "  public B n() {}",
                "}");
    String after =
        Joiner.on("\n")
            .join(
                "public class Foo {",
                "  public A m() {}",
                Strings.repeat(" ", "  @GwtIncompatible".length()),
                Strings.repeat(" ", "  //மெ.பை.".length()),
                Strings.repeat(" ", "  public B n() {}".length()),
                "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, "GwtIncompatible"));
  }
}
