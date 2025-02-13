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

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GwtIncompatibleStripperTest {

  @Test
  public void testNoProcess() {
    String content = "public class Foo {}";
    assertEquals(
        content, GwtIncompatibleStripper.strip(content, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testNoProcessOtherIncompatible() {
    String content = "@J2kIncompatible\npublic class Foo {}";
    assertEquals(
        content, GwtIncompatibleStripper.strip(content, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testNoProcessString() {
    String content = "public class Foo {String a = \"@GwtIncompatible\");}";
    assertEquals(
        content, GwtIncompatibleStripper.strip(content, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessClass() {
    String before =
        lines(
            "import a.b.X;",
            "@GwtIncompatible",
            "public class Foo {",
            "  public X m() {return null;}",
            "}");
    String after =
        lines(
            stripped("import a.b.X;"),
            stripped("@GwtIncompatible"),
            stripped("public class Foo {"),
            stripped("  public X m() {return null;}"),
            stripped("}"));
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessJ2ktIncompatibleClass() {
    String before =
        lines(
            "import a.b.X;",
            "@J2ktIncompatible",
            "public class Foo {",
            "  public X m() {return null;}",
            "}");
    String after =
        lines(
            stripped("import a.b.X;"),
            stripped("@J2ktIncompatible"),
            stripped("public class Foo {"),
            stripped("  public X m() {return null;}"),
            stripped("}"));
    assertEquals(
        after, GwtIncompatibleStripper.strip(before, ImmutableList.of("J2ktIncompatible")));
  }

  @Test
  public void testProcessMultipleAnnotationsPresent() {
    String before =
        lines(
            "import a.b.X;",
            "@GwtIncompatible",
            "@J2ktIncompatible",
            "public class Foo {",
            "  public X m() {return null;}",
            "}");
    String after =
        lines(
            stripped("import a.b.X;"),
            stripped("@GwtIncompatible"),
            stripped("@J2ktIncompatible"),
            stripped("public class Foo {"),
            stripped("  public X m() {return null;}"),
            stripped("}"));
    assertEquals(
        after, GwtIncompatibleStripper.strip(before, ImmutableList.of("J2ktIncompatible")));
  }

  @Test
  public void testProcessMultipleAnnotationsToStrip() {
    String before =
        lines(
            "import a.b.X;",
            "import a.b.Y;",
            "import a.b.Z;",
            "public class Foo {",
            "  @GwtIncompatible",
            "  @J2ktIncompatible",
            "  public X m() {return null;}",
            "  @GwtIncompatible",
            "  public Y j2ktOnly() {return null;}",
            "  @J2ktIncompatible",
            "  public Z gwtOnly() {return null;}",
            "}");
    String after =
        lines(
            stripped("import a.b.X;"),
            stripped("import a.b.Y;"),
            stripped("import a.b.Z;"),
            "public class Foo {",
            stripped("  @GwtIncompatible"),
            stripped("  @J2ktIncompatible"),
            stripped("  public X m() {return null;}"),
            stripped("  @GwtIncompatible"),
            stripped("  public Y j2ktOnly() {return null;}"),
            stripped("  @J2ktIncompatible"),
            stripped("  public Z gwtOnly() {return null;}"),
            "}");
    assertEquals(
        after,
        GwtIncompatibleStripper.strip(
            before, ImmutableList.of("J2ktIncompatible", "GwtIncompatible")));
  }

  @Test
  public void testProcessMethod() {
    String before =
        lines(
            "import a.b.C;",
            "import a.b.D;",
            "public class Foo {",
            "  public C m() {}",
            "  @Nullable",
            "  @GwtIncompatible",
            "  public D n() {}",
            "}");
    String after =
        lines(
            "import a.b.C;",
            stripped("import a.b.D;"),
            "public class Foo {",
            "  public C m() {}",
            stripped("  @Nullable"),
            stripped("  @GwtIncompatible"),
            stripped("  public D n() {}"),
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessField() {
    String before =
        lines(
            "public class Foo {",
            "  public String a;",
            "  @GwtIncompatible",
            "  public String b;",
            "  public String c;",
            "}");
    String after =
        lines(
            "public class Foo {",
            "  public String a;",
            stripped("  @GwtIncompatible"),
            stripped("  public String b;"),
            "  public String c;",
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessEnumType() {
    String before = "@GwtIncompatible public enum Foo {}";
    String after = stripped("@GwtIncompatible public enum Foo {}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessEnumConstant() {
    String before = lines("public enum Foo {", "  A,", "  @GwtIncompatible", "  B,", "  C;", "}");
    String after =
        lines(
            "public enum Foo {",
            "  A,",
            stripped("  @GwtIncompatible"),
            stripped("  B,"),
            "  C;",
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessAnnotationType() {
    String before = "@GwtIncompatible public @interface Foo {}";
    String after = stripped("@GwtIncompatible public @interface Foo {}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessAnnotationMember() {
    String before =
        lines(
            "public @interface Foo {",
            "  public String m();",
            "  @GwtIncompatible",
            "  public String n();",
            "}");
    String after =
        lines(
            "public @interface Foo {",
            "  public String m();",
            stripped("  @GwtIncompatible"),
            stripped("  public String n();"),
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testProcessMultiple() {
    String before =
        lines(
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
        lines(
            "public class Foo {",
            stripped("  @GwtIncompatible"),
            stripped("  public void m();"),
            "  public String c;",
            stripped("  @GwtIncompatible"),
            stripped("  public void n();"),
            "  public void o();",
            stripped("  @GwtIncompatible"),
            stripped("  private static class X {"),
            stripped("    void q() {}"),
            stripped("    @GwtIncompatible"),
            stripped("    void r() {}"),
            stripped("  }"),
            "  String s;",
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testNestedComment() {
    String before =
        lines(
            "public class Foo {",
            "  public void m() {}",
            "  @GwtIncompatible",
            "  public void n() {foo(x /* the value of x */);}",
            "}");
    String after =
        lines(
            "public class Foo {",
            "  public void m() {}",
            stripped("  @GwtIncompatible"),
            stripped("  public void n() {foo(x /* the value of x */);}"),
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testTabs() {
    String before =
        lines(
            "public class Foo {",
            "  public A m() {}",
            "  @GwtIncompatible",
            "  \tpublic B n() {}",
            "}");
    String after =
        lines(
            "public class Foo {",
            "  public A m() {}",
            stripped("  @GwtIncompatible"),
            "  \t" + stripped("public B n() {}"),
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  @Test
  public void testMultibyteChars() {
    String before =
        lines(
            "public class Foo {",
            "  public A m() {}",
            "  @GwtIncompatible",
            "  //மெ.பை.",
            "  public B n() {}",
            "}");
    String after =
        lines(
            "public class Foo {",
            "  public A m() {}",
            stripped("  @GwtIncompatible"),
            stripped("  //மெ.பை."),
            stripped("  public B n() {}"),
            "}");
    assertEquals(after, GwtIncompatibleStripper.strip(before, ImmutableList.of("GwtIncompatible")));
  }

  private static String lines(String... lines) {
    return LINE_JOINER.join(lines);
  }

  private static String stripped(String lineContent) {
    checkArgument(
        !lineContent.contains("\n"), "stripped() should only be called with one line at a time.");
    return " ".repeat(lineContent.length());
  }

  private static final Joiner LINE_JOINER = Joiner.on('\n');
}
