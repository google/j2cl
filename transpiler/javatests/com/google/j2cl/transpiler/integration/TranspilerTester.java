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
package com.google.j2cl.transpiler.integration;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.truth.Correspondence;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.J2clCommandLineRunner;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    return newTester().setJavaPackage("test").setArgs("-cp", JRE_PATH);
  }

  // The bundle contains both the standard library and its deps so that tests don't have to know how
  // to dep on all.
  private static final String JRE_PATH =
      "third_party/java_src/j2cl/transpiler/javatests/"
          + "com/google/j2cl/transpiler/integration/jre_bundle_deploy.jar";

  private List<File> files = new ArrayList<>();
  private List<String> args = new ArrayList<>();
  private String temporaryDirectoryPrefix = "transpile_tester";
  private String packageName = "";
  private Path outputPath;

  public TranspilerTester addCompilationUnit(String compilationUnitName, String... code) {
    List<String> content = new ArrayList<>(Arrays.asList(code));
    if (packageName != null && !packageName.isEmpty()) {
      content.add(0, "package " + packageName + ";");
    }
    this.files.add(
        new File(
            getPackageRelativePath(compilationUnitName + ".java"), Joiner.on('\n').join(content)));
    return this;
  }

  public TranspilerTester addNativeFile(String compilationUnitName, String... code) {
    if (code.length == 0) {
      code = new String[] {""};
    }
    this.files.add(
        new File(
            getPackageRelativePath(compilationUnitName + ".native.js"),
            Joiner.on('\n').join(code)));
    return this;
  }

  public TranspilerTester addFile(String filename, String content) {
    this.files.add(new File(Paths.get(filename), content));
    return this;
  }

  public TranspilerTester setArgs(String... args) {
    return setArgs(Arrays.asList(args));
  }

  public TranspilerTester setArgs(Collection<String> args) {
    this.args = new ArrayList<>(args);
    return this;
  }

  public TranspilerTester addArgs(String... args) {
    return addArgs(Arrays.asList(args));
  }

  public TranspilerTester addArgs(Collection<String> args) {
    this.args.addAll(args);
    return this;
  }

  public TranspilerTester setJavaPackage(String packageName) {
    this.packageName = packageName;
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
      assertThat(getProblems().getErrors())
          .containsExactlyElementsIn(Arrays.asList(expectedErrors));
      return this;
    }

    public TranspileResult assertLastMessage(String expectedMessage) {
      List<String> allMsgs = getProblems().getMessages();
      String lastMessage = Iterables.getLast(allMsgs, "");
      assertThat(lastMessage).contains(expectedMessage);
      return this;
    }

    public TranspileResult assertErrorsContainsSnippets(String... snippets) {
      assertThat(getProblems().getErrors())
          .comparingElementsUsing(CONTAINS_STRING)
          .containsAllIn(Arrays.asList(snippets));
      return this;
    }

    public TranspileResult assertOutputStreamContainsSnippets(String... snippets) {
      String output =
          J2clUtils.streamToString(stream -> getProblems().reportAndGetExitCode(stream));
      Arrays.stream(snippets)
          .forEach(snippet -> assertThat(output).named("Output").contains(snippet));
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

    private static final Correspondence<String, String> CONTAINS_STRING =
        new Correspondence<String, String>() {
          @Override
          public boolean compare(String actual, String expected) {
            return actual.contains(expected);
          }

          @Override
          public String toString() {
            return "contained within";
          }
        };

    private static final Pattern messagePattern =
        Pattern.compile("(?:(?:Error)|(?:Warning))(?::[\\w.]+:\\d+)?: (?<message>.*)");

    private static final Correspondence<String, String> ERROR_WITHOUT_SOURCE_POSITION_COMPARATOR =
        new Correspondence<String, String>() {
          @Override
          public boolean compare(String actual, String expected) {

            Matcher matcher = messagePattern.matcher(actual);
            checkState(matcher.matches());

            return matcher.group("message").equals(expected);
          }

          @Override
          public String toString() {
            return "contained within";
          }
        };
  }

  private static TranspileResult invokeTranspiler(Iterable<String> args, Path outputPath) {
    try {
      return new TranspileResult(transpile(args), outputPath);
    } catch (Exception e) {
      e.printStackTrace();
      Problems problems = new Problems();
      problems.error(e.toString());
      return new TranspileResult(problems, outputPath);
    }
  }

  private static Problems transpile(Iterable<String> args) throws Exception {
    // J2clCommandLineRunner.run is hidden since we don't want it to be used as an entry point. As a
    // result we use reflection here to invoke it.
    Method transpileMethod = J2clCommandLineRunner.class.getDeclaredMethod("run", String[].class);
    transpileMethod.setAccessible(true);
    return (Problems) transpileMethod.invoke(null, (Object) Iterables.toArray(args, String.class));
  }

  private static class File {
    private Path filePath;
    private String content;

    public File(Path filePath, String content) {
      this.filePath = filePath;
      this.content = content;
    }

    public Path getFilePath() {
      return filePath;
    }

    public void createFileIn(Path basePath) {
      try {
        Files.write(basePath.resolve(filePath), content.getBytes(Charset.forName("UTF-8")));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }

    private boolean isNativeJsFile() {
      return filePath.toString().endsWith(".native.js");
    }

    private boolean isJavaSourceFile() {
      return filePath.toString().endsWith(".java");
    }

    private boolean isSrcJar() {
      return filePath.toString().endsWith(".srcjar");
    }
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

      if (!files.isEmpty()) {
        // 1. Create an input directory
        Path inputPath = tempDir.resolve("input");
        Files.createDirectories(inputPath);

        checkState(!packageName.contains(".") && !packageName.contains("/"));
        Path packagePath = inputPath.resolve(packageName);
        Files.createDirectories(packagePath);

        // 2. Create all declared files on disk
        files.forEach(file -> file.createFileIn(inputPath));

        // 3. Add Java source files and srcjar files to command line.
        commandLineArgsBuilder.addAll(
            files
                .stream()
                .filter(Predicates.or(File::isJavaSourceFile, File::isSrcJar))
                .map(file -> inputPath.resolve(file.getFilePath()))
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(toImmutableList()));

        // 4. Create a source zip containing the native.js sources.
        List<File> nativeSources =
            files.stream().filter(File::isNativeJsFile).collect(toImmutableList());
        if (!nativeSources.isEmpty()) {
          Path nativeZipPath = createNativeZipFile(inputPath, nativeSources, "nativefiles.zip");
          commandLineArgsBuilder.add("-nativesourcepath", nativeZipPath.toString());
        }
      }

      // Passthru explicitly defined args
      commandLineArgsBuilder.addAll(args);

      return invokeTranspiler(commandLineArgsBuilder.build(), outputPath);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private Path createNativeZipFile(Path inputPath, List<File> nativeSources, String outputFileName)
      throws IOException {
    Path zipFilePath = inputPath.resolve(outputFileName);

    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()));
    for (File nativeSource : nativeSources) {
      Path nativeSourceAbsolutePath =
          inputPath.resolve(nativeSource.getFilePath()).toAbsolutePath();
      out.putNextEntry(new ZipEntry(inputPath.relativize(nativeSourceAbsolutePath).toString()));
      Files.copy(nativeSourceAbsolutePath, out);
      out.closeEntry();
    }
    out.close();

    return zipFilePath;
  }

  private Path getPackageRelativePath(String compilationUnitName) {
    Path filePath = Paths.get(compilationUnitName);
    return packageName != null && !packageName.isEmpty()
        ? Paths.get(packageName).resolve(filePath)
        : filePath;
  }

  private TranspilerTester() {}
}
