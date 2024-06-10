/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import com.google.common.truth.Correspondence;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.Problems;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.Assert;

/** Apis for end to end tests on the transpiler. */
public class TranspilerTester {
  /** Creates a new transpiler tester. */
  public static TranspilerTester newTester() {
    return new TranspilerTester();
  }

  /**
   * Creates a new transpiler tester initialized with the defaults (e.g. location for the JRE, etc)
   */
  public static TranspilerTester newTesterWithDefaults() {
    return newTester()
        .setClassPathArg("transpiler/javatests/com/google/j2cl/transpiler/jre_bundle_deploy.jar");
  }

  /** Creates a new transpiler tester initialized with Kotlin defaults. */
  public static TranspilerTester newTesterWithKotlinDefaults() {
    return newTester()
        .addArgs("-frontend", "KOTLIN")
        .addArgs("-kotlincOptions", "-Xmulti-platform")
        // J2CL Koltin frontend is based on Koltin/JVM compiler that requires that deps and the
        // current compilation use the same JVM target in order to inline bytecode. Even we don't
        // use the bytecode inliner, kotlinc fails in the early stage if we do not specify the right
        // JVM target.
        // Note: For Bazel compilation, this is provided through toolchain defaults.
        .addArgs("-kotlincOptions", "-jvm-target=11")
        .setClassPathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/ktstdlib_bundle_deploy.jar");
  }

  /** Creates a new transpiler tester initialized with WASM defaults. */
  public static TranspilerTester newTesterWithWasmDefaults() {
    return newTester()
        .addArgs("-backend", "WASM")
        .setClassPathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/jre_bundle-j2wasm_deploy.jar")
        .addArgs("-defineForWasm", "J2WASM_DEBUG=TRUE")
        .addArgs("-defineForWasm", "jre.strictFpToString=DISABLED")
        .addArgs("-defineForWasm", "jre.checkedMode=ENABLED")
        .addArgs("-defineForWasm", "jre.checks.checkLevel=NORMAL")
        .addArgs("-defineForWasm", "jre.checks.bounds=AUTO")
        .addArgs("-defineForWasm", "jre.checks.api=AUTO")
        .addArgs("-defineForWasm", "jre.checks.numeric=AUTO")
        .addArgs("-defineForWasm", "jre.checks.type=AUTO")
        .addArgs("-defineForWasm", "jre.logging.logLevel=ALL")
        .addArgs("-defineForWasm", "jre.logging.simpleConsoleHandler=ENABLED")
        .addArgs("-defineForWasm", "jre.classMetadata=SIMPLE")
        .addArgs("-defineForWasm", "jre.assertions=ENABLED")
        .addSourcePathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/jre_bundle-j2wasm_deploy-src.jar");
  }

  private abstract static class File {
    private Path filePath;

    public File(Path filePath) {
      this.filePath = filePath;
    }

    public Path getFilePath() {
      return filePath;
    }

    public abstract void createFileIn(Path basePath);

    private boolean isNativeJsFile() {
      return filePath.toString().endsWith(".native.js");
    }

    private boolean isJavaSourceFile() {
      return filePath.toString().endsWith(".java");
    }

    private boolean isKotlinSourceFile() {
      return filePath.toString().endsWith(".kt");
    }

    private boolean isSrcJar() {
      return filePath.toString().endsWith(".srcjar");
    }

    private boolean isZip() {
      return filePath.toString().endsWith(".zip");
    }
  }

  private static class SourceFile extends File {
    private String content;

    public SourceFile(Path filePath, String content) {
      super(filePath);
      this.content = content;
    }

    public String getContent() {
      return content;
    }

    @Override
    public void createFileIn(Path basePath) {
      try {
        Files.createDirectories(
            basePath.resolve(
                getFilePath().getParent() == null ? Paths.get("") : getFilePath().getParent()));
        Files.write(basePath.resolve(getFilePath()), content.getBytes(Charset.forName("UTF-8")));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class ZipFile extends File {
    private List<SourceFile> contents = new ArrayList<>();

    public ZipFile(Path filePath) {
      super(filePath);
    }

    public void addFile(SourceFile file) {
      contents.add(file);
    }

    @Override
    public void createFileIn(Path basePath) {
      try {
        Path zipFilePath = basePath.resolve(getFilePath());

        try (ZipOutputStream out =
            new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
          for (SourceFile nativeSource : contents) {
            out.putNextEntry(new ZipEntry(nativeSource.getFilePath().toString()));
            out.write(nativeSource.getContent().getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Map<Path, File> filesByPath = new LinkedHashMap<>();
  private List<String> args = new ArrayList<>();
  private String temporaryDirectoryPrefix = "transpile_tester";
  private Path outputPath;

  public TranspilerTester addCompilationUnit(String qualifiedCompilationUnitName, String... code) {
    List<String> content = Lists.newArrayList(code);

    String packageName = getPackageName(qualifiedCompilationUnitName);
    if (!packageName.isEmpty()) {
      content.add(0, "package " + packageName + ";");
    }

    return addFileUsingQualifiedName(
        qualifiedCompilationUnitName, ".java", content.toArray(new String[0]));
  }

  public TranspilerTester addKotlinCompilationUnit(
      String qualifiedCompilationUnitName, String... code) {
    List<String> content = Lists.newArrayList(code);

    String packageName = getPackageName(qualifiedCompilationUnitName);
    if (!packageName.isEmpty()) {
      content.add(0, "package " + packageName + "");
    }

    return addFileUsingQualifiedName(
        qualifiedCompilationUnitName, ".kt", content.toArray(new String[0]));
  }

  public TranspilerTester addNativeJsForCompilationUnit(
      String qualifiedCompilationUnitName, String... code) {
    return addFileUsingQualifiedName(qualifiedCompilationUnitName, ".native.js", code);
  }

  private TranspilerTester addFileUsingQualifiedName(
      String qualifiedCompilationUnitName, String ext, String... code) {
    return addFile(
        getPackageRelativePath(qualifiedCompilationUnitName)
            .resolve(getSimpleUnitName(qualifiedCompilationUnitName) + ext),
        code);
  }

  private String getPackageName(String qualifiedCompilationUnitName) {
    int dotIndex = qualifiedCompilationUnitName.lastIndexOf('.');
    if (dotIndex == -1) {
      return "";
    }
    return qualifiedCompilationUnitName.substring(0, dotIndex);
  }

  private Path getPackageRelativePath(String qualifiedCompilationUnitName) {
    return Paths.get(getPackageName(qualifiedCompilationUnitName).replace('.', '/'));
  }

  private String getSimpleUnitName(String qualifiedCompilationUnitName) {
    return qualifiedCompilationUnitName.substring(
        qualifiedCompilationUnitName.lastIndexOf('.') + 1);
  }

  public TranspilerTester addFile(String filename, String... code) {
    return addFile(Paths.get(filename), code);
  }

  public TranspilerTester addFile(Path path, String... code) {
    this.filesByPath.put(path, new SourceFile(path, Joiner.on('\n').join(code)));
    return this;
  }

  public TranspilerTester addFileToZipFile(String zipfilename, String filename, String... code) {
    addFileToZipFile(Paths.get(zipfilename), Paths.get(filename), code);
    return this;
  }

  public TranspilerTester addFileToZipFile(Path zipfilePath, Path filePath, String... code) {
    if (!filesByPath.containsKey(zipfilePath)) {
      filesByPath.put(zipfilePath, new ZipFile(zipfilePath));
    }
    ((ZipFile) filesByPath.get(zipfilePath))
        .addFile(new SourceFile(filePath, Joiner.on('\n').join(code)));
    return this;
  }

  public TranspilerTester setClassPathArg(String path) {
    return this.addArgs("-cp", toTestPath(path));
  }

  public TranspilerTester setNativeSourcePathArg(String path) {
    return this.addArgs("-nativesourcepath", toTestPath(path));
  }

  public TranspilerTester addSourcePathArg(String path) {
    return this.addArgs(toTestPath(path));
  }

  private static String toTestPath(String path) {
    return "" + path;
  }

  public TranspilerTester setArgs(String... args) {
    return setArgs(Arrays.asList(args));
  }

  public TranspilerTester setArgs(Collection<String> args) {
    this.args = new ArrayList<>(args);
    return this;
  }

  @CanIgnoreReturnValue
  public TranspilerTester addArgs(String... args) {
    return addArgs(Arrays.asList(args));
  }

  public TranspilerTester addArgs(Collection<String> args) {
    this.args.addAll(args);
    return this;
  }

  public TranspilerTester setOutputPath(Path outputPath) {
    this.outputPath = outputPath;
    return this;
  }

  public TranspileResult assertTranspileSucceeds() {
    return transpile().assertNoErrors();
  }

  public TranspileResult assertTranspileFails() {
    return transpile().assertHasErrors();
  }

  /** A bundle of data recording the results of a transpile operation. */
  public static class TranspileResult {
    private final Problems problems;
    private final Path outputPath;

    public TranspileResult(Problems problems, Path outputPath) {
      this.problems = problems;
      this.outputPath = outputPath;
    }

    public Problems getProblems() {
      return problems;
    }

    public Path getOutputPath() {
      return outputPath;
    }

    public List<String> getOutputSource(String outputFile) throws IOException {
      Path outputFilePath = outputPath.resolve(outputFile);
      assertThat(outputFilePath.toFile().exists()).isTrue();
      return Files.readAllLines(outputFilePath);
    }

    public TranspileResult assertNoWarnings() {
      return assertWarningsWithoutSourcePosition();
    }

    public TranspileResult assertWarningsWithoutSourcePosition(String... expectedWarnings) {
      assertThat(getProblems().getWarnings())
          .comparingElementsUsing(ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR)
          .containsExactlyElementsIn(Arrays.asList(expectedWarnings));
      return this;
    }

    public TranspileResult assertWarningsWithSourcePosition(String... expectedWarnings) {
      assertThat(getProblems().getWarnings())
          .containsExactlyElementsIn(Arrays.asList(expectedWarnings));
      return this;
    }

    public TranspileResult assertNoErrors() {
      assertThat(getProblems().getErrors()).isEmpty();
      return this;
    }

    public TranspileResult assertHasErrors() {
      assertThat(getProblems().getErrors()).isNotEmpty();
      return this;
    }

    public TranspileResult assertErrorsWithoutSourcePosition(String... expectedErrors) {
      assertThat(getProblems().getErrors())
          .comparingElementsUsing(ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR)
          .containsExactlyElementsIn(Arrays.asList(expectedErrors));
      return this;
    }

    public TranspileResult assertErrorsWithSourcePosition(String... expectedErrors) {
      assertThat(getProblems().getErrors()).containsExactlyElementsIn(expectedErrors);
      return this;
    }

    public TranspileResult assertLastMessage(String expectedMessage) {
      List<String> allMsgs = getProblems().getMessages();
      String lastMessage = Iterables.getLast(allMsgs, "");
      assertThat(lastMessage).contains(expectedMessage);
      return this;
    }

    public TranspileResult assertErrorsContainsSnippets(String... snippets) {
      return assertContainsSnippets(getProblems().getErrors(), snippets);
    }

    public TranspileResult assertInfoMessagesContainsSnippets(String... snippets) {
      return assertContainsSnippets(getProblems().getInfoMessages(), snippets);
    }

    private TranspileResult assertContainsSnippets(List<String> problems, String... snippets) {
      assertThat(problems)
          .comparingElementsUsing(Correspondence.from(String::contains, "contained within"))
          .containsAtLeastElementsIn(Arrays.asList(snippets));
      return this;
    }

    public TranspileResult assertOutputFilesExist(String... fileNames) {
      Arrays.stream(fileNames)
          .forEach(fileName -> Assert.assertTrue(Files.exists(outputPath.resolve(fileName))));
      return this;
    }

    public TranspileResult assertOutputFilesDoNotExist(String... fileNames) {
      Arrays.stream(fileNames)
          .forEach(fileName -> Assert.assertFalse(Files.exists(outputPath.resolve(fileName))));
      return this;
    }

    public TranspileResult assertOutputFilesAreSame(TranspileResult other) throws IOException {
      List<Path> actualPaths =
          ImmutableList.copyOf(MoreFiles.fileTraverser().depthFirstPreOrder(outputPath));
      List<Path> expectedPaths =
          ImmutableList.copyOf(MoreFiles.fileTraverser().depthFirstPreOrder(other.outputPath));

      // Compare simple names.
      assertThat(toFileNames(actualPaths))
          .containsExactlyElementsIn(toFileNames(expectedPaths))
          .inOrder();

      // Compare file contents.
      for (int i = 0; i < expectedPaths.size(); i++) {
        Path expectedPath = expectedPaths.get(i);
        Path actualPath = actualPaths.get(i);
        if (Files.isDirectory(expectedPath)) {
          assertThat(Files.isDirectory(actualPath)).isTrue();
        } else {
          assertThat(Files.readAllLines(actualPath))
              .containsExactlyElementsIn(Files.readAllLines(expectedPath))
              .inOrder();
        }
      }

      return this;
    }

    private static List<Path> toFileNames(List<Path> original) {
      return original.stream().map(Path::getFileName).collect(ImmutableList.toImmutableList());
    }

    private static final Pattern messagePattern =
        Pattern.compile("(?:(?:Error)|(?:Warning))(?::[\\w.]+:\\d+)?: (?<message>.*)");

    private static final Correspondence<String, String> ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR =
        Correspondence.from(TranspileResult::compare, "contained within");

    private static boolean compare(String actual, String expected) {
      Matcher matcher = messagePattern.matcher(actual);
      return matcher.matches()
          // Get just the text of the error without the filename and line number.
          ? matcher.group("message").equals(expected)
          // The error was not emitted with a filename and line number, match the whole message.
          : actual.equals(expected);
    }
  }

  private static TranspileResult invokeTranspiler(List<String> args, Path outputPath) {
    try {
      return new TranspileResult(transpile(args), outputPath);
    } catch (Exception e) {
      e.printStackTrace();
      Problems problems = new Problems();
      problems.error("%s", e.toString());
      return new TranspileResult(problems, outputPath);
    }
  }

  private static Problems transpile(Iterable<String> args) throws Exception {
    // J2clCommandLineRunner.run is hidden since we don't want it to be used as an entry point. As a
    // result we use reflection here to invoke it.
    Method transpileMethod =
        J2clCommandLineRunner.class.getDeclaredMethod("runForTest", String[].class);
    transpileMethod.setAccessible(true);
    return (Problems) transpileMethod.invoke(null, (Object) Iterables.toArray(args, String.class));
  }

  private TranspileResult transpile() {
    try {
      Path tempDir = Files.createTempDirectory(temporaryDirectoryPrefix);

      if (outputPath == null) {
        outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);
      }

      ImmutableList.Builder<String> commandLineArgsBuilder =
          ImmutableList.<String>builder()
              // Output dir
              .add("-d", outputPath.toAbsolutePath().toString());

      if (!filesByPath.isEmpty()) {
        // 1. Create an input directory
        Path inputPath = tempDir.resolve("input");
        Files.createDirectories(inputPath);

        // 2. Create all declared files on disk
        filesByPath.values().forEach(file -> file.createFileIn(inputPath));

        // 3. Add Java source files and srcjar to command line.
        commandLineArgsBuilder.addAll(
            filesByPath.values().stream()
                .filter(
                    Predicates.or(File::isSrcJar, File::isJavaSourceFile, File::isKotlinSourceFile))
                .map(file -> inputPath.resolve(file.getFilePath()))
                .map(Path::toString)
                .collect(toImmutableList()));

        // 4. Add the native sources to the command line.
        List<File> nativeSources =
            filesByPath.values().stream()
                .filter(Predicates.or(File::isNativeJsFile, File::isZip))
                .collect(toImmutableList());
        nativeSources.stream()
            .map(file -> inputPath.resolve(file.getFilePath()))
            .map(Path::toString)
            .forEach(path -> commandLineArgsBuilder.add("-nativesourcepath", path));
      }

      // Passthru explicitly defined args
      commandLineArgsBuilder.addAll(args);

      return invokeTranspiler(commandLineArgsBuilder.build(), outputPath);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private TranspilerTester() {}
}
