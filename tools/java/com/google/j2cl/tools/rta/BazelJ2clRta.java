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
import com.google.j2cl.bazel.BazelWorker;
import com.google.j2cl.common.Problems;
import com.google.j2cl.libraryinfo.LibraryInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** Runs The J2clRta as a worker. */
public class BazelJ2clRta extends BazelWorker {
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
      name = "--unusedMembersOutput",
      usage = "Path of output file containing the list of unused members",
      required = true)
  String unusedMembersOutputFilePath = null;

  @Argument(required = true, usage = "The list of call graph files", multiValued = true)
  List<String> inputs = null;

  @Override
  protected Problems run() {
    List<LibraryInfo> libraryInfos =
        inputs.parallelStream().map(BazelJ2clRta::readLibraryInfo).collect(toImmutableList());

    RtaResult rtaResult = RapidTypeAnalyser.analyse(libraryInfos);

    writeToFile(unusedTypesOutputFilePath, rtaResult.getUnusedTypes());
    writeToFile(unusedMembersOutputFilePath, rtaResult.getUnusedMembers());
    writeToFile(removalCodeInfoOutputFilePath, rtaResult.getCodeRemovalInfo());

    return new Problems();
  }

  private static LibraryInfo readLibraryInfo(String libraryInfoPath) {
    try (FileInputStream file = new FileInputStream(libraryInfoPath)) {
      return LibraryInfo.parseFrom(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeToFile(String filePath, List<String> lines) {
    CharSink outputSink = Files.asCharSink(new File(filePath), StandardCharsets.UTF_8);
    try {
      outputSink.writeLines(lines);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeToFile(String filePath, CodeRemovalInfo results) {
    try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
      results.writeTo(outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2clRta::new);
  }
}
