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

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.build.runtime.Runfiles;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/** Tests for {@link J2clMinifier}. */
public class J2clMinifierTest extends TestCase {

  private J2clMinifier minifier;

  public void testAvoidsCollisions() {
    assertChange(
        "this.foo(); this.m_foo__();", //
        "this.foo(); this.foo＿1();");
    assertChange(
        "this.m_bar__java_lang_String(null); this.m_bar__java_lang_Object(null);",
        "this.bar＿1(null); this.bar＿2(null);");

    assertChange("this.m_baz__java_lang_Object();", "this.baz＿1();");
    assertChange("this.m_baz__java_lang_String();", "this.baz＿2();");
    assertChange("this.m_baz__java_lang_Number();", "this.baz＿3();");
  }

  public void testBailOutCases() {
    // Triple underscores at the beginning mess up the mangling pattern. Minifying them would
    // result in an unreadable and illegal identifier of just "＿1". Make sure we leave them alone.
    assertNoChange("m___parseAndValidateInt__java_lang_String__int__int__int");
    assertNoChange("m___");
  }

  public void testBlockComments() {
    // Block comments are stripped.

    assertChange("/* */", "");
    assertChange("alert('hi');/* */", "alert('hi');");
    assertChange("/* */alert('hi');", "alert('hi');");

    assertChange("/** */", "");
    assertChange("/* **/", "");
    assertChange("a/** **/b", "ab");
    assertChange("alert('hi');/** */", "alert('hi');");
    assertChange("/** */alert('hi');", "alert('hi');");

    assertChange("/* *//", "/");
    assertChange("/* */*", "*");
    assertChange("//* */", "//* */");
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
    assertChange("this.m_foo__();", "this.foo＿1();");
    assertChange("this.m_foo__();", "this.foo＿1();");
    assertChange("this.m_foo__();", "this.foo＿1();");
  }

  public void testFields() {
    assertChange("f_someInstanceField__com_google_j2cl_MyClass", "someInstanceField＿1");
    assertChange("$f_someStaticField__com_google_j2cl_MyClass", "someStaticField＿1");
  }

  public void testFindsIdentifiersInContext() {
    assertChange(" m_foo__ ", " foo＿1 ");
    assertChange(".m_foo__(", ".foo＿1(");
    assertChange(".m_foo__;", ".foo＿1;");
    assertChange("{m_foo__}", "{foo＿1}");
    assertChange("(m_foo__)", "(foo＿1)");

    assertChange(" f_bar__com_google_j2cl_MyClass ", " bar＿1 ");
    assertChange(".f_bar__com_google_j2cl_MyClass(", ".bar＿1(");
    assertChange(".f_bar__com_google_j2cl_MyClass;", ".bar＿1;");
    assertChange("{f_bar__com_google_j2cl_MyClass}", "{bar＿1}");
    assertChange("(f_bar__com_google_j2cl_MyClass)", "(bar＿1)");

    assertChange(" $f_baz__com_google_j2cl_MyClass ", " baz＿1 ");
    assertChange(".$f_baz__com_google_j2cl_MyClass(", ".baz＿1(");
    assertChange(".$f_baz__com_google_j2cl_MyClass;", ".baz＿1;");
    assertChange("{$f_baz__com_google_j2cl_MyClass}", "{baz＿1}");
    assertChange("($f_baz__com_google_j2cl_MyClass)", "(baz＿1)");

    assertChange(" $implements__java_util_Map$Entry ", " implements＿1 ");
    assertChange(".$implements__java_util_Map$Entry(", ".implements＿1(");
    assertChange(".$implements__java_util_Map$Entry;", ".implements＿1;");
    assertChange("{$implements__java_util_Map$Entry}", "{implements＿1}");
    assertChange("($implements__java_util_Map$Entry)", "(implements＿1)");
  }

  public void testLineComments() {
    // Line comments are kept.

    assertNoChange("// m_foo__ ");
    assertNoChange("// foo ");
    assertNoChange("// 'foo' ");
    assertNoChange("// 'fo\noo' ");
    assertNoChange("// /* */ ");
    assertNoChange("// /* \n */ ");
    assertNoChange("//# sourceMappingURL=/path/to/file.js.map");

    assertChange("m_foo__// bar \nm_foo__", "foo＿1// bar \nfoo＿1");
    assertChange("/* */// bar \n/* */", "// bar \n");
    assertNoChange("'foo'// bar \n'foo'");
    assertNoChange("alert('hi');// bar \nalert('hi');");

    assertNoChange("//* */");
    assertChange("/ /* */", "/ ");

    // preserves trailing newlines
    assertNoChange("// foo \n");
    assertNoChange("//# sourceMappingURL=/path/to/file.js.map\n");
  }

  public void testMeta() {
    assertChange("$create__", "create＿1");
    assertChange("$ctor__java_lang_String__", "ctor＿1");
    assertChange("$init__java_lang_String__java_lang_Object", "init＿1");
    assertChange("$implements__java_util_Map$Entry", "implements＿1");
  }

  public void testMethods() {
    assertChange("m_foo__", "foo＿1");
    assertChange("m_bar__java_lang_Object", "bar＿1");
    assertChange("m_baz__java_lang_Object__java_lang_String", "baz＿1");
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
  }

  public void testStrings() {
    // Single quoted
    assertNoChange("'/* */'");
    assertNoChange("'this.m_foo__();'");
    assertChange("'' + this.m_foo__(); + ''", "'' + this.foo＿1(); + ''");
    assertNoChange("'this.m_foo__();'");
    assertNoChange("'\\'/* */\\''");

    // Double quoted
    assertNoChange("\"/* */\"");
    assertNoChange("\"this.m_foo__();\"");
    assertChange("\"\" + this.m_foo__(); + \"\"", "\"\" + this.foo＿1(); + \"\"");
    assertNoChange("\"this.m_foo__();\"");
    assertNoChange("\"\\\"/* */\\\"'");

    // Mixed
    assertNoChange("\"'/* */'\"");
    assertNoChange("'\"/* */\"'");
  }

  public void testAbiIntegration_field() throws IOException {
    String readableExampleName = "instancefieldinitializer";
    Map<String, String> expectedMappings = new HashMap<>();
    expectedMappings.put("$create__", "create＿1");
    expectedMappings.put(
        "$ctor__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer__",
        "ctor＿1");
    expectedMappings.put(
        "f_b__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "b＿1");
    expectedMappings.put("$ctor__java_lang_Object__", "ctor＿2");
    expectedMappings.put(
        "f_a__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "a＿1");
    expectedMappings.put(
        "$init__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "init＿1");

    verifyReadableExample(readableExampleName, expectedMappings);
  }

  public void testAbiIntegration_metafunctions() throws IOException {
    String readableExampleName = "implementsgenericinterface";
    Map<String, String> expectedMappings = new HashMap<>();
    expectedMappings.put("$ctor__java_lang_Object__", "ctor＿2");
    expectedMappings.put(
        "$ctor__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericInterfaceGenericImpl__",
        "ctor＿1");
    expectedMappings.put("$create__", "create＿1");
    expectedMappings.put(
        "$init__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericInterfaceImpl",
        "init＿2");
    expectedMappings.put(
        "$ctor__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericInterfaceImpl__",
        "ctor＿3");
    expectedMappings.put(
        "$init__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericInterfaceGenericImpl",
        "init＿1");
    expectedMappings.put(
        "$implements__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericSubInterface",
        "implements＿2");
    expectedMappings.put(
        "$implements__com_google_j2cl_transpiler_readable_implementsgenericinterface_GenericInterface",
        "implements＿1");

    verifyReadableExample(readableExampleName, expectedMappings);
  }

  public void testAbiIntegration_method() throws IOException {
    String readableExampleName = "arithmeticexpressions";
    Map<String, String> expectedMappings = new HashMap<>();
    expectedMappings.put(
        "m_getLongArray___$pp_com_google_j2cl_transpiler_readable_arithmeticexpressions",
        "getLongArray＿1");
    expectedMappings.put("f_MAX_VALUE__java_lang_Long", "MAX_VALUE＿1");
    expectedMappings.put("$create__", "create＿1");
    expectedMappings.put(
        "$init__com_google_j2cl_transpiler_readable_arithmeticexpressions_ArithmeticExpressions",
        "init＿1");
    expectedMappings.put("m_main__", "main＿1");
    expectedMappings.put(
        "$ctor__com_google_j2cl_transpiler_readable_arithmeticexpressions_ArithmeticExpressions__",
        "ctor＿1");
    expectedMappings.put("$ctor__java_lang_Object__", "ctor＿2");
    expectedMappings.put("m_intValue__", "intValue＿1");
    expectedMappings.put("m_valueOf__int", "valueOf＿1");

    verifyReadableExample(readableExampleName, expectedMappings);
  }

  public void testAbiIntegration_staticfield() throws IOException {
    String readableExampleName = "instancefieldinitializer";
    Map<String, String> expectedMappings = new HashMap<>();
    expectedMappings.put(
        "$ctor__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer__",
        "ctor＿1");
    expectedMappings.put("$create__", "create＿1");
    expectedMappings.put(
        "f_b__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "b＿1");
    expectedMappings.put("$ctor__java_lang_Object__", "ctor＿2");
    expectedMappings.put(
        "f_a__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "a＿1");
    expectedMappings.put(
        "$init__com_google_j2cl_transpiler_readable_instancefieldinitializer_InstanceFieldInitializer",
        "init＿1");

    verifyReadableExample(readableExampleName, expectedMappings);
  }

  private void assertChange(String input, String output) {
    assertEquals(output, minifier.minify(input));
  }

  private void assertNoChange(String input) {
    assertEquals(input, minifier.minify(input));
  }

  private void verifyReadableExample(
      String readableExampleName, Map<String, String> expectedMappings) throws IOException {
    File inputDirectory =
        new File(
            Runfiles.getRunfilesDir()
                + "/google3/third_party/java_src/j2cl/transpiler/javatests"
                + "/com/google/j2cl/transpiler/readable/"
                + readableExampleName);

    List<File> jsFiles = Lists.newArrayList(inputDirectory.listFiles());
    Collections.sort(jsFiles);

    for (File jsFile : jsFiles) {
      if (!jsFile.getName().endsWith(".js.txt")) {
        continue;
      }
      minifier.minify(Files.toString(jsFile, StandardCharsets.UTF_8));
    }

    System.out.println("Actual mappings:");
    Map<String, String> actualMappings = minifier.minifiedIdentifiersByIdentifier;
    for (String mangledIdentifier : minifier.minifiedIdentifiersByIdentifier.keySet()) {
      System.out.println(
          "expectedMappings.put(\""
              + mangledIdentifier
              + "\", \""
              + actualMappings.get(mangledIdentifier)
              + "\");");
    }

    assertEquals(expectedMappings, actualMappings);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    minifier = new J2clMinifier();
  }
}
