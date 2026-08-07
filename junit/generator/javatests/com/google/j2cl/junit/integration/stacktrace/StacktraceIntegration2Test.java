/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.junit.integration.stacktrace;

import static org.junit.Assume.assumeTrue;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for stack trace deobfuscation */
@RunWith(Parameterized.class)
public class StacktraceIntegration2Test extends IntegrationTestBase {

  @Test
  public void testRecursive() throws Exception {
    runStacktraceTest("RecursiveStacktraceTest");
  }

  @Test
  public void testSimpleThrowingMethod() throws Exception {
    runStacktraceTest("SimpleThrowingStacktraceTest");
  }

  @Test
  public void testThrowsInBridgeMethod() throws Exception {
    runStacktraceTest("ThrowsInBridgeMethod");
  }

  @Test
  public void testThrowsInClassInitializer() throws Exception {
    runStacktraceTest("ThrowsInClassInitializer");
  }

  @Test
  public void testThrowsInConstructor() throws Exception {
    runStacktraceTest("ThrowsInConstructorTest");
  }

  @Test
  public void testThrowsInInstanceInitializer() throws Exception {
    runStacktraceTest("ThrowsInInstanceInitializer");
  }

  @Test
  public void testThrowsInJsConstructor() throws Exception {
    runStacktraceTest("ThrowsInJsConstructorTest");
  }

  @Test
  public void testThrowsInJsFunction() throws Exception {
    runStacktraceTest("ThrowsInJsFunction");
  }

  @Test
  public void testThrowsInJsProperty() throws Exception {
    runStacktraceTest("ThrowsInJsProperty");
  }

  @Test
  public void testThrowsInNativeJs() throws Exception {
    // uses native methods which won't work in Java or Wasm
    assumeTrue(testMode.isJ2cl());

    runStacktraceTest("ThrowsInNativeJs");
  }
}
