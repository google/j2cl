/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.truth.Correspondence;
import com.google.devtools.build.runtime.Runfiles;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.problems.Problems;
import com.google.j2cl.transpiler.J2clTranspiler;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import junit.framework.TestCase;

/**
 * Base class for integration tests.
 * <p>
 * Provides utilities for running the transpiler (with or without a target directory of Java source)
 * and making assertions about the std and err log output.
 */
public class IntegrationTestCase extends TestCase {

  private static final String TEST_ROOT = "third_party/java_src/j2cl/transpiler/javatests";

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

  // The bundle contains both the standard library and its deps so that tests don't have to know how
  // to dep on all.
  protected static final String JRE_PATH =
      TEST_ROOT + "/com/google/j2cl/transpiler/integration/jre_bundle_deploy.jar";

  public enum OutputType {
    DIR,
    ZIP
  }

  /**
   * A bundle of data recording the results of a transpile operation.
   */
  public static class TranspileResult {
    private final int exitCode;
    private final Problems problems;
    private final File outputLocation;

    public TranspileResult(int exitCode, Problems problems, File outputLocation) {
      this.exitCode = exitCode;
      this.problems = problems;
      this.outputLocation = outputLocation;
    }

    public int getExitCode() {
      return exitCode;
    }

    public Problems getProblems() {
      return problems;
    }

    public File getOutputLocation() {
      return outputLocation;
    }

    public void assertCompileFails(String... expectedErrors) throws Exception {
      assertThat(getProblems().getErrors())
          .comparingElementsUsing(CONTAINS_STRING)
          .containsExactlyElementsIn(Arrays.asList(expectedErrors));
    }

    public void assertCompileSucceeds() throws Exception {
      assertThat(getProblems().getErrors()).isEmpty();
    }
  }

  protected static void assertErrorsContainsSnippet(Problems problems, String snippet) {
    assertThat(problems.getErrors()).comparingElementsUsing(CONTAINS_STRING).contains(snippet);
  }

  protected static void assertOutputContainsSnippet(Problems problems, String snippet) {
    String output = J2clUtils.streamToString(stream -> problems.report(stream, stream));
    assertThat(output).named("Output").contains(snippet);
  }

  protected static String[] getTranspileArgs(
      File outputLocation, Class<?> testClass, String inputDirectoryName, String... extraArgs) {
    List<String> argList = new ArrayList<>();

    // Output dir
    argList.add("-d");
    argList.add(outputLocation.getAbsolutePath());

    // Input source
    for (File sourceFile : listSourceFilesInDir(testClass, inputDirectoryName)) {
      argList.add(sourceFile.getAbsolutePath());
    }

    Collections.addAll(argList, extraArgs);

    return Iterables.toArray(argList, String.class);
  }

  private static List<File> listExtensionFilesInDir(
      String extension, Class<?> clazz, String directoryName) {
    List<File> filesInDir = listFilesInDir(clazz, directoryName);
    List<File> extensionFilesInDir = new ArrayList<>();
    for (File fileInDir : filesInDir) {
      if (fileInDir.getName().endsWith(extension)) {
        extensionFilesInDir.add(fileInDir);
      }
    }
    return extensionFilesInDir;
  }

  private static List<File> listFilesInDir(Class<?> clazz, String directoryName) {
    return Lists.newArrayList(
        Runfiles.packageRelativeLocation(TEST_ROOT, clazz, directoryName).listFiles());
  }

  private static List<File> listSourceFilesInDir(Class<?> clazz, String directoryName) {
    List<File> sourceFiles = new ArrayList<>();
    sourceFiles.addAll(listExtensionFilesInDir(".java", clazz, directoryName));
    sourceFiles.addAll(listExtensionFilesInDir(".srcjar", clazz, directoryName));
    return sourceFiles;
  }

  protected TranspileResult transpileDirectory(
      String directoryName, OutputType outputType, String... extraArgs) throws IOException {
    File outputLocation = Files.createTempDir();
    if (outputType == OutputType.ZIP) {
      outputLocation = new File(outputLocation, "output.zip");
    }
    String[] transpileArgs =
        IntegrationTestCase.getTranspileArgs(
            outputLocation, this.getClass(), directoryName, extraArgs);
    return transpile(transpileArgs, outputLocation);
  }

  protected TranspileResult transpile(String[] args) {
    return transpile(args, null);
  }

  protected TranspileResult transpile(String[] args, File outputLocation) {
    try {
      // Run the transpiler in its own thread
      J2clTranspiler.Result result =
          Executors.newSingleThreadExecutor().submit(() -> invokeTranspile(args)).get();
      return new TranspileResult(result.getExitCode(), result.getProblems(), outputLocation);
    } catch (Exception e) {
      e.printStackTrace();
      Problems problems = new Problems();
      problems.error(e.toString());
      return new TranspileResult(-3, problems, outputLocation);
    }
  }

  private static J2clTranspiler.Result invokeTranspile(Object args) throws Exception {
    // J2clTranspiler.transpile is hidden since we don't want it to be used as an entry point. As a
    // result we use reflection here to invoke it.
    Method transpileMethod = J2clTranspiler.class.getDeclaredMethod("transpile", String[].class);
    transpileMethod.setAccessible(true);
    return (J2clTranspiler.Result) transpileMethod.invoke(new J2clTranspiler(), args);
  }
}
