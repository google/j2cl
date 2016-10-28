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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.build.runtime.Runfiles;
import com.google.j2cl.transpiler.J2clTranspiler.Result;
import com.google.j2cl.transpiler.J2clTranspilerDriver;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
    public List<String> errorLines = new ArrayList<>();
    public int exitCode;
    public List<String> outputLines = new ArrayList<>();
    public File outputLocation;
  }

  protected static void assertLogContainsSnippet(List<String> logLines, String snippet) {
    boolean foundSnippet = false;
    for (String logLine : logLines) {
      if (logLine.contains(snippet)) {
        foundSnippet = true;
        break;
      }
    }
    assertTrue(
        "Expected to find snippet '"
            + snippet
            + "' but did not find it in log:\n"
            + Joiner.on("\n").join(logLines)
            + "\n",
        foundSnippet);
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
      String directoryName, OutputType outputType, String... extraArgs)
      throws IOException, InterruptedException {
    File outputLocation = Files.createTempDir();
    if (outputType == OutputType.ZIP) {
      outputLocation = new File(outputLocation, "output.zip");
    }
    String[] transpileArgs =
        IntegrationTestCase.getTranspileArgs(
            outputLocation, this.getClass(), directoryName, extraArgs);
    return transpile(transpileArgs, outputLocation);
  }

  protected TranspileResult transpile(String[] args)
      throws UnsupportedEncodingException, IOException, InterruptedException {
    return transpile(args, null);
  }

  protected TranspileResult transpile(String[] args, File outputLocation)
      throws IOException, InterruptedException, UnsupportedEncodingException {

    TranspileResult transpileResult = new TranspileResult();
    transpileResult.outputLocation = outputLocation;
    Result result = J2clTranspilerDriver.transpile(args);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    result.getProblems().report(new PrintStream(outputStream), new PrintStream(errorStream));

    transpileResult.exitCode = result.getExitCode();
    transpileResult.errorLines =
        Splitter.on('\n').omitEmptyStrings().splitToList(errorStream.toString());
    transpileResult.outputLines =
        Splitter.on('\n').omitEmptyStrings().splitToList(outputStream.toString());

    return transpileResult;
  }
}
