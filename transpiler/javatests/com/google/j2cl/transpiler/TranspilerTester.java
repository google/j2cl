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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.MoreFiles;
import com.google.common.truth.Correspondence;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.Problems;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

  /** Creates a new transpiler tester initialized with Kotlin (frontend) defaults. */
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
        .addArgs("-kotlincOptions", "-language-version=2.1")
        .setClassPathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/ktstdlib_bundle_deploy.jar");
  }

  /** Creates a new transpiler tester initialized with Kotlin (backend) defaults. */
  public static TranspilerTester newTesterWithJ2ktDefaults() {
    return newTester()
        .addArgs("-backend", "KOTLIN")
        .setClassPathArg(
            "transpiler/javatests/com/google/j2cl/transpiler/jre_bundle-j2kt_deploy.jar");
  }

  /** Creates a new transpiler tester initialized with WASM defaults. */
  public static TranspilerTester newTesterWithWasmDefaults() {
    return newTester()
        // TODO(b/395921769): Remove this after the test are ported to modular WASM.
        .noAssertDelayedCancelChecks()
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
  private boolean assertDelayedCancelChecks = true;

  @CanIgnoreReturnValue
  public TranspilerTester addCompilationUnit(String qualifiedCompilationUnitName, String code) {
    String packageName = getPackageName(qualifiedCompilationUnitName);
    if (!packageName.isEmpty()) {
      code = "package " + packageName + ";\n" + code;
    }
    return addFileUsingQualifiedName(qualifiedCompilationUnitName, ".java", code);
  }

  @CanIgnoreReturnValue
  public TranspilerTester addKotlinCompilationUnit(
      String qualifiedCompilationUnitName, String code) {
    String packageName = getPackageName(qualifiedCompilationUnitName);
    if (!packageName.isEmpty()) {
      code = "package " + packageName + "\n" + code;
    }

    return addFileUsingQualifiedName(qualifiedCompilationUnitName, ".kt", code);
  }

  @CanIgnoreReturnValue
  public TranspilerTester addNativeJsForCompilationUnit(
      String qualifiedCompilationUnitName, String code) {
    return addFileUsingQualifiedName(qualifiedCompilationUnitName, ".native.js", code);
  }

  @CanIgnoreReturnValue
  private TranspilerTester addFileUsingQualifiedName(
      String qualifiedCompilationUnitName, String ext, String code) {
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

  @CanIgnoreReturnValue
  public TranspilerTester addFile(String filename, String code) {
    return addFile(Paths.get(filename), code);
  }

  @CanIgnoreReturnValue
  public TranspilerTester addFile(Path path, String code) {
    this.filesByPath.put(path, new SourceFile(path, code));
    return this;
  }

  @CanIgnoreReturnValue
  public TranspilerTester addFileToZipFile(String zipfilename, String filename, String code) {
    addFileToZipFile(Paths.get(zipfilename), Paths.get(filename), code);
    return this;
  }

  @CanIgnoreReturnValue
  public TranspilerTester addFileToZipFile(Path zipfilePath, Path filePath, String code) {
    if (!filesByPath.containsKey(zipfilePath)) {
      filesByPath.put(zipfilePath, new ZipFile(zipfilePath));
    }
    ((ZipFile) filesByPath.get(zipfilePath)).addFile(new SourceFile(filePath, code));
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
    return resolvePathToRunfiles(path).toString();
  }

  public static Path resolvePathToRunfiles(String path) {
    try {
      return Paths.get(
          com.google.devtools.build.runfiles.Runfiles.create().rlocation("j2cl/" + path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  @CanIgnoreReturnValue
  public TranspilerTester noAssertDelayedCancelChecks() {
    this.assertDelayedCancelChecks = false;
    return this;
  }

  public TranspilerTester addNullMarkPackageInfo(String pkg) {
    var unused =
        addCompilationUnit(
            "org.jspecify.annotations.NullMarked", "public @interface NullMarked {}");
    return addFile(
        Path.of(pkg.replace('.', '/'), "package-info.java"),
        """
        @org.jspecify.annotations.NullMarked
        package %s;
        """
            .formatted(pkg));
  }

  public TranspileResult assertTranspileSucceeds() {
    return transpile().assertNoErrors();
  }

  public TranspileResult assertTranspileFails() {
    return transpile().assertHasErrors();
  }

  public void assertTranspileWithCancellation(int cancelDelayMs) throws IOException {
    noAssertDelayedCancelChecks(); // Not compatible.
    var result = transpile(cancelDelayMs);
    result.assertNoErrors();
    assertThat(MoreFiles.listFiles(result.getOutputPath())).isEmpty();
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

    @CanIgnoreReturnValue
    public TranspileResult assertNoWarnings() {
      return assertWarningsWithoutSourcePosition();
    }

    @CanIgnoreReturnValue
    public TranspileResult assertWarningsWithoutSourcePosition(String... expectedWarnings) {
      assertThat(getProblems().getWarnings())
          .comparingElementsUsing(ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR)
          .containsExactlyElementsIn(Arrays.asList(expectedWarnings));
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertWarningsWithSourcePosition(String... expectedWarnings) {
      assertThat(getProblems().getWarnings())
          .containsExactlyElementsIn(Arrays.asList(expectedWarnings));
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertNoErrors() {
      assertThat(getProblems().getErrors()).isEmpty();
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertHasErrors() {
      assertThat(getProblems().getErrors()).isNotEmpty();
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertErrorsWithoutSourcePosition(String... expectedErrors) {
      assertThat(getProblems().getErrors())
          .comparingElementsUsing(ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR)
          .containsExactlyElementsIn(Arrays.asList(expectedErrors));
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertErrorsWithSourcePosition(String... expectedErrors) {
      assertThat(getProblems().getErrors()).containsExactlyElementsIn(expectedErrors);
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertLastMessage(String expectedMessage) {
      List<String> allMsgs = getProblems().getMessages();
      String lastMessage = Iterables.getLast(allMsgs, "");
      assertThat(lastMessage).contains(expectedMessage);
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertErrorsContainsSnippets(String... snippets) {
      return assertContainsSnippets(getProblems().getErrors(), snippets);
    }

    @CanIgnoreReturnValue
    public TranspileResult assertInfoMessagesContainsSnippets(String... snippets) {
      return assertContainsSnippets(getProblems().getInfoMessages(), snippets);
    }

    @CanIgnoreReturnValue
    private TranspileResult assertContainsSnippets(List<String> problems, String... snippets) {
      assertThat(problems)
          .comparingElementsUsing(Correspondence.from(String::contains, "contained within"))
          .containsAtLeastElementsIn(Arrays.asList(snippets));
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertOutputFilesExist(String... fileNames) {
      Arrays.stream(fileNames)
          .forEach(fileName -> Assert.assertTrue(Files.exists(outputPath.resolve(fileName))));
      return this;
    }

    @CanIgnoreReturnValue
    public TranspileResult assertOutputFilesDoNotExist(String... fileNames) {
      Arrays.stream(fileNames)
          .forEach(fileName -> Assert.assertFalse(Files.exists(outputPath.resolve(fileName))));
      return this;
    }

    @CanIgnoreReturnValue
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

  /** Do not automatically cancel the transpiler. */
  private static final int NO_CANCEL = -1;

  /**
   * The maximum delay (in ms) that we allow for calls to #isCancelled(). Note that we have a much
   * lower delay but this number should be high enough to cover random GC events.
   */
  private static final int MAX_DELAY = 250;

  @SuppressWarnings("FutureReturnValueIgnored")
  private static Problems transpile(
      ImmutableList<String> args, boolean assertDelayedCancelChecks, int cancelDelayMs) {
    List<String> delayedCalls = new ArrayList<>();
    List<String> slightlyDelayedCalls = new ArrayList<>();
    Problems problems =
        assertDelayedCancelChecks
            ? new Problems() {
              long lastCall = System.nanoTime();

              @Override
              public boolean isCancelled() {
                long delay = (System.nanoTime() - lastCall) / 1_000_000;
                if (delay > MAX_DELAY) {
                  var stackTrace =
                      Throwables.getStackTraceAsString(new Throwable("Delay: " + delay));
                  if (delay > MAX_DELAY * 2) {
                    delayedCalls.add(stackTrace);
                  } else {
                    slightlyDelayedCalls.add(stackTrace);
                  }
                }
                lastCall = System.nanoTime();
                return false;
              }
            }
            : new Problems();

    J2clCommandLineRunner runner = new J2clCommandLineRunner(problems);
    // Make sure a new thread is spawned to run the transpiler for Thread locals. See
    // J2clCommandLineRunner.run for the details.
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    try {
      executorService.execute(() -> runner.executeForTesting(args));
      if (cancelDelayMs != NO_CANCEL) {
        executorService.schedule(problems::requestCancellation, cancelDelayMs, MILLISECONDS);
      }
    } finally {
      MoreExecutors.shutdownAndAwaitTermination(executorService, 60, SECONDS);
    }
    assertThat(Thread.currentThread().isInterrupted()).isFalse();

    final String[] knownDelayedCalls = {
      "com.google.j2cl.transpiler.frontend.javac.JavacParser.parseFiles",
      // Kotlin frontend is currently missing a lot of checks in between large chunks of works.
      "com.google.j2cl.transpiler.frontend.kotlin.KotlinParser.parseFiles",
    };
    for (String knownDelayedCall : knownDelayedCalls) {
      delayedCalls.removeIf(t -> t.contains(knownDelayedCall));
      // Remove from slightly delayed calls as well since they are a subset.
      slightlyDelayedCalls.removeIf(t -> t.contains(knownDelayedCall));
    }
    assertThat(delayedCalls).isEmpty();

    final String[] knownSlightlyDelayedCalls = {
      // Jdt is slow to do the check and we can't do much about it.
      "org.eclipse.core.runtime.SubMonitor.isCanceled",
    };
    for (String knownSlightlyDelayedCall : knownSlightlyDelayedCalls) {
      slightlyDelayedCalls.removeIf(t -> t.contains(knownSlightlyDelayedCall));
    }
    assertThat(slightlyDelayedCalls).isEmpty();

    return problems;
  }

  private TranspileResult transpile() {
    return transpile(NO_CANCEL);
  }

  private TranspileResult transpile(int cancelDelayMs) {
    try {
      Path tempDir = Files.createTempDirectory(temporaryDirectoryPrefix);

      if (outputPath == null) {
        outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);
      }

      ImmutableList.Builder<String> commandLineArgsBuilder =
          ImmutableList.<String>builder()
              // Output dir
              .add("-d", outputPath.toAbsolutePath().toString())
              .add("-libraryinfooutput", Files.createTempFile("libraryinfo", ".bin").toString());

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

      return new TranspileResult(
          transpile(commandLineArgsBuilder.build(), assertDelayedCancelChecks, cancelDelayMs),
          outputPath);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private TranspilerTester() {}
}
