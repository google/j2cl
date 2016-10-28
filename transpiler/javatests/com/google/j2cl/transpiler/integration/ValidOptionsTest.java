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
package com.google.j2cl.transpiler.integration;

import java.io.IOException;

/**
 * Tests that valid options do not trigger an error.
 */
public class ValidOptionsTest extends IntegrationTestCase {
  public void testValidOptions() throws IOException {
    TranspileResult transpileResult =
        transpileDirectory("validoptions", OutputType.DIR, "-source", "1.8", "-encoding", "UTF-8");
    assertEquals(0, transpileResult.getExitCode());
    assertFalse(transpileResult.getProblems().hasErrors());
  }
}
