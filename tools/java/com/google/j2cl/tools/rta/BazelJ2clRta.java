/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.tools.rta;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.common.bazel.FileCache;
import com.google.j2cl.transpiler.backend.libraryinfo.LibraryInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** Runs The J2clRta as a worker. */
final class BazelJ2clRta extends BazelWorker {

  private static final int CACHE_SIZE =
      Integer.parseInt(System.getProperty("j2cl.rta.protocachesize", "5000"));

  private static final FileCache<LibraryInfo> libraryInfoCache =
      new FileCache<>(BazelJ2clRta::readLibraryInfo, CACHE_SIZE);

  @Option(
      name = "--unusedTypesOutput",
      usage = "Path of output file containing the list of unused types",
      required = true)
  String unusedTypesOutputFilePath = null;

  @Option(
      name = "--removalCodeInfoOutput",
      usage = "Path of output file containing the list files and lines that can be removed.",
      required = true)
  String removalCodeInfoOutputFilePath = null;

  @Option(
      name = "--legacy_keep_jstype_interfaces_do_not_use",
      usage =
          "When this flag is set all JsType interfaces, even uninstantiated ones, are "
              + "retained.",
      required = false)
  boolean keepJsTypeInterfaces = false;

  @Argument(required = true, usage = "The list of call graph files", multiValued = true)
  List<String> inputs = null;

  @Override
  protected void run(Problems problems) {
    List<LibraryInfo> libraryInfos =
        inputs.parallelStream().map(libraryInfoCache::get).collect(toImmutableList());

    RtaResult rtaResult = RapidTypeAnalyser.analyse(libraryInfos, keepJsTypeInterfaces);

    writeToFile(unusedTypesOutputFilePath, rtaResult.getUnusedTypes(), problems);
    writeToFile(removalCodeInfoOutputFilePath, rtaResult.getCodeRemovalInfo(), problems);
  }

  private static LibraryInfo readLibraryInfo(Path libraryInfoPath) throws IOException {
    try (InputStream inputStream = java.nio.file.Files.newInputStream(libraryInfoPath)) {
      return LibraryInfo.parseFrom(inputStream);
    }
  }

  private static void writeToFile(String filePath, List<String> lines, Problems problems) {
    CharSink outputSink = Files.asCharSink(new File(filePath), StandardCharsets.UTF_8);
    try {
      outputSink.writeLines(lines);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  private static void writeToFile(String filePath, CodeRemovalInfo results, Problems problems) {
    try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
      results.writeTo(outputStream);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2clRta::new);
  }
}
