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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.devtools.build.runtime.Runfiles;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests.
 * <p>
 * Provides utilities for running the transpiler (with or without a target directory of Java source)
 * and making assertions about the std and err log output.
 */
public class IntegrationTestCase extends TestCase {

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

  protected static final String TRANSPILER_BINARY = "third_party/java_src/j2cl/j2cl";

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

    argList.add(TRANSPILER_BINARY);

    // Output dir
    argList.add("-d");
    argList.add(outputLocation.getAbsolutePath());

    // Input source
    for (File javaFile : listJavaFilesInDir(testClass, inputDirectoryName)) {
      argList.add(javaFile.getAbsolutePath());
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
        Runfiles.packageRelativeLocation(
                "third_party/java_src/j2cl/transpiler/javatests", clazz, directoryName)
            .listFiles());
  }

  private static List<File> listJavaFilesInDir(Class<?> clazz, String directoryName) {
    return listExtensionFilesInDir(".java", clazz, directoryName);
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

    class OutputProcessor implements Runnable {

      private final InputStream inputStream;
      private final OutputStream outputStream;
      private final List<String> logLines;

      OutputProcessor(InputStream inputStream, OutputStream outputStream, List<String> logLines) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.logLines = logLines;
      }

      @Override
      public void run() {
        String line;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
            PrintStream formattedOutput = new PrintStream(outputStream)) {
          while ((line = reader.readLine()) != null) {
            logLines.add(line);
            formattedOutput.println(line);
            formattedOutput.flush();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    ProcessBuilder processBuilder = new ProcessBuilder(args);
    Process transpileProcess = processBuilder.start();
    TranspileResult transpileResult = new TranspileResult();
    transpileResult.outputLocation = outputLocation;
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    executorService.execute(
        new OutputProcessor(
            transpileProcess.getInputStream(), System.out, transpileResult.outputLines));
    executorService.execute(
        new OutputProcessor(
            transpileProcess.getErrorStream(), System.err, transpileResult.errorLines));
    executorService.shutdown();

    // Wait for transpilation to finish, gather the results and return them.
    transpileResult.exitCode = transpileProcess.waitFor();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    return transpileResult;
  }
}
