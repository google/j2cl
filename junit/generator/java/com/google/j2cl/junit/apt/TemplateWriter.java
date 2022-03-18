/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.junit.apt;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.StandardLocation;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

class TemplateWriter {
  private final List<String> testSummary = new ArrayList<>();
  private final VelocityEngine velocityEngine = J2clTestingVelocityUtil.createEngine();
  private final ErrorReporter errorReporter;
  private final Filer filer;
  private final boolean isJ2wasmTest;

  public TemplateWriter(ErrorReporter errorReporter, Filer filer, boolean isJ2wasmTest) {
    this.errorReporter = errorReporter;
    this.filer = filer;
    this.isJ2wasmTest = isJ2wasmTest;
  }

  public void writeSummary() {
    if (testSummary.isEmpty()) {
      errorReporter.report(ErrorMessage.NO_TEST);
      return;
    }

    try {
      writeResource(
          "test_summary.json",
          testSummary.stream().collect(Collectors.joining("\",\"", "{\"tests\": [\"", "\"]}")));
    } catch (Exception e) {
      errorReporter.report(ErrorMessage.CANNOT_WRITE_RESOURCE, exceptionToString(e));
    }
  }

  public void writeTestClass(TestClass testClass) {
    String testSuiteFileName = testClass.qualifiedName().replace('.', '/');
    testSummary.add(testSuiteFileName + ".js");
    String jsSuite = "com/google/j2cl/junit/apt/JsSuite.vm";
    String jsUnitAdapter = "com/google/j2cl/junit/apt/JsUnitAdapter.vm";
    if (isJ2wasmTest) {
      jsSuite = "com/google/j2cl/junit/apt/WasmJsSuite.vm";
      jsUnitAdapter = "com/google/j2cl/junit/apt/WasmJsUnitAdapter.vm";
    }
    try {
      String mergedJsTemplate = mergeTemplate(testClass, jsSuite);
      writeResource(testSuiteFileName + ".testsuite", mergedJsTemplate);
      String mergedJavaTemplate = mergeTemplate(testClass, jsUnitAdapter);
      writeClass(testClass.jsUnitAdapterQualifiedClassName(), mergedJavaTemplate);
    } catch (Exception e) {
      errorReporter.report(ErrorMessage.CANNOT_WRITE_RESOURCE, exceptionToString(e));
    }
  }

  private String mergeTemplate(TestClass testClass, String template) throws Exception {
    VelocityContext velocityContext = new VelocityContext();
    velocityContext.put("testClass", testClass);
    StringWriter outputBuffer = new StringWriter();

    boolean success =
        velocityEngine.mergeTemplate(
            template, StandardCharsets.UTF_8.name(), velocityContext, outputBuffer);

    if (!success) {
      throw new Exception("Unable to merge velocity template");
    }
    return outputBuffer.toString();
  }

  private void writeResource(String qualifiedName, String content) throws IOException {
    try (Writer writer =
        filer
            .createResource(StandardLocation.CLASS_OUTPUT, "", qualifiedName, new Element[0])
            .openWriter()) {
      writer.write(content);
    }
  }

  private void writeClass(String qualifiedName, String content) throws IOException {
    try (Writer writer = filer.createSourceFile(qualifiedName, new Element[0]).openWriter()) {
      writer.write(content);
    }
  }

  private static String exceptionToString(Exception e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    printWriter.println("Exception: " + e.getMessage());
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
