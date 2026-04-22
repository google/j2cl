/*
 * Copyright 2026 Google Inc.
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

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import junit.framework.TestCase;

/** Tests that annotation processing running in J2CL. */
public class AnnotationProcessingTest extends TestCase {

  private static final String PROCESSOR_JAR =
      TranspilerTester.resolvePathToRunfiles(
              "transpiler/javatests/com/google/j2cl/transpiler/libtest_apt_processor.jar")
          .toString();

  public void testAnnotationProcessing() throws Exception {
    newTesterWithDefaults()
        .addCompilationUnit(
            "bar.Foo",
            """
            import com.google.j2cl.transpiler.TestAnnotation;
            @TestAnnotation
            public class Foo {}
            """)
        .addArgs("-cp", PROCESSOR_JAR)
        .addJavacOptions(
            "-processorpath", PROCESSOR_JAR, "-processor", TestAptProcessor.class.getName())
        .assertTranspileSucceeds()
        .assertOutputFilesExist(
            "bar/Foo.java",
            "bar/Foo.impl.java.js",
            "bar/GeneratedFoo.java",
            "bar/GeneratedFoo.java.js",
            "bar/GeneratedFoo.impl.java.js");
  }

  public void testAnnotationProcessing_kytheSmokeTest() throws Exception {
    // Make sure running annotation processing does not crash when generating kythe metadata.
    // This is a smoke test only, it does not verify that the kythe metadata is correct.
    newTesterWithDefaults()
        .addCompilationUnit(
            "bar.Foo",
            """
            import com.google.j2cl.transpiler.TestAnnotation;
            @TestAnnotation
            public class Foo {}
            """)
        .addArgs("-generatekytheindexingmetadata")
        .addArgs("-cp", PROCESSOR_JAR)
        .addJavacOptions(
            "-processorpath", PROCESSOR_JAR, "-processor", TestAptProcessor.class.getName())
        .assertTranspileSucceeds();
  }
}
