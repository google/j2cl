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

import static com.google.common.truth.Truth.assertThat;
import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class KytheIndexingMetadataTest {

  private static final Pattern PATH_PATTERN = Pattern.compile("\"path\":\"([^\"]+)\"");

  @Test
  public void testKytheIndexingMetadata() throws Exception {
    TranspileResult result =
        newTesterWithDefaults()
            .addFile(
                "test/KytheIndexingMetadata.java",
                """
                package test;
                public class KytheIndexingMetadata {}
                """)
            .addArgs("-generatekytheindexingmetadata")
            .assertTranspileSucceeds()
            .assertNoWarnings();

    assertKytheMetadata(result.getOutputSource("test/KytheIndexingMetadata.java.js"));

    String kytheMetadata =
        assertKytheMetadata(result.getOutputSource("test/KytheIndexingMetadata.impl.java.js"));

    // Ensures there's an anchor on the name of the class declaration.
    assertImputesEdgeExists(
        kytheMetadata,
        /* sourceBegin= */ 27,
        /* sourceEnd= */ 48,
        /* targetBegin= */ 171,
        /* targetEnd= */ 192);
  }

  @Test
  public void testKytheIndexingMetadata_multibyteChars() throws Exception {
    TranspileResult result =
        newTesterWithDefaults()
            .addFile(
                "test/KytheIndexingMetadata.java",
                """
                package test;
                // °
                public class KytheIndexingMetadata {}
                """)
            .addArgs("-generatekytheindexingmetadata")
            .assertTranspileSucceeds()
            .assertNoWarnings();

    String kytheMetadata =
        assertKytheMetadata(result.getOutputSource("test/KytheIndexingMetadata.impl.java.js"));

    // Ensures there's an anchor on the name of the class declaration.
    assertImputesEdgeExists(
        kytheMetadata,
        // TODO(b/505491718): This source range is off by one due the earlier ° character occupying
        // two bytes in the source file.
        /* sourceBegin= */ 32,
        /* sourceEnd= */ 53,
        /* targetBegin= */ 171,
        /* targetEnd= */ 192);
  }

  @CanIgnoreReturnValue
  private static String assertKytheMetadata(List<String> lines) {
    assertThat(lines.size()).isGreaterThan(1);
    assertThat(lines.get(lines.size() - 2)).isEqualTo("// Kythe Indexing Metadata:");
    String kytheMetadataLine = lines.getLast();
    assertThat(kytheMetadataLine).startsWith("// {\"type\":\"KYTHE0\"");
    // return the JSON string without the "// " prefix.
    String kytheMedataJson = kytheMetadataLine.substring(3);
    // Snip out paths as they are not stable across different runs/evironments.
    return PATH_PATTERN.matcher(kytheMedataJson).replaceAll("\"path\":\"<snip>\"");
  }

  private static void assertImputesEdgeExists(
      String actualMetadata, int sourceBegin, int sourceEnd, int targetBegin, int targetEnd) {
    String expectedAnchor =
        String.format(
            """
            {\
            "type":"ANCHOR_ANCHOR",\
            "source_begin":%d,\
            "source_end":%d,\
            "target_begin":%d,\
            "target_end":%d,\
            "edge":"/kythe/edge/imputes",\
            "source_vname":{"corpus":"google3","path":"<snip>"}}\
            """,
            sourceBegin, sourceEnd, targetBegin, targetEnd);
    assertThat(actualMetadata).contains(expectedAnchor);
  }
}
