/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.minifier;

import junit.framework.TestCase;

/** Tests for {@link J2clMinifier}. */
public class J2clMinifierTest extends TestCase {

  private J2clMinifier minifier;

  public void testAvoidsCollisions() {
    assertChange(
        "this.foo(); this.m_foo__();", //
        "this.foo(); this.foo_$1();");
    assertChange(
        "this.m_bar__java_lang_String(null); this.m_bar__java_lang_Object(null);",
        "this.bar_$1(null); this.bar_$2(null);");

    assertChange("this.m_baz__java_lang_Object();", "this.baz_$1();");
    assertChange("this.m_baz__java_lang_String();", "this.baz_$2();");
    assertChange("this.m_baz__java_lang_Number();", "this.baz_$3();");
  }

  public void testBailOutCases() {
    // Triple underscores at the beginning mess up the mangling pattern. Minifying them would
    // result in an unreadable and illegal identifier of just "_$1". Make sure we leave them alone.
    assertNoChange("m___parseAndValidateInt__java_lang_String__int__int__int");
    assertNoChange("m___");
    assertNoChange("m__parseAndValidate");
    assertNoChange("m__x");
    assertNoChange("m_____x");
    assertNoChange("thism_baz__java_lang_Number();");
    assertNoChange("ClassEndingInLowercasem__InnerClass");
    // TODO(b/109721646): Uncomment once the regex literals are handled correctly.
    // assertNoChange("/m_baz__java_lang_Number/");
  }

  public void testBlockComments() {
    // Block comments are stripped.

    assertChange("/* */", "");
    assertChange("alert('hi');/* */", "alert('hi');");
    assertChange("/* */alert('hi');", "alert('hi');");

    assertChange("/* *//* */", "");
    assertChange("''/* */", "''");

    assertChange("/** */", "");
    assertChange("/* **/", "");
    assertChange("a/** **/b", "ab");
    assertChange("alert('hi');/** */", "alert('hi');");
    assertChange("/** */alert('hi');", "alert('hi');");

    assertChange("/* *//", "/");
    assertChange("/* */*", "*");
    assertChange("//* */", "");
    assertChange("*/* */", "*");

    assertChange("/*/* */*/", "*/");

    assertChange("/* m_foo__ */", "");
    assertChange("/* 'foo' */", "");
    assertChange("/* // */", "");

    // preserves newlines
    assertChange("/*\n*/", "\n");
    assertChange("/**\n*/", "\n");
  }

  public void testConsistency() {
    assertChange("this.m_foo__();", "this.foo_$1();");
    assertChange("this.m_foo__();", "this.foo_$1();");
    assertChange("this.m_foo__();", "this.foo_$1();");
  }

  public void testFields() {
    assertChange("f_someInstanceField__com_google_j2cl_MyClass", "someInstanceField_$1");
    assertChange("$static_someStaticField__com_google_j2cl_MyClass", "someStaticField_$1");
    assertChange("$ordinal_ordinalField__com_google_j2cl_MyClass", "ordinalField_$1");
    assertChange("$captured_capturedField__com_google_j2cl_MyClass", "capturedField_$1");
  }

  public void testFindsIdentifiersInContext() {
    assertChange(" m_foo__ ", " foo_$1 ");
    assertChange(".m_foo__(", ".foo_$1(");
    assertChange(".m_foo__;", ".foo_$1;");
    assertChange("{m_foo__}", "{foo_$1}");
    assertChange("(m_foo__)", "(foo_$1)");

    assertChange(" f_bar__com_google_j2cl_MyClass ", " bar_$1 ");
    assertChange(".f_bar__com_google_j2cl_MyClass(", ".bar_$1(");
    assertChange(".f_bar__com_google_j2cl_MyClass;", ".bar_$1;");
    assertChange("{f_bar__com_google_j2cl_MyClass}", "{bar_$1}");
    assertChange("(f_bar__com_google_j2cl_MyClass)", "(bar_$1)");

    assertChange(" $static_baz__com_google_j2cl_MyClass ", " baz_$1 ");
    assertChange(".$static_baz__com_google_j2cl_MyClass(", ".baz_$1(");
    assertChange(".$static_baz__com_google_j2cl_MyClass;", ".baz_$1;");
    assertChange("{$static_baz__com_google_j2cl_MyClass}", "{baz_$1}");
    assertChange("($static_baz__com_google_j2cl_MyClass)", "(baz_$1)");

    assertChange(" $implements__java_util_Map$Entry ", " implements_$1 ");
    assertChange(".$implements__java_util_Map$Entry(", ".implements_$1(");
    assertChange(".$implements__java_util_Map$Entry;", ".implements_$1;");
    assertChange("{$implements__java_util_Map$Entry}", "{implements_$1}");
    assertChange("($implements__java_util_Map$Entry)", "(implements_$1)");

    assertChange("abc.$static_baz__com_google_j2cl_MyClass.klm", "abc.baz_$1.klm");
  }

  public void testLineComments() {
    assertChange("// m_foo__ ", "");
    assertChange("// foo ", "");
    assertChange("// 'foo' ", "");
    assertChange("// 'fo\noo' ", "\noo' ");
    assertChange("// /* */ ", "");
    assertChange("// /* \n */ ", "\n */ ");
    assertNoChange("//# sourceMappingURL=/path/to/file.js.map");

    assertChange("m_foo__// bar \nm_foo__", "foo_$1\nfoo_$1");
    assertChange("/* */// bar \n/* */", "\n");
    assertChange("'foo'// bar \n'foo'", "'foo'\n'foo'");
    assertChange("alert('hi');// bar \nalert('hi');", "alert('hi');\nalert('hi');");

    assertChange("//* */", "");
    assertChange("/ /* */", "/ ");

    // preserves trailing newlines
    assertChange("// foo \n", "\n");
    assertNoChange("//# sourceMappingURL=/path/to/file.js.map\n");

    // Division
    assertChange("1/f_bar__java_lang_Number;", "1/bar_$1;");
  }

  public void testMeta() {
    assertChange("$create__", "create_$1");
    assertChange("$ctor__java_lang_String__", "ctor_$1");
    assertChange("$init__java_lang_String__java_lang_Object", "init_$1");
    assertChange("$implements__java_util_Map$Entry", "implements_$1");
  }

  public void testMethods() {
    assertChange("m_foo__", "foo_$1");
    assertChange("m_bar__java_lang_Object", "bar_$1");
    assertChange("m_baz__java_lang_Object__java_lang_String", "baz_$1");
  }

  public void testWhiteSpace() {
    assertChange(" \n", "\n");
    assertChange("  \n  \n", "\n\n");
    assertChange("m_bar__java_lang_Object \n", "bar_$1\n");
    assertChange("  /* */\n", "\n");
    assertChange("  //abc\n", "\n");
    assertChange("x = {};   \n", "x = {};\n");
    assertChange("if (a) {    \n", "if (a) {\n");
    assertChange("\"     \"    \n", "\"     \"\n");

    assertNoChange("  this.call();");
  }

  public void testForwardDeclare() {
    assertChange("let Byte = goog.forwardDeclare('java.lang.Byte$impl');", "let Byte;");
    assertChange("let Byte = goog.forwardDeclare(\"java.lang.Byte$impl\");", "let Byte;");
    assertChange("var Foo = goog.forwardDeclare('java.lang.Foo');", "var Foo;");
    assertChange("var $Foo = goog.forwardDeclare('java.lang.Foo');", "var $Foo;");
    assertChange("\nlet Byte = goog.forwardDeclare('java.lang.Byte$impl');\n", "\nlet Byte;\n");
    assertChange(
        "let Byte = goog.forwardDeclare(\"java.lang.Byte$impl\");\n"
            + "var Foo = goog.forwardDeclare('java.lang.Foo');",
        "let Byte;\nvar Foo;");

    assertNoChange("goog.forwardDeclare('java.lang.Foo');");
    assertNoChange("var Foo = goog.forwardDeclare('java.lang.Foo')");
    assertNoChange("identvar Foo = goog.forwardDeclare('java.lang.Foo');");
    assertNoChange("\"let Byte = goog.forwardDeclare('java.lang.Byte$impl');\"");
  }

  public void testGoogRequire() {
    assertChange("goog.require('java.lang.Foo');", "");
    assertChange("goog.require('java.lang.Foo$1_');", "");
    assertChange("\ngoog.require('java.lang.Foo');\n", "\n\n");
    assertChange("goog.require('java.lang.Foo');\ngoog.require('java.lang.Bar');", "\n");

    assertNoChange("let Byte = goog.require('java.lang.Byte');");
    assertNoChange("var Foo = goog.require('java.lang.Byte');");
    assertNoChange("goog.require('java.lang.Foo')");
    assertNoChange("identgoog.require('java.lang.Foo');");
    assertNoChange("\"goog.require('java.lang.Foo');\"");
  }

  public void testNoChanges() {
    assertNoChange("foo");
    assertNoChange("m_foo");
    assertNoChange("f_foo");
    assertNoChange("m_");
    assertNoChange("f_");
    assertNoChange("m_foo_");
    assertNoChange("f_foo_");
    assertNoChange("$outer_this");
    assertNoChange("$$REPLACER_FOR_ESCAPE_JS_STRING__AND__ESCAPE_JS_REGEX_");
    assertNoChange("meConstants__");
    assertNoChange("fsCloseBox__soy3");
    assertNoChange("menuCss__soy3");
    assertNoChange("this.bar;");
    assertNoChange("for(;this.bar;)");
  }

  public void testStrings() {
    // Single quoted
    assertNoChange("'/* */'");
    assertNoChange("'this.m_foo__();'");
    assertChange("'' + this.m_foo__(); + ''", "'' + this.foo_$1(); + ''");
    assertNoChange("'this.m_foo__();'");
    assertChange("'m_foo__'", "'foo_$1'");
    assertNoChange("'This is \\'m_foo__'");
    // We don't handle string identifiers in complicated scenarios. See related comment in minifier.
    assertNoChange("1/'m_foo__'.length()");

    assertNoChange("'\\'/* */\\''");

    // Double quoted
    assertNoChange("\"/* */\"");
    assertNoChange("\"this.m_foo__();\"");
    assertChange("\"\" + this.m_foo__(); + \"\"", "\"\" + this.foo_$1(); + \"\"");
    assertNoChange("\"this.m_foo__();\"");
    assertNoChange("\"\\\"/* */\\\"'");
    // Identifiers in double quotes are not replaced. J2CL only generates single quote.
    assertNoChange("\"m_foo__\"");

    // Mixed
    assertNoChange("\"'/* */'\"");
    assertNoChange("'\"/* */\"'");
  }

  public void testExample() {
    assertChange(
        String.join(
            "\n",
            "constructor(fn) {",
            "  $LambdaAdaptor.$clinit();",
            "  super();",
            "  /**@type{number}*/",
            "  this.m_boo__;",
            "  this.$ctor__hoo__(fn);",
            "}"),
        String.join(
            "\n",
            "constructor(fn) {",
            "  $LambdaAdaptor.$clinit();",
            "  super();",
            "",
            "  this.boo_$1;",
            "  this.ctor_$1(fn);",
            "}"));
  }

  private void assertChange(String input, String output) {
    assertEquals(output, minifier.minify(input));
  }

  private void assertNoChange(String input) {
    assertEquals(input, minifier.minify(input));
  }
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    minifier = new J2clMinifier();
  }
}
