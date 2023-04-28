/*
 * Copyright 2008 Google Inc.
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
package com.google.j2cl.jre.java.lang;

import static com.google.j2cl.jre.testing.TestUtils.isJvm;
import static com.google.j2cl.jre.testing.TestUtils.isWasm;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.j2cl.jre.testing.TestUtils;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/** Tests java.lang.String. */
public class StringTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testCharAt() {
    assertEquals('b', hideFromCompiler("abc").charAt(1));

    try {
      hideFromCompiler("abc").charAt(-1);
      fail();
    } catch (StringIndexOutOfBoundsException ignore) {
      // expected
    }

    try {
      hideFromCompiler("abc").charAt(3);
      fail();
    } catch (StringIndexOutOfBoundsException ignore) {
      // expected
    }
  }

  public void testCodePoint() {
    String testPlain = hideFromCompiler("CAT");
    String testUnicode = hideFromCompiler("C\uD801\uDF00T");
    assertEquals("CAT", new String(new int[] {'C', 'A', 'T'}, 0, 3));
    assertEquals("C\uD801\uDF00T",
        new String(new int[] {'C', 67328, 'T'}, 0, 3));
    assertEquals("\uD801\uDF00", new String(new int[] {'C', 67328, 'T'}, 1, 1));
    assertEquals(65, testPlain.codePointAt(1));
    assertEquals("codePointAt fails on surrogate pair", 67328,
        testUnicode.codePointAt(1));
    assertEquals(65, testPlain.codePointBefore(2));
    assertEquals("codePointBefore fails on surrogate pair", 67328,
        testUnicode.codePointBefore(3));
    assertEquals("codePointCount(plain): ", 3, testPlain.codePointCount(0, 3));
    assertEquals("codePointCount(unicode): ", 3, testUnicode.codePointCount(0,
        4));
    assertEquals(1, testPlain.codePointCount(1, 2));
    assertEquals(1, testUnicode.codePointCount(1, 2));
    assertEquals(2, testUnicode.codePointCount(2, 4));
    assertEquals(1, testUnicode.offsetByCodePoints(0, 1));
    assertEquals("offsetByCodePoints(1,1): ", 3,
        testUnicode.offsetByCodePoints(1, 1));
    assertEquals("offsetByCodePoints(2,1): ", 3,
        testUnicode.offsetByCodePoints(2, 1));
    assertEquals(4, testUnicode.offsetByCodePoints(3, 1));
    assertEquals(1, testUnicode.offsetByCodePoints(2, -1));
    assertEquals(1, testUnicode.offsetByCodePoints(3, -1));
    assertEquals("offsetByCodePoints(4.-1): ", 3,
        testUnicode.offsetByCodePoints(4, -1));
    assertEquals(0, testUnicode.offsetByCodePoints(3, -2));
    /*
     * The next line contains a Unicode character outside the base multilingual
     * plane -- it may not show properly depending on your fonts, etc. The
     * character is the Gothic letter Faihu, or U+10346. We use it to verify
     * that multi-char UTF16 characters are handled properly.
     *
     * In Windows 2000, registry changes are required to support non-BMP
     * characters (or surrogates in general) -- surrogates are not supported
     * before Win2k and they are enabled by default in WinXP and later.
     *
     * [HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows
     * NT\CurrentVersion\LanguagePack] SURROGATE=(REG_DWORD)0x00000002
     *
     * [HKEY_CURRENT_USER\Software\Microsoft\Internet
     * Explorer\International\Scripts\42] IEFixedFontName=[Surrogate Font Face
     * Name] IEPropFontName=[Surrogate Font Face Name]
     */
    String nonBmpChar = hideFromCompiler("𐍆");
    assertEquals("\uD800\uDF46", nonBmpChar);
    assertEquals(0x10346, nonBmpChar.codePointAt(0));
    assertEquals(2, nonBmpChar.length());
    assertEquals(1, nonBmpChar.codePointCount(0, 2));
  }

  public void testCompareTo() {
    assertEquals(0, hideFromCompiler("a").compareTo("a"));
    assertEquals(-1, hideFromCompiler("a").compareTo("b"));
    assertEquals(1, hideFromCompiler("b").compareTo("a"));

    assertEquals(-1, hideFromCompiler("a").compareTo("aa"));
    assertEquals(1, hideFromCompiler("aa").compareTo("a"));

    // Ensure it's not a locale-sensitive comparison per Java spec.
    assertEquals(0, hideFromCompiler("ä").compareTo("ä"));
    assertTrue(hideFromCompiler("ä").compareTo("z") > 0);
    assertEquals(0, hideFromCompiler("İ").compareTo("İ"));
    assertTrue(hideFromCompiler("İ").compareTo("z") > 0);
  }

  public static void testCompareToNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().compareTo("");
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      returnNull().compareTo(returnNull());
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      "".compareTo(returnNull());
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testCompareToIgnoreCase() {
    assertEquals(0, hideFromCompiler("a").compareToIgnoreCase("a"));
    assertEquals(0, hideFromCompiler("a").compareToIgnoreCase("A"));
    assertEquals(-1, hideFromCompiler("a").compareToIgnoreCase("b"));
    assertEquals(-1, hideFromCompiler("a").compareToIgnoreCase("B"));
    assertEquals(1, hideFromCompiler("b").compareToIgnoreCase("a"));
    assertEquals(1, hideFromCompiler("B").compareToIgnoreCase("a"));

    assertEquals(-1, hideFromCompiler("a").compareToIgnoreCase("aa"));
    assertEquals(-1, hideFromCompiler("a").compareToIgnoreCase("AA"));
    assertEquals(1, hideFromCompiler("aa").compareToIgnoreCase("a"));
    assertEquals(1, hideFromCompiler("aa").compareToIgnoreCase("A"));

    // Ensure it's not a locale-sensitive comparison per Java spec.
    assertEquals(0, hideFromCompiler("ä").compareToIgnoreCase("ä"));
    assertEquals(0, hideFromCompiler("ä").compareToIgnoreCase("Ä"));
    assertTrue(hideFromCompiler("ä").compareToIgnoreCase("z") > 0);
    assertTrue(hideFromCompiler("ä").compareToIgnoreCase("Z") > 0);
    assertTrue(hideFromCompiler("aä").compareToIgnoreCase("aa") > 0);
    assertTrue(hideFromCompiler("aä").compareToIgnoreCase("ab") > 0);
    assertEquals(0, hideFromCompiler("aä").compareToIgnoreCase("aä"));
    assertTrue(hideFromCompiler("삼").compareToIgnoreCase("집") < 0);
    assertEquals(0, hideFromCompiler("İ").compareToIgnoreCase("İ"));
    assertTrue(hideFromCompiler("İ").compareToIgnoreCase("z") < 0);
  }

  public void testConcat() {
    String abc = String.valueOf(new char[] {'a', 'b', 'c'});
    String def = String.valueOf(new char[] {'d', 'e', 'f'});
    String empty = String.valueOf(new char[] {});
    assertEquals("abcdef", abc + def);
    assertEquals("abcdef", abc.concat(def));
    assertEquals("", empty.concat(empty));
    char c = def.charAt(0);
    String s = abc;
    assertEquals("abcd", abc + 'd');
    assertEquals("abcd", abc + c);
    assertEquals("abcd", s + 'd');
    assertEquals("abcd", s + c);
    s += c;
    assertEquals("abcd", s);
  }

  public static void testConcatNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().concat("");
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      hideFromCompiler("").concat(returnNull());
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testConstructor() {
    char[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
    String constant = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    String shortString = String.valueOf(new char[] {'c', 'd', 'e'});
    assertEquals(constant, new String(hideFromCompiler(constant)));
    assertEquals(constant, new String(chars), constant);
    assertEquals(shortString, new String(chars, 2, 3), shortString);
    assertEquals("", new String(hideFromCompiler("")));
    assertEquals("", new String(new String(new String(new String(
        hideFromCompiler(""))))));
    assertEquals("", new String(new char[] {}));
    StringBuffer buf = new StringBuffer();
    buf.append('c');
    buf.append('a');
    buf.append('t');
    assertEquals("cat", new String(buf));
    StringBuilder sb = new StringBuilder();
    sb.append('c');
    sb.append('a');
    sb.append('t');
    assertEquals("cat", new String(sb));
    sb = new StringBuilder();
    sb.appendCodePoint(0x21);
    assertEquals("\u0021", new String(sb));
    sb = new StringBuilder();
    sb.appendCodePoint(0x10400);
    assertEquals("\uD801\uDC00", new String(sb));
  }

  public static void testConstructorNull() {

    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }
    try {
      new String(returnNull());
    } catch (NullPointerException e) {
      // expected
    }

    try {
      new String(hideFromCompiler((StringBuilder) null));
    } catch (NullPointerException e) {
      // expected
    }
  }

  public static void testConstructorBytes() {
    if (isWasm()) {
      // TODO(b/233695357): Re-enable when EmulatedCharsed is fixed.
      return;
    }

    byte[] bytes = new byte[] {'a', 'b', 'c', 'd', 'e', 'f'};
    String str = new String(bytes);
    assertEquals("abcdef", str);
    str = new String(bytes, 1, 3);
    assertEquals("bcd", str);
    try {
      new String(bytes, 1, 6);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public static void testConstructorLatin1() throws UnsupportedEncodingException {
    if (isWasm()) {
      // TODO(b/233695357): Re-enable when EmulatedCharsed is fixed.
      return;
    }

    internalTestConstructorLatin1("ISO-8859-1");
    internalTestConstructorLatin1("iso-8859-1");
  }

  private static void internalTestConstructorLatin1(String encoding)
      throws UnsupportedEncodingException {
    byte[] bytes =
        new byte[] {(byte) 0xE0, (byte) 0xDF, (byte) 0xE7, (byte) 0xD0, (byte) 0xE9, 'f'};
    String str = new String(bytes, encoding);
    assertEquals("àßçÐéf", str);
    str = new String(bytes, 1, 3, encoding);
    assertEquals("ßçÐ", str);
    try {
      new String(bytes, 1, 6, encoding);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, encoding);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2, encoding);
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 1, 6, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 6, 2, Charset.forName(encoding));
      assertTrue("Should have thrown IOOB in JVM", !isJvm());
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public static void testConstructorUtf8() throws UnsupportedEncodingException {
    if (isWasm()) {
      // TODO(b/233695357): Re-enable when EmulatedCharsed is fixed.
      return;
    }

    internalTestConstructorUtf8("UTF-8");
    internalTestConstructorUtf8("utf-8");
  }

  private static void internalTestConstructorUtf8(String encoding)
      throws UnsupportedEncodingException {
    byte[] bytes =
        new byte[] {
          (byte) 0xC3,
          (byte) 0xA0,
          (byte) 0xC3,
          (byte) 0x9F,
          (byte) 0xC3,
          (byte) 0xA7,
          (byte) 0xC3,
          (byte) 0x90,
          (byte) 0xC3,
          (byte) 0xA9,
          'f'
        };
    String str = new String(bytes, encoding);
    assertEquals("àßçÐéf", str);
    str = new String(bytes, 2, 6, encoding);
    assertEquals("ßçÐ", str);
    try {
      new String(bytes, 2, 12, encoding);
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, encoding);
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 12, 2, encoding);
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 2, 12, Charset.forName(encoding));
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, -1, 2, Charset.forName(encoding));
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      new String(bytes, 12, 2, Charset.forName(encoding));
      fail("Should have thrown IOOB");
    } catch (IndexOutOfBoundsException expected) {
    }

    // Byte count edge boundaries.
    bytes = new byte[] {0x00};
    assertEquals("\u0000", new String(bytes, encoding));
    assertEquals("\u0000", new String(bytes, Charset.forName(encoding)));
    bytes = new byte[] {0x7F};
    assertEquals("\u007F", new String(bytes, encoding));
    assertEquals("\u007F", new String(bytes, Charset.forName(encoding)));
    bytes = new byte[] {(byte) 0xC2, (byte) 0x80};
    assertEquals("\u0080", new String(bytes, encoding));
    assertEquals("\u0080", new String(bytes, Charset.forName(encoding)));
    bytes = new byte[] {(byte) 0xDF, (byte) 0xBF};
    assertEquals("\u07FF", new String(bytes, encoding));
    assertEquals("\u07FF", new String(bytes, Charset.forName(encoding)));
    bytes = new byte[] {(byte) 0xE0, (byte) 0xA0, (byte) 0x80};
    assertEquals("\u0800", new String(bytes, encoding));
    assertEquals("\u0800", new String(bytes, Charset.forName(encoding)));
    bytes = new byte[] {(byte) 0xEF, (byte) 0xBF, (byte) 0xBD};
    assertEquals("\uFFFD", new String(bytes, encoding));
    assertEquals("\uFFFD", new String(bytes, Charset.forName(encoding)));

    // Malformed case 1: Overlong encoding for '$'. We'll attempt 2, 3, and 4 bytes.
    bytes =
        new byte[] {
          (byte) 0xC0, // len 2
          (byte) 0xA4, // continuation | 0x24
        };
    assertEquals(utf8Replacement(2), new String(bytes, encoding));
    assertEquals(utf8Replacement(2), new String(bytes, Charset.forName(encoding)));

    bytes =
        new byte[] {
          (byte) 0xE0, // len 3
          (byte) 0x80, // continuation
          (byte) 0xA4, // continuation | 0x24
        };
    assertEquals(utf8Replacement(3), new String(bytes, encoding));
    assertEquals(utf8Replacement(3), new String(bytes, Charset.forName(encoding)));

    bytes =
        new byte[] {
          (byte) 0xF0, // len 4
          (byte) 0x80, // continuation
          (byte) 0x80, // continuation
          (byte) 0xA4, // continuation | 0x24
        };
    assertEquals(utf8Replacement(4), new String(bytes, encoding));
    assertEquals(utf8Replacement(4), new String(bytes, Charset.forName(encoding)));

    // Malformed case 2: First byte show length 4 but following byte is non-continuation.
    bytes =
        new byte[] {
          (byte) 0xF0, // len 4
          (byte) 0xC0, // non-continuation
          (byte) 0x80, // continuation
          (byte) 0x80, // continuation
          (byte) 0x80, // continuation, but a start should be here!
          (byte) 0xC2, // copyright symbol, length 2
          (byte) 0xA9, // copyright symbol, end
        };
    assertEquals(utf8Replacement(5) + "©", new String(bytes, encoding));
    assertEquals(utf8Replacement(5) + "©", new String(bytes, Charset.forName(encoding)));

    // Malformed case 3: First byte is continuation. Last two bytes are continuations of nothing.
    bytes =
        new byte[] {
          (byte) 0xF0, // len 4
          (byte) 0x80, // continuation
          (byte) 0xC2, // copyright symbol, length 2
          (byte) 0xA9, // copyright symbol, end
        };
    assertEquals(utf8Replacement(2) + "©", new String(bytes, encoding));
    assertEquals(utf8Replacement(2) + "©", new String(bytes, Charset.forName(encoding)));

    // Malformed case 4: First byte is length 4, but we see a new start before then
    bytes =
        new byte[] {
          (byte) 0x80, // continuation
          (byte) 0xC2, // copyright symbol, length 2
          (byte) 0xA9, // copyright symbol, end
          (byte) 0x80, // continuation
          (byte) 0x80, // continuation
        };
    assertEquals(utf8Replacement(1) + "©" + utf8Replacement(2), new String(bytes, encoding));
    assertEquals(
        utf8Replacement(1) + "©" + utf8Replacement(2),
        new String(bytes, Charset.forName(encoding)));

    // Malformed case 5: 0xFF and 0xFE are never valid, anywhere.
    bytes =
        new byte[] {
          (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFE,
        };
    assertEquals(utf8Replacement(4), new String(bytes, encoding));
    assertEquals(utf8Replacement(4), new String(bytes, Charset.forName(encoding)));

    // Malformed case 6: First byte states length of 4 but actual length is 2.
    bytes =
        new byte[] {
          (byte) 0xF0, // len 4
          'a', // continuation
        };
    assertEquals(utf8Replacement(1) + "a", new String(bytes, encoding));
    assertEquals(utf8Replacement(1) + "a", new String(bytes, Charset.forName(encoding)));

    // Malformed case 7: Unpaired high surrogate followed by a surrogate pair.
    bytes =
        new byte[] {
          (byte) 0xed,
          (byte) 0xa0,
          (byte) 0x81,
          (byte) 0xF0, // start U+1F9CB
          (byte) 0x9F,
          (byte) 0xA7,
          (byte) 0x8B, // end U+1F9CB
        };
    // JVM replaces the unpaired surrogate with a single replacement character, whereas the J2CL
    // emulation replaces each of the three bytes.
    assertEquals(utf8Replacement(isJvm() ? 1 : 3) + "\uD83E\uDDCB", new String(bytes, encoding));
    assertEquals(
        utf8Replacement(isJvm() ? 1 : 3) + "\uD83E\uDDCB",
        new String(bytes, Charset.forName(encoding)));
  }

  public void testContains() {
    // at the beginning
    assertTrue(hideFromCompiler("abcdef").contains("ab"));
    assertTrue(hideFromCompiler("abcdef").contains(new StringBuffer("ab")));
    // at the end
    assertTrue(hideFromCompiler("abcdef").contains("ef"));
    assertTrue(hideFromCompiler("abcdef").contains(new StringBuffer("ef")));
    // in the middle
    assertTrue(hideFromCompiler("abcdef").contains("cd"));
    assertTrue(hideFromCompiler("abcdef").contains(new StringBuffer("cd")));
    // the same
    assertTrue(hideFromCompiler("abcdef").contains("abcdef"));
    assertTrue(hideFromCompiler("abcdef").contains(new StringBuffer("abcdef")));
    // not present
    assertFalse(hideFromCompiler("abcdef").contains("z"));
    assertFalse(hideFromCompiler("abcdef").contains(new StringBuffer("z")));
  }

  public void testEndsWith() {
    String haystack = "abcdefghi";
    assertTrue("a", haystack.endsWith("defghi"));
    assertTrue("b", haystack.endsWith(haystack));
    assertFalse("c", haystack.endsWith(haystack + "j"));
  }

  public void testEquals() {
    assertFalse(hideFromCompiler("ABC").equals("abc"));
    assertFalse(hideFromCompiler("abc").equals("ABC"));
    assertTrue(hideFromCompiler("abc").equals("abc"));
    assertTrue(hideFromCompiler("ABC").equals("ABC"));
    assertFalse(hideFromCompiler("AbC").equals("aBC"));
    assertFalse(hideFromCompiler("AbC").equals("aBC"));
    assertTrue(hideFromCompiler("").equals(""));
    assertFalse(hideFromCompiler("").equals(null));

    // Test various variation of common prefix and suffix.
    assertTrue(hideFromCompiler("a").equals("a"));
    assertFalse(hideFromCompiler("a").equals("b"));
    assertTrue(hideFromCompiler("aa").equals("aa"));
    assertFalse(hideFromCompiler("aa").equals("ab"));
    assertFalse(hideFromCompiler("ba").equals("aa"));
    assertTrue(hideFromCompiler("aba").equals("aba"));
    assertFalse(hideFromCompiler("aba").equals("aca"));
    assertTrue(hideFromCompiler("abba").equals("abba"));
    assertFalse(hideFromCompiler("abca").equals("acba"));
    assertFalse(hideFromCompiler("acba").equals("abca"));
  }

  public static void testEqualsNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().equals("other");
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      returnNull().equals(returnNull());
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testEqualsIgnoreCase() {
    assertTrue(hideFromCompiler("ABC").equalsIgnoreCase("abc"));
    assertTrue(hideFromCompiler("abc").equalsIgnoreCase("ABC"));
    assertTrue(hideFromCompiler("abc").equalsIgnoreCase("abc"));
    assertTrue(hideFromCompiler("ABC").equalsIgnoreCase("ABC"));
    assertTrue(hideFromCompiler("AbC").equalsIgnoreCase("aBC"));
    assertTrue(hideFromCompiler("AbC").equalsIgnoreCase("aBC"));
    assertTrue(hideFromCompiler("").equalsIgnoreCase(""));
    assertFalse(hideFromCompiler("").equalsIgnoreCase(null));

    assertTrue(hideFromCompiler("ß").equalsIgnoreCase("ß"));
    assertFalse(hideFromCompiler("ß").equalsIgnoreCase("ss"));
    assertFalse(hideFromCompiler("ß").equalsIgnoreCase("SS"));

    // Test various variation of common prefix and suffix.
    assertTrue(hideFromCompiler("a").equalsIgnoreCase("A"));
    assertFalse(hideFromCompiler("a").equalsIgnoreCase("b"));
    assertTrue(hideFromCompiler("aa").equalsIgnoreCase("AA"));
    assertFalse(hideFromCompiler("aa").equalsIgnoreCase("ab"));
    assertFalse(hideFromCompiler("ba").equalsIgnoreCase("aa"));
    assertTrue(hideFromCompiler("aba").equalsIgnoreCase("aBa"));
    assertFalse(hideFromCompiler("aba").equalsIgnoreCase("aca"));
    assertTrue(hideFromCompiler("abba").equalsIgnoreCase("aBBa"));
    assertFalse(hideFromCompiler("abca").equalsIgnoreCase("acba"));
    assertFalse(hideFromCompiler("acba").equalsIgnoreCase("abca"));
  }

  public static void testEqualsIgnoreCaseNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().equalsIgnoreCase("other");
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      returnNull().equalsIgnoreCase(returnNull());
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testEqualsIgnoreCaseNonAscii() {
    assertTrue(
        hideFromCompiler("ö=>Ö, ç=>Ç, ş=>Ş, ğ=>Ğ, ü=>Ü, ı")
            .equalsIgnoreCase("Ö=>ö, Ç=>ç, Ş=>ş, Ğ=>ğ, Ü=>ü, ı")); // a.k.a "Turkey Test"

    assertTrue(hideFromCompiler("abcğa").equalsIgnoreCase("abcĞa"));
    assertFalse(hideFromCompiler("abcğa").equalsIgnoreCase("abcĞb"));
    assertFalse(hideFromCompiler("abcğa").equalsIgnoreCase("abcŞa"));

    // Test various variation of common prefix and suffix.
    assertTrue(hideFromCompiler("ğ").equalsIgnoreCase("Ğ"));
    assertFalse(hideFromCompiler("ğ").equalsIgnoreCase("ş"));
    assertTrue(hideFromCompiler("ağ").equalsIgnoreCase("aĞ"));
    assertFalse(hideFromCompiler("ğa").equalsIgnoreCase("aa"));
    assertTrue(hideFromCompiler("ağa").equalsIgnoreCase("aĞa"));
    assertFalse(hideFromCompiler("ağa").equalsIgnoreCase("aşa"));
    assertTrue(hideFromCompiler("ağğa").equalsIgnoreCase("aĞĞa"));
    assertFalse(hideFromCompiler("ağşa").equalsIgnoreCase("aşğa"));
    assertFalse(hideFromCompiler("aşğa").equalsIgnoreCase("ağşa"));
  }

  public void testContentEquals() throws Exception {
    String s = "abc";
    assertTrue(s.contentEquals(new StringBuffer("abc")));
    assertFalse(s.contentEquals(new StringBuffer("abd")));
    assertTrue(s.contentEquals(new StringBuffer("ab").append("c")));
    assertFalse(s.contentEquals(new StringBuffer("ab").append("d")));

    // Test also CharSequence overload.
    assertTrue(s.contentEquals((CharSequence) new StringBuffer("abc")));
    assertFalse(s.contentEquals((CharSequence) new StringBuffer("abd")));
    assertTrue(s.contentEquals((CharSequence) new StringBuffer("ab").append("c")));
    assertFalse(s.contentEquals((CharSequence) new StringBuffer("ab").append("d")));
  }

  public void testGetBytesAscii() {
    // Simple ASCII should get through any standard encoding (EBCDIC users,
    // you're out of luck).
    String str = "This is a simple ASCII string";
    byte[] bytes = str.getBytes();
    assertEquals(str.length(), bytes.length);
    for (int i = 0; i < str.length(); ++i) {
      assertEquals((byte) str.charAt(i), bytes[i]);
    }
    assertTrue(Arrays.equals(bytes, str.getBytes()));
  }

  public static void testGetBytesLatin1() throws UnsupportedEncodingException {
    if (isWasm()) {
      // TODO(b/233695357): Re-enable when EmulatedCharsed is fixed.
      return;
    }
    internalTestGetBytesLatin1("ISO-8859-1");
    internalTestGetBytesLatin1("iso-8859-1");
  }

  private static void internalTestGetBytesLatin1(String encoding)
      throws UnsupportedEncodingException {
    // Contains only ISO-Latin-1 characters.
    String str = "Îñtérñåtîöñålîzåtîöñ";
    byte[] bytes = str.getBytes(encoding);
    assertEquals(str.length(), bytes.length);
    for (int i = 0; i < str.length(); ++i) {
      assertEquals("latin1 byte " + i + " differs", (byte) str.charAt(i),
          bytes[i]);
    }
  }

  public static void testGetBytesUtf8() throws UnsupportedEncodingException {
    if (isWasm()) {
      // TODO(b/233695357): Re-enable when EmulatedCharsed is fixed.
      return;
    }

    internalTestGetBytesUtf8("UTF-8");
    internalTestGetBytesUtf8("utf-8");
  }

  private static void internalTestGetBytesUtf8(String encoding)
      throws UnsupportedEncodingException {
    // Test a range of characters getting encoded to UTF8 in 1-2 bytes.
    char[] chars = new char[384];
    for (int i = 0; i < chars.length; ++i) {
      chars[i] = (char) i;
    }
    String str = String.copyValueOf(chars);
    byte[] bytes = str.getBytes(encoding);
    assertEquals(640, bytes.length);
    for (int i = 0; i < 128; ++i) {
      assertEquals("Position " + i, i, bytes[i]);
    }
    for (int i = 128; i < chars.length; ++i) {
      byte first = bytes[2 * i - 128];
      byte second = bytes[2 * i - 127];
      char ch = str.charAt(i);
      assertEquals("byte " + i + " differs", ch,
          ((first & 31) << 6) | (second & 63));
    }

    // non-BMP characters, all take 4 UTF8 bytes.
    int firstCodePoint = 0x100000;
    int numChars = chars.length / 2;
    for (int i = 0; i < numChars; ++i) {
      Character.toChars(firstCodePoint + i, chars, 2 * i);
    }
    str = String.copyValueOf(chars);
    bytes = str.getBytes(encoding);
    assertEquals(4 * numChars, bytes.length);
    for (int i = 0; i < numChars; ++i) {
      assertEquals("1st byte of " + i, (byte) 0xF4, bytes[4 * i]);
      assertEquals("2nd byte of " + i, (byte) 0x80, bytes[4 * i + 1]);
      assertEquals("3rd byte of " + i, (byte) 0x80 + ((i >> 6) & 63),
          bytes[4 * i + 2]);
      assertEquals("4th byte of " + i, (byte) 0x80 + (i & 63),
          bytes[4 * i + 3]);
    }

    // Invalid unicode code point.
    str = "\uFFFF";
    bytes = str.getBytes(encoding);
    assertEquals(3, bytes.length);
    assertEquals((byte) 0xEF, bytes[0]);
    assertEquals((byte) 0xBF, bytes[1]);
    assertEquals((byte) 0xBF, bytes[2]);

    // Invalid surrogate pair -- missing the second codepoint.
    str = "\uD801";
    bytes = str.getBytes(encoding);
    assertEquals(1, bytes.length);
    assertEquals('?', bytes[0]);

    // Invalid surrogate pair -- invalid low surrogate.
    str = "\uD801 ";
    bytes = str.getBytes(encoding);
    assertEquals(2, bytes.length);
    assertEquals('?', bytes[0]);
    assertEquals(' ', bytes[1]);

    // Invalid surrogate pair -- low surrogate without preceding high surrogate.
    str = " \uDDCB";
    bytes = str.getBytes(encoding);
    assertEquals(2, bytes.length);
    assertEquals(' ', bytes[0]);
    assertEquals('?', bytes[1]);

    // Invalid surrogate pair -- low and high surrogate reversed
    str = "\uDDCB\uD83E";
    bytes = str.getBytes(encoding);
    assertEquals(2, bytes.length);
    assertEquals('?', bytes[0]);
    assertEquals('?', bytes[1]);
  }

  /**
   * Tests hashing with strings.
   *
   * The specific strings used in this test used to trigger failures because we
   * use a JavaScript object as a hash map to cache the computed hash codes.
   * This conflicts with built-in properties defined on objects -- see issue
   * #631.
   *
   */
  public void testHashCode() {
    String[] testStrings = {
        "watch", "unwatch", "toString", "toSource", "eval", "valueOf",
        "constructor", "__proto__", "polygenelubricants", "xy", "x", "" };
    int[] javaHashes = {
        112903375, -274141738, -1776922004, -1781441930, 3125404, 231605032,
        -1588406278, 2139739112, Integer.MIN_VALUE, 3841, 120, 0 };

    for (int i = 0; i < testStrings.length; ++i) {
      String testString = testStrings[i];
      int expectedHash = javaHashes[i];

      // verify that the hash codes of these strings match their java
      // counterparts
      assertEquals("Unexpected hash for string " + testString, expectedHash,
          testString.hashCode());

      // Verify that the resulting hash code is numeric (might not be if it is collided with a
      // property).
      assertEquals(expectedHash, Integer.parseInt(Integer.toString(expectedHash)));

      // get hashes again to verify the values are constant for a given string
      assertEquals(expectedHash, testStrings[i].hashCode());
    }
  }

  public static void testHashCodeNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().hashCode();
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testIndexOf() {
    String haystack = "abcdefghi";
    assertEquals(-1, haystack.indexOf("q"));
    assertEquals(-1, haystack.indexOf('q'));
    assertEquals(0, haystack.indexOf("a"));
    assertEquals(0, haystack.indexOf('a'));
    assertEquals(-1, haystack.indexOf('a', 1));
    assertEquals(1, haystack.indexOf("bc"));
    assertEquals(0, haystack.indexOf(""));
  }

  public static void testIndexOfNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().indexOf("");
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testLastIndexOf() {
    String x = "abcdeabcdef";
    assertEquals(9, x.lastIndexOf("e"));
    assertEquals(10, x.lastIndexOf("f"));
    assertEquals(-1, x.lastIndexOf("f", 1));
  }

  public void testLength() {
    String abc = String.valueOf(new char[] {'a', 'b', 'c'});
    assertEquals(3, abc.length());
    String str = "x";
    for (int i = 0; i < 16; i++) {
      str = str + str;
    }
    assertEquals(1 << 16, str.length());
    String cat = String.valueOf(new char[] {'C', '\uD801', '\uDF00', 'T'});
    assertEquals(4, cat.length());
  }

  public void testLowerCase() {
    assertEquals("abc", hideFromCompiler("AbC").toLowerCase());
    assertEquals("abc", hideFromCompiler("abc").toLowerCase());
    assertEquals("", hideFromCompiler("").toLowerCase());

    assertEquals("abc", hideFromCompiler("AbC").toLowerCase(Locale.US));
    assertEquals("abc", hideFromCompiler("abc").toLowerCase(Locale.US));
    assertEquals("", hideFromCompiler("").toLowerCase(Locale.US));

    assertEquals("abc", hideFromCompiler("AbC").toLowerCase(Locale.getDefault()));
    assertEquals("abc", hideFromCompiler("abc").toLowerCase(Locale.getDefault()));
    assertEquals("", hideFromCompiler("").toLowerCase(Locale.getDefault()));
  }

  public void testLowerCaseNonAscii() {
    assertEquals("i̇öçşğü", hideFromCompiler("İÖÇŞĞÜ").toLowerCase()); // a.k.a "Turkey Test"

    // Greek sigma
    assertEquals("\u03C3", hideFromCompiler("\u03A3").toLowerCase());
    assertEquals("abc\u03C2", hideFromCompiler("ABC\u03A3").toLowerCase());
    assertEquals("abc\u03C3abc", hideFromCompiler("ABC\u03A3ABC").toLowerCase());
  }

  public void testMatch() {
    assertFalse("1f", hideFromCompiler("abbbbcd").matches("b*"));
    assertFalse("2f", hideFromCompiler("abbbbcd").matches("b+"));
    assertTrue("3t", hideFromCompiler("abbbbcd").matches("ab*bcd"));
    assertTrue("4t", hideFromCompiler("abbbbcd").matches("ab+cd"));
    assertTrue("5t", hideFromCompiler("abbbbcd").matches("ab+bcd"));
    assertFalse("6f", hideFromCompiler("abbbbcd").matches(""));
    assertTrue("7t", hideFromCompiler("abbbbcd").matches("a.*d"));
    assertFalse("8f", hideFromCompiler("abbbbcd").matches("a.*e"));
    // issue #7736
    assertTrue("9t.1", hideFromCompiler("").matches("(|none)"));
    assertTrue("9t.2", hideFromCompiler("none").matches("(|none)"));
    assertFalse("9f.1", hideFromCompiler("ab").matches("(|none)"));
    assertFalse("9f.2", hideFromCompiler("anoneb").matches("(|none)"));
    assertTrue("10t", hideFromCompiler("none").matches("^(|none)$"));
    assertFalse("10f", hideFromCompiler("abbbbcd").matches("^b*$"));
    assertTrue("11t.1", hideFromCompiler("").matches("|none"));
    assertTrue("11t.2", hideFromCompiler("none").matches("|none"));
    assertFalse("11f.1", hideFromCompiler("ab").matches("|none"));
    assertFalse("11f.2", hideFromCompiler("anoneb").matches("|none"));
    assertTrue("12t", hideFromCompiler("none").matches("^|none$"));
  }

  public void testNull() {
    {
      assertNull(returnNull());
      String a = returnNull() + returnNull();
      assertEquals("nullnull", a);

      String s = returnNull();
      s += returnNull();
      assertEquals("nullnull", s);
    }

    // Same tests allowing the compiler to propagate constants.
    {
      String a = null + (String) null;
      assertEquals("nullnull", a);

      String s = null;
      s += null;
      assertEquals("nullnull", s);
    }
  }

  public void testRegionMatches() {
    String test = String.valueOf(new char[] {'a', 'b', 'c', 'd', 'e', 'f'});
    assertTrue(test.regionMatches(1, "bcd", 0, 3));
    assertTrue(test.regionMatches(1, "bcdx", 0, 3));
    assertFalse(test.regionMatches(1, "bcdx", 0, 4));
    assertFalse(test.regionMatches(1, "bcdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "XAbCd", 1, 4));
    assertTrue(test.regionMatches(true, 1, "BcD", 0, 3));
    assertTrue(test.regionMatches(true, 1, "bCdx", 0, 3));
    assertFalse(test.regionMatches(true, 1, "bCdx", 0, 4));
    assertFalse(test.regionMatches(true, 1, "bCdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "xaBcd", 1, 4));
    test = test.toUpperCase(Locale.ROOT);
    assertTrue(test.regionMatches(true, 0, "XAbCd", 1, 4));
    assertTrue(test.regionMatches(true, 1, "BcD", 0, 3));
    assertTrue(test.regionMatches(true, 1, "bCdx", 0, 3));
    assertFalse(test.regionMatches(true, 1, "bCdx", 0, 4));
    assertFalse(test.regionMatches(true, 1, "bCdx", 1, 3));
    assertTrue(test.regionMatches(true, 0, "xaBcd", 1, 4));

    String empty = String.valueOf(new char[] {});
    assertTrue(empty.regionMatches(true, 0, "", 0, 0));
    assertTrue(empty.regionMatches(true, 0, " ", 0, 0));
    assertFalse(empty.regionMatches(true, 0, " ", 0, 1));

    assertTrue(test.regionMatches(true, 0, "xx", 0, -1));
    assertFalse(test.regionMatches(true, -1, "xx", 0, 0));
    assertFalse(test.regionMatches(true, 0, "xx", -1, 0));
    assertFalse(test.regionMatches(true, -1, "F", 0, 1));
    assertFalse(test.regionMatches(true, -1, "A", 0, 1));

    try {
      test.regionMatches(-1, null, -1, -1);
      if (TestUtils.getJdkVersion() < 21) {
        fail();
      }
    } catch (NullPointerException expected) {
      // NPE must be thrown before any range checks
    }

    try {
      test.regionMatches(true, -1, null, -1, -1);
      if (TestUtils.getJdkVersion() < 11) {
        fail();
      }
    } catch (NullPointerException expected) {
      // NPE must be thrown before any range checks
    }
  }

  public void testReplace() {
    String axax = String.valueOf(new char[] {'a', 'x', 'a', 'x'});
    String aaaa = String.valueOf(new char[] {'a', 'a', 'a', 'a'});
    assertEquals("aaaa", axax.replace('x', 'a'));
    assertEquals("aaaa", aaaa.replace('x', 'a'));
    for (char from = 32; from < 250; ++from) {
      char to = (char) (from + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    for (char to = 32; to < 250; ++to) {
      char from = (char) (to + 5);
      assertEquals(toS(to), toS(from).replace(from, to));
    }
    // issue 1480
    String exampleXd = String.valueOf(new char[] {
        'e', 'x', 'a', 'm', 'p', 'l', 'e', ' ', 'x', 'd'});
    assertEquals("example xd", exampleXd.replace('\r', ' ').replace('\n', ' '));
    String dotFood = String.valueOf(new char[] {'d', 'o', 't', '\u0120', 'f', 'o', 'o', 'd'});
    assertEquals("dot food", dotFood.replace('\u0120', ' '));
    String testStr = String.valueOf(new char[] {
        '\u1111', 'B', '\u1111', 'B', '\u1111', 'B'});
    assertEquals("ABABAB", testStr.replace('\u1111', 'A'));
  }

  public void testReplaceAll() {
    String regex = hideFromCompiler("*[").replaceAll(
        "([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}])", "\\\\$1");
    assertEquals("\\*\\[", regex);
    String replacement = hideFromCompiler("\\").replaceAll("\\\\", "\\\\\\\\").replaceAll(
        "\\$", "\\\\$");
    assertEquals("\\\\", replacement);
    assertEquals("+1", hideFromCompiler("*[1").replaceAll(regex, "+"));
    String x1 = String.valueOf(new char[] {
        'x', 'x', 'x', 'a', 'b', 'c', 'x', 'x', 'd', 'e', 'x', 'f'});
    assertEquals("abcdef", x1.replaceAll("x*", ""));
    String x2 = String.valueOf(new char[] {
        '1', 'a', 'b', 'c', '1', '2', '3', 'd', 'e', '1', '2', '3', '4', 'f'});
    assertEquals("1\\1abc123\\123de1234\\1234f", x2.replaceAll("([1234]+)",
        "$1\\\\$1"));
    String x3 = String.valueOf(new char[] {'x', ' ', ' ', 'x'});
    assertEquals("\n  \n", x3.replaceAll("x", "\n"));
    String x4 = String.valueOf(new char[] {'\n', ' ', ' ', '\n'});
    assertEquals("x  x", x4.replaceAll("\\\n", "x"));
    String x5 = String.valueOf(new char[] {'x'});
    assertEquals("x\"\\", x5.replaceAll("x", "\\x\\\"\\\\"));
    assertEquals("$$x$", x5.replaceAll("(x)", "\\$\\$$1\\$"));
  }

  public void testReplaceString() {
    assertEquals("foobar", hideFromCompiler("bazbar").replace("baz", "foo"));
    assertEquals("$0bar", hideFromCompiler("foobar").replace("foo", "$0"));
    assertEquals("$1bar", hideFromCompiler("foobar").replace("foo", "$1"));
    assertEquals("\\$1bar", hideFromCompiler("foobar").replace("foo", "\\$1"));
    assertEquals("\\1", hideFromCompiler("*[)1").replace("*[)", "\\"));

    // issue 2363
    assertEquals("cb", hideFromCompiler("$ab").replace("$a", "c"));
    assertEquals("cb", hideFromCompiler("^ab").replace("^a", "c"));

    // test JS replacement characters
    assertEquals("a$$b", hideFromCompiler("a[x]b").replace("[x]", "$$"));
    assertEquals("a$1b", hideFromCompiler("a[x]b").replace("[x]", "$1"));
    assertEquals("a$`b", hideFromCompiler("a[x]b").replace("[x]", "$`"));
    assertEquals("a$'b", hideFromCompiler("a[x]b").replace("[x]", "$'"));
  }

  public void testSplit() {
    compareList("fullSplit", new String[] {"abc", "", "", "de", "f"},
        hideFromCompiler("abcxxxdexfxx").split("x"));
    String booAndFoo = hideFromCompiler("boo:and:foo");
    compareList("2:", new String[] {"boo", "and:foo"}, booAndFoo.split(":", 2));
    compareList("5:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        5));
    compareList("-2:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        -2));
    compareList("5o", new String[] {"b", "", ":and:f", "", ""},
        booAndFoo.split("o", 5));
    compareList("-2o", new String[] {"b", "", ":and:f", "", ""},
        booAndFoo.split("o", -2));
    compareList("0o", new String[] {"b", "", ":and:f"}, booAndFoo.split("o", 0));
    compareList("0:", new String[] {"boo", "and", "foo"}, booAndFoo.split(":",
        0));
    // issue 2742
    compareList("issue2742", new String[] {}, hideFromCompiler("/").split("/", 0));

    // Splitting an empty string should result in an array containing a single
    // empty string.
    String[] s = "".split(",");
    assertTrue(s != null);
    assertTrue(s.length == 1);
    assertTrue(s[0] != null);
    assertTrue(s[0].length() == 0);
  }

  public void testSplit_emptyExpr() {
    // TODO(rluble):  implement JDK8 string.split semantics and fix test.
    String[] expected = (TestUtils.getJdkVersion() > 7) ?
        new String[] {"a", "b", "c", "x", "x", "d", "e", "x", "f", "x"} :
        new String[] {"", "a", "b", "c", "x", "x", "d", "e", "x", "f", "x"};
    compareList("emptyRegexSplit", expected, "abcxxdexfx".split(""));
  }

  public void testStartsWith() {
    String haystack = "abcdefghi";
    assertTrue(haystack.startsWith("abc"));
    assertTrue(haystack.startsWith("bc", 1));
    assertTrue(haystack.startsWith(haystack));
    assertFalse(haystack.startsWith(haystack + "j"));
  }

  public void testSubstring() {
    assertEquals("cd", hideFromCompiler("abcdefghi").substring(2, 4));
    assertEquals("bc", hideFromCompiler("abcdef").substring(1, 3));
    assertEquals("bcdef", hideFromCompiler("abcdef").substring(1));
    assertEquals("", hideFromCompiler("abcdef").substring(6));
    assertEquals("", hideFromCompiler("abcdef").substring(6, 6));

    try {
      hideFromCompiler("abc").substring(-1);
      fail("Should have thrown");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      hideFromCompiler("abc").substring(4);
      fail("Should have thrown");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      hideFromCompiler("abc").substring(2, 4);
      fail("Should have thrown");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      hideFromCompiler("abc").substring(2, 1);
      fail("Should have thrown");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testToCharArray() {
    char[] a1 = "abc".toCharArray();
    char[] a2 = new char[] {'a', 'b', 'c'};
    for (int i = 0; i < a1.length; i++) {
      assertEquals(a1[i], a2[i]);
    }

    String original = "\\*\\[";
    assertEquals(original, String.valueOf(original.toCharArray()));
  }

  public void testToString() {
    String str = hideFromCompiler("abc");
    assertSame("str same as str.toString()", str, str.toString());
    // that one is mostly to debug how the code gets compiled to JS.
    assertEquals("concat with str.toString()", "0abcd", "0" + str.toString() + "d");

    // issue 4301
    Object s = hideFromCompiler("abc");
    assertSame("s same as s.toString()", s, s.toString());
  }

  public static void testToStringNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    try {
      returnNull().toString();
      fail();
    } catch (NullPointerException e) {
      // expected
    }
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testTrim() {
    trimRightAssertEquals("abc", "   \t abc \n  ");
    trimRightAssertEquals("abc", "abc");
    trimRightAssertSame("abc", "abc");
    String s = '\u0023' + "hi";
    trimRightAssertSame(s, s);
    trimRightAssertEquals("abc", " abc");
    trimRightAssertEquals("abc", "abc ");
    trimRightAssertEquals("", "");
    trimRightAssertEquals("", "   \t ");

    // Check for removal of nulls; trim treats nulls as if they are whitespace.
    // See issue 8534.

    trimRightAssertEquals("abc", "\0\0\0 abc");
    trimRightAssertEquals("abc", "abc\0\0\0");
    trimRightAssertEquals("abc", "abc   \0\0\0");
    trimRightAssertEquals("abc", "   \0 \0 \0" + "abc");
    trimRightAssertEquals("abc \0 a", "    abc \0 a");
    trimRightAssertEquals("abc \0 a", "    abc \0 a   \0");
    trimRightAssertEquals("abc \0 a", "    abc \0 a   \0   ");
    trimRightAssertEquals("\u0021\u0020abc", "\u0019\u0017\u0021\u0020abc\u0019\u0017\u0018 ");
    trimRightAssertEquals("\u0021 abc",
        "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009" +
        "\n" + "\u000b\u000c" + "\r" + "\u000e\u000f" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019" +
        "\u001A\u001b\u001c\u001d\u001e\u001f\u0020\u0021 " + "abc");

    // JavaScript would trim \u2029 and other unicode whitespace type characters; but Java wont
    trimRightAssertEquals("\u2029abc\u00a0","\u2029abc\u00a0");
  }

  public void testUpperCase() {
    assertEquals("ABC", hideFromCompiler("AbC").toUpperCase());
    assertEquals("ABC", hideFromCompiler("abc").toUpperCase());
    assertEquals("", hideFromCompiler("").toUpperCase());

    assertEquals("ABC", hideFromCompiler("AbC").toUpperCase(Locale.US));
    assertEquals("ABC", hideFromCompiler("abc").toUpperCase(Locale.US));
    assertEquals("", hideFromCompiler("").toUpperCase(Locale.US));

    assertEquals("ABC", hideFromCompiler("AbC").toUpperCase(Locale.getDefault()));
    assertEquals("ABC", hideFromCompiler("abc").toUpperCase(Locale.getDefault()));
    assertEquals("", hideFromCompiler("").toUpperCase(Locale.getDefault()));
  }

  public void testUpperCaseNonAscii() {
    assertEquals("İÖÇŞĞÜ", hideFromCompiler("i̇öçşğü").toUpperCase()); // a.k.a "Turkey Test"

    assertEquals("SS", hideFromCompiler("ß").toUpperCase());
    assertEquals("ʼN", hideFromCompiler("ŉ").toUpperCase());
    assertEquals("ΩΙ", hideFromCompiler("ῼ").toUpperCase());
    assertEquals("ὮΙ", hideFromCompiler("ᾮ").toUpperCase());

    // surrogate example
    assertEquals("\uD801\uDC1c", hideFromCompiler("\uD801\uDC44").toUpperCase());
  }

  /*
   * TODO: needs rewriting to avoid compiler optimizations.
   */
  public void testValueOf() {
    assertTrue(String.valueOf(C.FLOAT_VALUE).startsWith(C.FLOAT_STRING));
    assertEquals(C.INT_STRING, String.valueOf(C.INT_VALUE));
    assertEquals(C.LONG_STRING, String.valueOf(C.LONG_VALUE));
    assertTrue(String.valueOf(C.DOUBLE_VALUE).startsWith(C.DOUBLE_STRING));
    assertEquals(C.CHAR_STRING, String.valueOf(C.CHAR_VALUE));
    assertEquals(C.CHAR_ARRAY_STRING, String.valueOf(C.CHAR_ARRAY_VALUE));
    assertEquals(
        C.CHAR_ARRAY_STRING_SUB, String.valueOf(C.CHAR_ARRAY_VALUE, 1,
        4));
    assertEquals(C.FALSE_STRING, String.valueOf(C.FALSE_VALUE));
    assertEquals(C.TRUE_STRING, String.valueOf(C.TRUE_VALUE));
    assertEquals(C.getLargeCharArrayString(), String.valueOf(C.getLargeCharArrayValue()));
  }

  /**
   * Helper method for testTrim to avoid compiler optimizations.
   *
   * TODO: insufficient, compiler now inlines.
   */
  public void trimRightAssertEquals(String left, String right) {
    assertEquals(left, right.trim());
  }

  /**
   * Helper method for testTrim to avoid compiler optimizations.
   *
   * TODO: insufficient, compiler now inlines.
   */
  public void trimRightAssertSame(String left, String right) {
    assertSame(left, right.trim());
  }

  private static void compareList(String category, String[] desired, String[] got) {
    assertEquals(category + " length", desired.length, got.length);
    for (int i = 0; i < desired.length; i++) {
      assertEquals(category + " " + i, desired[i], got[i]);
    }
  }

  private static <T> T hideFromCompiler(T value) {
    if (Math.random() < -1) {
      // Can never happen, but fools the compiler enough not to optimize this call.
      fail();
    }
    return value;
  }

  private static String returnNull() {
    return hideFromCompiler(null);
  }

  private static String toS(char from) {
    return Character.toString(from);
  }

  private static String utf8Replacement(int times) {
    if (times < 0) {
      throw new IllegalArgumentException();
    }
    // Naive String repeat implementation just using string concatenation to avoid using other
    // String APIs that may be under test.
    String str = "";
    for (int i = 0; i < times; i++) {
      str += "\uFFFD";
    }
    return str;
  }
}
