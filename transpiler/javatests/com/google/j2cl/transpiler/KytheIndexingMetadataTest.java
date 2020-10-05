/*
 * Copyright 2018 Google Inc.
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

import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import java.util.List;
import junit.framework.TestCase;

public class KytheIndexingMetadataTest extends TestCase {
  public void testKytheIndexingMetadata() throws Exception {
    TranspileResult result =
        newTesterWithDefaults()
            .addFile(
                "test/KytheIndexingMetadata.java",
                "package test; public class KytheIndexingMetadata {}")
            .addArgs("-generatekytheindexingmetadata")
            .assertTranspileSucceeds()
            .assertNoWarnings();

    assertLinesContainsKytheMetadata(result.getOutputSource("test/KytheIndexingMetadata.java.js"));
    assertLinesContainsKytheMetadata(
        result.getOutputSource("test/KytheIndexingMetadata.impl.java.js"));
  }

  private void assertLinesContainsKytheMetadata(List<String> lines) {
    assertTrue(lines.size() > 1);
    assertTrue(lines.get(lines.size() - 2).equals("// Kythe Indexing Metadata:"));
    String kytheMetadataLine = lines.get(lines.size() - 1);
    assertTrue(
        kytheMetadataLine.startsWith(
            "// {\"type\":\"kythe0\",\"meta\":[{\"type\":\"anchor_anchor\",\"source_begin\":"));
    assertTrue(
        kytheMetadataLine.contains(
            "\"edge\":\"/kythe/edge/imputes\",\"source_vname\":{\"corpus\":\"google3\",\"path\":"));
    assertTrue(kytheMetadataLine.contains("\"source_end\":"));
    assertTrue(kytheMetadataLine.contains("\"target_begin\":"));
    assertTrue(kytheMetadataLine.contains("\"target_end\":"));
  }
}
