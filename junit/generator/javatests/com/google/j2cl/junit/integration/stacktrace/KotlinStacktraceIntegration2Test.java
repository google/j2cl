/*
 * Copyright 2024 Google Inc.
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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import com.google.j2cl.junit.integration.IntegrationTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration test for stack trace deobfuscation */
@RunWith(Parameterized.class)
public class KotlinStacktraceIntegration2Test extends IntegrationTestBase {

  @Before
  public void assumeNonJ2wasm() {
    // TODO(b/223396796): J2wasm does not support stacktrace test.
    assumeFalse(testMode.isJ2wasm());
  }

  @Test
  public void testKotlinNative() throws Exception {
    // test contains native js code and can't be run in pure Java or Wasm,
    // this condition overlaps init but it is acceptable
    assumeTrue(testMode.isJ2cl());

    runStacktraceTest("KotlinNativeStacktraceTest");
  }

  @Test
  public void testKotlinThrowsInNativeJs() throws Exception {
    // uses native methods which won't work in Java or Wasm
    assumeTrue(testMode.isJ2cl());

    runStacktraceTest("KotlinThrowsInNativeJs");
  }

  @Test
  public void testKotlinThrowsInJsFunction() throws Exception {
    runStacktraceTest("KotlinThrowsInJsFunction");
  }

  @Test
  public void testKotlinThrowsInJsProperty() throws Exception {
    runStacktraceTest("KotlinThrowsInJsProperty");
  }

  @Test
  public void testKotlinExceptionWithCause() throws Exception {
    runStacktraceTest("KotlinExceptionWithCauseStacktraceTest");
  }

  @Test
  public void testKotlinDuplicateFileName() throws Exception {
    runStacktraceTest("KotlinDuplicateFileNameStacktraceTest");
  }
}
