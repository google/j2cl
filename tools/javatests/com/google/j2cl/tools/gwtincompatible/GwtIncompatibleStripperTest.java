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
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GwtIncompatibleStripperTest {

  @Test
  public void testNoProcess() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testNoProcessOtherIncompatible() {
    assertAnnotatedCodeStripped(
        """
        @J2kIncompatible
        public class Foo {}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testNoProcessString() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {String a = "@GwtIncompatible");}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessClass() {
    assertAnnotatedCodeStripped(
        """
        -import a.b.X;
        -@GwtIncompatible
        -public class Foo {
        -  public X m() {return null;}
        -}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessJ2ktIncompatibleClass() {
    assertAnnotatedCodeStripped(
        """
        -import a.b.X;
        -@J2ktIncompatible
        -public class Foo {
        -  public X m() {return null;}
        -}
        """,
        "J2ktIncompatible");
  }

  @Test
  public void testProcessMultipleAnnotationsPresent() {
    assertAnnotatedCodeStripped(
        """
        -import a.b.X;
        -@GwtIncompatible
        -@J2ktIncompatible
        -public class Foo {
        -  public X m() {return null;}
        -}
        """,
        "J2ktIncompatible");
  }

  @Test
  public void testProcessMultipleAnnotationsToStrip() {
    assertAnnotatedCodeStripped(
        """
        -import a.b.X;
        -import a.b.Y;
        -import a.b.Z;
        public class Foo {
        -  @GwtIncompatible
        -  @J2ktIncompatible
        -  public X m() {return null;}
        -  @GwtIncompatible
        -  public Y j2ktOnly() {return null;}
        -  @J2ktIncompatible
        -  public Z gwtOnly() {return null;}
        }
        """,
        "J2ktIncompatible",
        "GwtIncompatible");
  }

  @Test
  public void testProcessMethod() {
    assertAnnotatedCodeStripped(
        """
        import a.b.C;
        -import a.b.D;
        public class Foo {
          public C m() {}
        -  @Nullable
        -  @GwtIncompatible
        -  public D n() {}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessField() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
          public String a;
        -  @GwtIncompatible
        -  public String b;
          public String c;
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessEnumType() {
    assertAnnotatedCodeStripped(
        """
        -@GwtIncompatible public enum Foo {}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessEnumConstant() {
    assertAnnotatedCodeStripped(
        """
        public enum Foo {
          A,
        -  @GwtIncompatible
        -  B,
          C;
         }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessAnnotationType() {
    assertAnnotatedCodeStripped(
        """
        -@GwtIncompatible public @interface Foo {}
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessAnnotationMember() {
    assertAnnotatedCodeStripped(
        """
        public @interface Foo {
          public String m();
        -  @GwtIncompatible
        -  public String n();
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessMultiple() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  @GwtIncompatible
        -  public void m();
          public String c;
        -  @GwtIncompatible
        -  public void n();
          public void o();
        -  @GwtIncompatible
        -  private static class X {
        -    void q() {}
        -    @GwtIncompatible
        -    void r() {}
        -  }
          String s;
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessUnrelatedAnnotations() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  @GwtIncompatible
        -  @Override
        -  public void m() {}
        -  @Override
        -  @GwtIncompatible
        -  public void n() {}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessJavadoc() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  /**
        -   * doc
        -   */
        -  @GwtIncompatible
        -  public void m() {}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessMultiVariable() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  @GwtIncompatible
        -  public String a, b;
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessMultiVariableJavadoc() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  /**
        -   * doc
        -   */
        -  @GwtIncompatible
        -  public String a, b;
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessAnnotationBeforeJavadoc() {
    // Note that this is NOT considered a javadoc by javac.
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  @GwtIncompatible
        -  /**"
        -   * doc"
        -   */"
        -  public String a, b;
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessCommentAfterJavadoc() {
    // Note that this is STILL considered a javadoc by javac.
    assertAnnotatedCodeStripped(
        """
        public class Foo {
        -  /**
        -   * doc
        -   */
        -  // /**
        -  @GwtIncompatible
        -  public String a, b;
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testNestedComment() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
          public void m() {}
        -  @GwtIncompatible
        -  public void n() {foo(x /* the value of x */);}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testTabs() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
          public A m() {}
        -  @GwtIncompatible
        -  \tpublic B n() {}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testMultibyteChars() {
    assertAnnotatedCodeStripped(
        """
        public class Foo {
          public A m() {}
        -  @GwtIncompatible
        -  //மெ.பை.
        -  public B n() {}
        }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessRecord() {
    assertAnnotatedCodeStripped(
        """
            "public class Foo {",
            "-  @GwtIncompatible",
            "-  public record Bar(int a, String b) {}",
            }
        """,
        "GwtIncompatible");
  }

  @Test
  public void testProcessSealedClass() {
    assertAnnotatedCodeStripped(
        """
            "public class Foo {",
            "-  @GwtIncompatible",
            "-  public sealed class Bar permits Baz {}",
            "  public final class Baz extends Bar {}",
        }
        """,
        "GwtIncompatible  ");
  }

  private static void assertAnnotatedCodeStripped(String code, String... annotations) {
    assertEquals(
        after(code),
        GwtIncompatibleStripper.strip(before(code), ImmutableList.copyOf(annotations)));
  }

  private static String before(String code) {
    return transformMarkedLines(code, Function.identity());
  }

  private static String after(String code) {
    return transformMarkedLines(code, GwtIncompatibleStripperTest::stripLine);
  }

  private static String transformMarkedLines(String code, Function<String, String> transformer) {
    return Splitter.on('\n')
        .splitToStream(code)
        .map(
            line ->
                !line.isEmpty() && line.charAt(0) == '-'
                    ? transformer.apply(line.substring(1))
                    : line)
        .collect(joining("\n"));
  }

  private static String stripLine(String lineContent) {
    checkArgument(
        !lineContent.contains("\n"), "strip() should only be called with one line at a time.");
    return lineContent.replaceAll("[^\\s]", " ");
  }
}
