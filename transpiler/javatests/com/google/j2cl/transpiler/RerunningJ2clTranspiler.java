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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.j2cl.bazel.BazelWorker;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A transpiler entry point that compiles code twice.
 *
 * <p>This is used in testing to run the transpiler twice and possibly uncover static state within
 * the transpiler.
 */
public class RerunningJ2clTranspiler {

  public static void main(final String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, RerunningJ2clTranspiler::process);
  }

  private static int process(String[] args, PrintWriter output) {
    try {
      return processImpl(args, output);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-10);
      return -1;
    }
  }

  private static int processImpl(String[] args, PrintWriter output) throws IOException {
    File outputZip =
        Iterators.getOnlyElement(
            Stream.of(args)
                .filter(arg -> arg.endsWith("jre_transpiled_twice.js.zip"))
                .map(File::new)
                .iterator());

    int rv = J2clTranspiler.transpile(args).reportAndGetExitCode(output);
    if (rv != 0) {
      System.err.println("First compile failed");
      System.exit(-1);
    }
    Set<OutFile> firstCompileOut = getOutFilesFromZip(outputZip);
    byte[] firstOutputData = Files.toByteArray(outputZip);

    rv = J2clTranspiler.transpile(args).reportAndGetExitCode(output);
    if (rv != 0) {
      System.err.println("Second compile failed");
      System.exit(-1);
    }
    Set<OutFile> secondCompileOut = getOutFilesFromZip(outputZip);
    byte[] secondOutputData = Files.toByteArray(outputZip);

    SetView<OutFile> difference = Sets.difference(secondCompileOut, firstCompileOut);
    if (!difference.isEmpty()) {
      System.err.println("Output of first and second compile do not match");
      for (OutFile outFile : difference) {
        System.err.println("File: " + outFile.getName());
      }
      System.exit(-2);
    }

    if (!Arrays.equals(firstOutputData, secondOutputData)) {
      System.err.println("Output is not identical");
      System.exit(-3);
    }

    return 0;
  }

  @AutoValue
  abstract static class OutFile {
    public abstract String getName();

    public abstract long getHash();

    static OutFile create(String name, long hash) {
      return new AutoValue_RerunningJ2clTranspiler_OutFile(name, hash);
    }
  }

  private static Set<OutFile> getOutFilesFromZip(File f) throws IOException {
    Set<OutFile> outFiles = new HashSet<>();

    try (ZipFile zipFile = new ZipFile(f)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        String name = zipEntry.getName();
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        HashCode hashCode = Hashing.sha256().hashBytes(ByteStreams.toByteArray(inputStream));
        long hash = hashCode.padToLong();
        outFiles.add(OutFile.create(name, hash));
      }
    }
    return outFiles;
  }
}
