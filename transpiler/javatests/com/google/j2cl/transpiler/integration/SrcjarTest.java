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

/** Tests srcjar IO errors. */
public class SrcjarTest extends IntegrationTestCase {
  public void testMissingSrcjar() throws IOException {
    TranspileResult transpileResult =
        transpileDirectory("missingsrcjar", OutputType.DIR, "/foo/bar/Missing.srcjar");
    assertErrorsContainsSnippet(transpileResult.getProblems(), "File not found");
  }

  public void testCorruptSrcjar() throws IOException {
    TranspileResult transpileResult = transpileDirectory("corruptsrcjar", OutputType.DIR);
    assertErrorsContainsSnippet(transpileResult.getProblems(), "Cannot extract zip");
  }
}
