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
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTester;
import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;
import junit.framework.TestCase;

/** End to end test for command line invocations. */
public class CommandLineInvocationTest extends TestCase {
  public void testHelpFlag() {
    newTesterWithDefaults()
        .setArgs("-help")
        .assertTranspileSucceeds()
        // Just a smoke test to verify that we printing some flags with description
        .assertInfoMessagesContainsSnippets("-classpath")
        .assertInfoMessagesContainsSnippets("Specifies where to find");
  }

  public void testInvalidFlag() {
    newTesterWithDefaults()
        .setArgs("-unknown", "abc")
        .assertTranspileFails()
        .assertErrorsContainsSnippets(
            "\"-unknown\" is not a valid option\n"
                + "Usage: j2cl <options> <source files>\n"
                + "use -help for a list of possible options");
  }

  public void testFrontendFlag() throws IOException {
    newTesterWithDefaults()
        .addArgs("-frontend", "llama")
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("\"llama\" is not a valid value for \"-frontend");

    newTesterWithDefaults()
        .addArgs("-frontend", "jdt")
        .setOutputPath(Files.createTempFile("output", ".zip"))
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileSucceeds();
  }

  public void testSyntaxError() {
    newTesterWithDefaults()
        .addCompilationUnit("SyntaxError", "public class SyntaxError {", "  =", "}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("Syntax error on token \"=\"");
  }

  public void testInvalidOutputLocation() throws IOException {
    // Output to a location that is not a directory and is not a zip file.
    Path outputLocation = Files.createTempFile("foo", ".txt");
    newTesterWithDefaults()
        .addCompilationUnit("Foo", "public class Foo {}")
        .setOutputPath(outputLocation)
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Output location '" + outputLocation + "' must be a directory or .zip file.");
  }

  public void testMissingJreDependency() {
    // Create a clean tester so that the JRE dependency is not automatically added.
    newTester()
        .addCompilationUnit("EmptyClass", "public class EmptyClass {}")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("The type java.lang.Object cannot be resolved.");
  }

  public void testMissingSrcjar() {
    newTesterWithDefaults()
        .addArgs("/foo/bar/Missing.srcjar")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("File '/foo/bar/Missing.srcjar' not found.");
  }

  public void testCorruptSrcjar() {
    newTesterWithDefaults()
        .addFile("corrupt.srcjar", "This is not what an srcjar/zip file should contain.")
        .assertTranspileFails()
        .assertErrorsContainsSnippets("Cannot extract zip")
        .assertErrorsContainsSnippets("/corrupt.srcjar");
  }

  public void testNativeJsInSameDir() {
    Path sourcesPath = Paths.get("deep/package");
    newTesterWithDefaults()
        .addFile(
            sourcesPath.resolve("NativeClass.java"),
            "package random;",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFile(
            sourcesPath.resolve("NativeClass.native.js"),
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  public void testNativeJsInDifferentSourceJar() {
    newTesterWithDefaults()
        .addFileToZipFile(
            "sources.srcjar",
            "src/random/NativeClass.java",
            "package random;",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "src/random/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  // TODO(b/158564515): Enable when the bug is fixed.
  public void disabled_testNativeJsInSameDirSourceInDifferentSourceJar() {
    newTesterWithDefaults()
        .addFileToZipFile(
            "sources.srcjar",
            "src/random/NativeClass.java",
            "package random;",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFile(
            "src/random/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  public void testNativeJsInPackageRelativePath() {
    Path sourcesPath = Paths.get("deep/package");
    newTesterWithDefaults()
        .addFile(
            sourcesPath.resolve("NativeClass.java"),
            "package random;",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFile(
            "java/random/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  public void testNativeJsIsFullyQualified() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "@JsType(name = \"SomethingElse\")",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "  public static class InnerClass {",
            "    @JsMethod public native void nativeInstanceMethod();",
            "  }",
            "}")
        .addFile(
            "this/can/be/anywhere/nativeclasstest.NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .addFile(
            "somewhere/else/nativeclasstest.NativeClass$InnerClass.native.js",
            "InnerClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  public void testNoMatchingSource() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/BadNameNativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Unused native file 'nativeclasstest/BadNameNativeClass.native.js'.");
  }

  public void testNativeJsFileForJsEnum() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.ClosureEnum",
            "import jsinterop.annotations.*;",
            "@JsEnum",
            "public enum ClosureEnum{",
            "  OK,",
            "  CANCEL",
            "}")
        .addNativeJsForCompilationUnit(
            "nativeclasstest.ClosureEnum", "const ClosureEnum ={ OK : 'OK', CANCEL : 'Cancel' }")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'ClosureEnum' does not support having a '.native.js' file.");
  }

  public void testNativeJsFileForNativeJsEnum() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.ClosureEnum",
            "import jsinterop.annotations.*;",
            "@JsEnum(isNative=true)",
            "public enum ClosureEnum{",
            "  OK,",
            "  CANCEL",
            "}")
        .addNativeJsForCompilationUnit(
            "nativeclasstest.ClosureEnum", "const ClosureEnum ={ OK : 'OK', CANCEL : 'Cancel' }")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "JsEnum 'ClosureEnum' does not support having a '.native.js' file.");
  }

  public void testNativeJsFileForNativeJsType() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "@JsType(isNative=true)",
            "public class NativeClass{",
            "}")
        .addNativeJsForCompilationUnit("nativeclasstest.NativeClass", "Class NativeClass{}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Native JsType 'NativeClass' does not support having a '.native.js' file.");
  }

  public void testUnusedSource() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/ExtraClass.native.js",
            "ExtraClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Unused native file 'nativeclasstest/ExtraClass.native.js'.");
  }

  public void testMultipleNativeSourceMatches() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/nativeclasstest.NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Unused native file 'nativeclasstest/NativeClass.native.js'.");
  }

  public void testMultipleNativeSourcesWithSameName() {
    newTesterWithDefaults()
        .addCompilationUnit(
            "nativeclasstest.NativeClass",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void nativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/NativeClass.native.js",
            "NativeClass.prototype.nativeInstanceMethod = function () {}")
        .addCompilationUnit(
            "nativeclasstest.subpackage.NativeClass",
            "import jsinterop.annotations.*;",
            "public class NativeClass {",
            "  @JsMethod public native void otherNativeInstanceMethod();",
            "}")
        .addFileToZipFile(
            "native.zip",
            "nativeclasstest/subpackage/NativeClass.native.js",
            "NativeClass.prototype.otherNativeInstanceMethod = function () {}")
        .assertTranspileSucceeds();
  }

  public void testOutputsToDirectory() throws IOException {
    newTesterWithDefaults()
        .setOutputPath(Files.createTempDirectory("outputdir"))
        .addCompilationUnit("test.Foo", "public class Foo {", "  public class InnerFoo {}", "}")
        .addCompilationUnit("test.Bar", "public class Bar {", "  public class InnerBar {}", "}")
        .assertTranspileSucceeds()
        .assertOutputFilesExist(
            "test/Foo.java.js",
            "test/Foo.impl.java.js",
            "test/Bar.java.js",
            "test/Bar.impl.java.js")
        .assertOutputFilesDoNotExist("some/thing/Bogus.js");

    // Test transpilation of java file without java package
    newTesterWithDefaults()
        .setOutputPath(Files.createTempDirectory("outputdir"))
        .addCompilationUnit("Foo", "public class Foo {}")
        .assertTranspileSucceeds()
        .assertOutputFilesExist("Foo.java.js", "Foo.impl.java.js");
  }

  public void testOutputsToZipFile() throws IOException {
    Path outputLocation = Files.createTempFile("output", ".zip");
    newTesterWithDefaults()
        .setOutputPath(outputLocation)
        .addCompilationUnit("test.Foo", "public class Foo {", "  public class InnerFoo {}", "}")
        .addCompilationUnit("test.Bar", "public class Bar {", "  public class InnerBar {}", "}")
        .assertTranspileSucceeds();

    try (ZipFile zipFile = new ZipFile(outputLocation.toFile())) {
      assertNotNull(zipFile.getEntry("test/Foo.java.js"));
      assertNotNull(zipFile.getEntry("test/Foo.impl.java.js"));
      assertNotNull(zipFile.getEntry("test/Bar.java.js"));
      assertNotNull(zipFile.getEntry("test/Bar.impl.java.js"));
      assertNull(zipFile.getEntry("some/thing/Bogus.js"));
    }
  }

  public void testForbiddenAnnotations() {
    newTesterWithDefaults()
        .addArgs("-forbiddenAnnotation", "GwtIncompatible")
        .addCompilationUnit(
            "annotation.GwtIncompatible",
            "import java.lang.annotation.*;",
            "@Retention(RetentionPolicy.CLASS)",
            "@Target({ElementType.METHOD})",
            "@interface GwtIncompatible {}")
        .addCompilationUnit(
            "annotation.ClassWithForbiddenAnnotation",
            "import jsinterop.annotations.*;",
            "public class ClassWithForbiddenAnnotation {",
            "  @GwtIncompatible public  void nativeInstanceMethod() {}",
            "}")
        .assertTranspileFails()
        .assertErrorsWithoutSourcePosition(
            "Unexpected @GwtIncompatible annotation found. Please run this library through the"
                + " incompatible annotated code stripper tool.");

    newTesterWithDefaults()
        .addArgs("-forbiddenAnnotation", "Foo")
        .addCompilationUnit(
            "annotation.GwtIncompatible",
            "import java.lang.annotation.*;",
            "@Retention(RetentionPolicy.CLASS)",
            "@Target({ElementType.METHOD})",
            "@interface GwtIncompatible {}")
        .addCompilationUnit(
            "annotation.ClassWithForbiddenAnnotation",
            "import jsinterop.annotations.*;",
            "public class ClassWithForbiddenAnnotation {",
            "  @GwtIncompatible public void nativeInstanceMethod() {}",
            "}")
        .assertTranspileSucceeds();
  }
}
