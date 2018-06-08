/*
 * Copyright 2016 Google Inc.
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

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A transpiler entry point that compiles code twice.
 *
 * <p>This is used in testing to run the transpiler twice and possibly uncover static state within
 * the transpiler.
 */
public class RerunningJ2clTranspiler {

  private static class CompilerRunner implements Runnable {

    private boolean success;
    private final String[] args;

    public CompilerRunner(String[] args) {
      this.args = args;
    }

    public boolean compile() throws InterruptedException {
      Thread thread = new Thread(this);
      thread.start();
      thread.join();
      return success;
    }

    @Override
    public void run() {
      J2clTranspiler transpiler = new J2clTranspiler();

      try {
        Problems problems = transpiler.transpile(args);
        success = problems.reportAndGetExitCode(System.err) == 0;
      } catch (RuntimeException r) {
        // Compiler correctness preconditions were violated. Log a stacktrace and quit the test
        r.printStackTrace();
        System.exit(-1);
      }
    }
  }

  public static void main(final String[] rawArgs) throws Exception {
    Problems problems = new Problems();
    String[] args = FrontendUtils.expandFlagFile(rawArgs);
    FrontendFlags frontendFlags = FrontendFlags.parse(args, problems);
    problems.abortIfRequested();

    File outputZip = new File(frontendFlags.output);

    boolean firstCompileSuccess = compile(new CompilerRunner(args));
    Set<OutFile> firstCompileOut = null;

    byte[] firstOutputData = null;
    if (firstCompileSuccess) {
      firstCompileOut = getOutFilesFromZip(outputZip);
      firstOutputData = Files.toByteArray(outputZip);
    }

    boolean secondCompileSuccess = compile(new CompilerRunner(args));

    if (firstCompileSuccess != secondCompileSuccess) {
      System.err.println("Compiles do not agree on success");
      System.exit(-1);
    }

    if (secondCompileSuccess) {
      Set<OutFile> secondCompileOut = getOutFilesFromZip(outputZip);
      SetView<OutFile> difference = Sets.difference(secondCompileOut, firstCompileOut);

      if (!difference.isEmpty()) {
        System.err.println("Output of first and second compile do not match");
        for (OutFile outFile : difference) {
          System.err.println("File: " + outFile.name);
        }
        System.exit(-2);
      }
    }

    if (!firstCompileSuccess) {
      System.err.println("Compile failed");
      System.exit(-3);
    }

    if (!Arrays.equals(firstOutputData, Files.toByteArray(outputZip))) {
      System.err.println("Output is not identical");
      System.exit(-4);
    }
  }

  private static boolean compile(CompilerRunner compilerRunner) {
    try {
      return compilerRunner.compile();
    } catch (InterruptedException e) {
      System.exit(-1);
      return false;
    }
  }

  private static class OutFile {
    public final String name;
    public final long hash;

    public OutFile(String name, long hash) {
      this.name = name;
      this.hash = hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof OutFile)) {
        return false;
      }
      OutFile other = (OutFile) obj;
      return Objects.equal(name, other.name) && hash == other.hash;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, hash);
    }
  }

  private static Set<OutFile> getOutFilesFromZip(File f) throws ZipException, IOException {
    Set<OutFile> outFiles = new HashSet<>();

    try (ZipFile zipFile = new ZipFile(f)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        String name = zipEntry.getName();
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        HashCode hashCode = Hashing.sha256().hashBytes(ByteStreams.toByteArray(inputStream));
        long hash = hashCode.padToLong();
        outFiles.add(new OutFile(name, hash));
      }
    }
    return outFiles;
  }
}
