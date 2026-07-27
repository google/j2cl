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
package com.google.j2cl.tools.sourcemapcomposer;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.debugging.sourcemap.SourceMapConsumerFactory;
import com.google.debugging.sourcemap.SourceMapConsumerV3;
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SourceMapComposerTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  // KT map: maps Foo.kt (line 1, col 1) -> Foo.java (line 10 [index 9="S"], col 5 [index 4="I"],
  // symbol "myVar")
  private static final String KT_MAP_CONTENT =
      """
      {
        "version": 3,
        "file": "Foo.kt",
        "lineCount": 1,
        "mappings": "AASIA",
        "sources": ["Foo.java"],
        "names": ["myVar"]
      }
      """;

  // JS map helper: builds JS map JSON targeting specified js file and kt source file
  private static String jsMapContent(String jsFile, String ktSource) {
    return """
    {
      "version": 3,
      "file": "%s",
      "lineCount": 1,
      "mappings": "AAAAA",
      "sources": ["%s"],
      "names": ["myVarKt"]
    }
    """
        .formatted(jsFile, ktSource);
  }

  private static final String JS_MAP_CONTENT = jsMapContent("Foo.js", "Foo.kt");

  private Path intermediateMapRoot;
  private Path targetSourceRoot;
  private Path outputPath;
  private Problems problems;
  private Output output;

  @Before
  public void setUp() throws Exception {
    intermediateMapRoot = tempFolder.newFolder("target.kt.map").toPath();
    targetSourceRoot = tempFolder.newFolder("target.js.map").toPath();
    outputPath = tempFolder.newFolder("out").toPath();
    problems = new Problems();
    output = OutputUtils.initOutput(outputPath, tempFolder.getRoot().toPath(), problems);
  }

  @Test
  public void composeJ2ktSourceMaps_success() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_missingIntermediate() throws Exception {
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);

    compose(intermediateMapRoot, targetSourceRoot);

    Path resultJsMap = outputPath.resolve("com/foo/Foo.js.map");
    SourceMapConsumerV3 consumer =
        (SourceMapConsumerV3) SourceMapConsumerFactory.parse(Files.readString(resultJsMap));
    OriginalMapping mapping = consumer.getMappingForLine(1, 1);

    assertThat(problems.hasErrors()).isFalse();
    assertThat(Files.exists(resultJsMap)).isTrue();
    assertThat(mapping).isNotNull();
    assertThat(mapping.getOriginalFile()).isEqualTo("Foo.kt");
    assertThat(mapping.getLineNumber()).isEqualTo(1);
    assertThat(mapping.getColumnPosition()).isEqualTo(1);
    assertThat(mapping.getIdentifier()).isEqualTo("myVarKt");
  }

  @Test
  public void composeJ2ktSourceMaps_innerClass() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Foo$Inner.js.map", jsMapContent("Foo$Inner.js", "Foo.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo$Inner.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_companionSupplier() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(
        targetSourceRoot,
        "com/foo/FooCompanionSupplier.js.map",
        jsMapContent("FooCompanionSupplier.js", "Foo.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/FooCompanionSupplier.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_deeplyNestedAndAnonymousInnerClass() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(
        targetSourceRoot,
        "com/foo/Foo$Inner1$Inner2.js.map",
        jsMapContent("Foo$Inner1$Inner2.js", "Foo.kt"));
    writeFile(targetSourceRoot, "com/foo/Foo$1.js.map", jsMapContent("Foo$1.js", "Foo.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo$Inner1$Inner2.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo$1.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_dollarSignInDirectoryPath() throws Exception {
    writeFile(intermediateMapRoot, "com/foo$bar/Baz.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo$bar/Baz.js.map", jsMapContent("Baz.js", "Baz.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo$bar/Baz.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_dotInDirectoryPath() throws Exception {
    writeFile(intermediateMapRoot, "com/foo.bar/Baz.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo.bar/Baz.js.map", jsMapContent("Baz.js", "Baz.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo.bar/Baz.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_directories() throws Exception {
    writeFile(intermediateMapRoot, "sourcemap/SourceMap.kt.map", KT_MAP_CONTENT);
    writeFile(
        targetSourceRoot,
        "sourcemap/SourceMap.js.map",
        jsMapContent("SourceMap.js", "SourceMap.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "sourcemap/SourceMap.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_directoryInput() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Bar.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Bar.js.map", jsMapContent("Bar.js", "Bar.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Bar.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_mixedSyntheticAndMappedFiles() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);
    writeFile(
        targetSourceRoot,
        "com/foo/TestRunner.js.map",
        jsMapContent("TestRunner.js", "TestRunner.js"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/TestRunner.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "TestRunner.js",
        /* expectedLine= */ 1,
        /* expectedCol= */ 1,
        /* expectedIdentifier= */ "myVarKt");
  }

  @Test
  public void composeJ2ktSourceMaps_semanticMappingResolution_hasIdentifier() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @Test
  public void composeJ2ktSourceMaps_semanticMappingResolution_fallbackSymbolName()
      throws Exception {
    // KT map without identifier mapping ("names": [])
    String ktMapNoIdentifierContent =
        """
        {
          "version": 3,
          "file": "Foo.kt",
          "lineCount": 1,
          "mappings": "AAAA",
          "sources": ["Foo.java"],
          "names": []
        }
        """;

    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", ktMapNoIdentifierContent);
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 1,
        /* expectedCol= */ 1,
        /* expectedIdentifier= */ "myVarKt");
  }

  @Test
  public void composeJ2ktSourceMaps_invalidSourceMap() throws Exception {
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", "{ invalid json ... ");
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", JS_MAP_CONTENT);

    assertThrows(Problems.Exit.class, () -> compose(intermediateMapRoot, targetSourceRoot));

    assertThat(problems.hasErrors()).isTrue();
  }

  @Test
  public void composeJ2ktSourceMaps_duplicateSimpleNamesAcrossPackages() throws Exception {
    String ktMapPkg1 =
        """
        {
          "version": 3,
          "file": "Widget.kt",
          "lineCount": 1,
          "mappings": "AASIA",
          "sources": ["Widget.java"],
          "names": ["varPkg1"]
        }
        """;
    String ktMapPkg2 =
        """
        {
          "version": 3,
          "file": "Widget.kt",
          "lineCount": 1,
          "mappings": "AASIA",
          "sources": ["Widget.java"],
          "names": ["varPkg2"]
        }
        """;

    writeFile(intermediateMapRoot, "com/pkg1/Widget.kt.map", ktMapPkg1);
    writeFile(intermediateMapRoot, "com/pkg2/Widget.kt.map", ktMapPkg2);

    writeFile(targetSourceRoot, "com/pkg1/Widget.js.map", jsMapContent("Widget.js", "Widget.kt"));
    writeFile(targetSourceRoot, "com/pkg2/Widget.js.map", jsMapContent("Widget.js", "Widget.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/pkg1/Widget.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Widget.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "varPkg1");
    assertMapping(
        /* relativeJsMapPath= */ "com/pkg2/Widget.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Widget.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "varPkg2");
  }

  @Test
  public void composeJ2ktSourceMaps_persistsUncomposedMappings() throws Exception {
    String multiSourceJsMapContent =
        """
        {
          "version": 3,
          "file": "Foo.js",
          "lineCount": 2,
          "mappings": "AAAAA;ACAAC",
          "sources": ["Foo.kt", "SyntheticRuntime.js"],
          "names": ["myVar", "runtimeVar"]
        }
        """;

    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "com/foo/Foo.js.map", multiSourceJsMapContent);

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
    assertMapping(
        /* relativeJsMapPath= */ "com/foo/Foo.js.map",
        /* line= */ 2,
        /* col= */ 1,
        /* expectedOriginalFile= */ "SyntheticRuntime.js",
        /* expectedLine= */ 1,
        /* expectedCol= */ 1,
        /* expectedIdentifier= */ "runtimeVar");
  }

  @Test
  public void composeJ2ktSourceMaps_dropsUnmappedIntermediatePositions() throws Exception {
    // Intermediate map (Foo.kt.map) only maps Foo.kt line 5 -> Foo.java line 10.
    // Line 1 of Foo.kt is unmapped and has no preceding mapping.
    String ktMapLine5Only =
        """
        {
          "version": 3,
          "file": "Foo.kt",
          "lineCount": 5,
          "mappings": ";;;;AASIA",
          "sources": ["Foo.java"],
          "names": ["myVar"]
        }
        """;
    writeFile(intermediateMapRoot, "com/foo/Foo.kt.map", ktMapLine5Only);

    // Target map (Foo.js.map) maps Foo.js line 1 -> Foo.kt line 1.
    String targetJsMapLine1 =
        """
        {
          "version": 3,
          "file": "Foo.js",
          "lineCount": 1,
          "mappings": "AAAAA",
          "sources": ["Foo.kt"],
          "names": ["jsVar"]
        }
        """;

    writeFile(targetSourceRoot, "com/foo/Foo.js.map", targetJsMapLine1);

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    Path resultJsMap = outputPath.resolve("com/foo/Foo.js.map");
    assertThat(Files.exists(resultJsMap)).isTrue();
    SourceMapConsumerV3 consumer =
        (SourceMapConsumerV3) SourceMapConsumerFactory.parse(Files.readString(resultJsMap));
    OriginalMapping mapping = consumer.getMappingForLine(1, 1);
    assertThat(mapping).isNull();
  }

  @Test
  public void composeJ2ktSourceMaps_originalPathWithPrefix() throws Exception {
    writeFile(intermediateMapRoot, "project/src/java/com/foo/Foo.kt.map", KT_MAP_CONTENT);
    writeFile(targetSourceRoot, "project/src/java/com/foo/Foo.js.map", JS_MAP_CONTENT);
    writeFile(intermediateMapRoot, "project/src/super/com/bar/Bar.kt.map", KT_MAP_CONTENT);
    writeFile(
        targetSourceRoot, "project/src/super/com/bar/Bar.js.map", jsMapContent("Bar.js", "Bar.kt"));

    compose(intermediateMapRoot, targetSourceRoot);

    assertThat(problems.getErrors()).isEmpty();
    assertMapping(
        /* relativeJsMapPath= */ "project/src/java/com/foo/Foo.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
    assertMapping(
        /* relativeJsMapPath= */ "project/src/super/com/bar/Bar.js.map",
        /* line= */ 1,
        /* col= */ 1,
        /* expectedOriginalFile= */ "Foo.java",
        /* expectedLine= */ 10,
        /* expectedCol= */ 5,
        /* expectedIdentifier= */ "myVar");
  }

  @CanIgnoreReturnValue
  private Path writeFile(Path root, String relativePath, String content) throws Exception {
    Path file = root.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
    return file;
  }

  private void compose(Path intermediateMapDir, Path targetSourceDir) {
    SourceMapComposer.composeSourceMaps(
        getFiles(intermediateMapDir), getFiles(targetSourceDir), output, problems);
    output.close();
  }

  private static ImmutableList<FileInfo> getFiles(Path dir) {
    return SourceUtils.getAllSources(dir)
        .filter(f -> Files.isRegularFile(Path.of(f.sourcePath())))
        .collect(toImmutableList());
  }

  private void assertMapping(
      String relativeJsMapPath,
      int line,
      int col,
      String expectedOriginalFile,
      int expectedLine,
      int expectedCol,
      String expectedIdentifier)
      throws Exception {
    Path resultJsMap = outputPath.resolve(relativeJsMapPath);
    String composedContent = Files.readString(resultJsMap);
    SourceMapConsumerV3 consumer =
        (SourceMapConsumerV3) SourceMapConsumerFactory.parse(composedContent);
    OriginalMapping mapping = consumer.getMappingForLine(line, col);

    assertThat(Files.exists(resultJsMap)).isTrue();
    assertThat(mapping).isNotNull();
    assertThat(mapping.getOriginalFile()).isEqualTo(expectedOriginalFile);
    assertThat(mapping.getLineNumber()).isEqualTo(expectedLine);
    assertThat(mapping.getColumnPosition()).isEqualTo(expectedCol);
    assertThat(mapping.getIdentifier()).isEqualTo(expectedIdentifier);
  }
}
