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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.build.runtime.Runfiles;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.errors.Problems;
import com.google.j2cl.transpiler.J2clTranspiler;
import com.google.j2cl.transpiler.J2clTranspilerDriver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

/**
 * Base class for integration tests.
 * <p>
 * Provides utilities for running the transpiler (with or without a target directory of Java source)
 * and making assertions about the std and err log output.
 */
public class IntegrationTestCase extends TestCase {

  private static final String TEST_ROOT = "third_party/java_src/j2cl/transpiler/javatests";

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
  }

  protected static void assertErrorsContainsSnippet(Problems problems, String snippet) {
    assertTrue(
        "Expected to find snippet '"
            + snippet
            + "' but did not find it in:\n"
            + problems.getErrors()
            + "\n",
        problems.getErrors().stream().anyMatch(error -> error.contains(snippet)));
  }

  protected static void assertOutputContainsSnippet(Problems problems, String snippet) {
    String output = J2clUtils.streamToString(stream -> problems.report(stream, stream));
    assertTrue(
        "Expected to find snippet '" + snippet + "' but did not find it in:\n" + output + "\n",
        output.contains(snippet));
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

    J2clTranspiler.Result result = J2clTranspilerDriver.transpile(args);
    return new TranspileResult(result.getExitCode(), result.getProblems(), outputLocation);
  }
}
