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

import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.j2cl.libraryinfo.LibraryInfo;
import com.google.j2cl.libraryinfo.TypeInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/** Command-line helper for running the RTA algorithm. */
public class J2clRta {
  public static void main(String[] args) throws CmdLineException {
    J2clRta runner = new J2clRta();
    CmdLineParser parser = new CmdLineParser(runner);

    parser.parseArgument(args);
    runner.run();
  }

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

  private void run() {
    List<TypeInfo> typeInfos = collectTypeInfos();

    RtaResult rtaResult = RapidTypeAnalyser.analyse(typeInfos);

    writeToFile(unusedTypesOutputFilePath, rtaResult.getUnusedTypes());
    writeToFile(unusedMembersOutputFilePath, rtaResult.getUnusedMembers());
    writeToFile(removalCodeInfoOutputFilePath, rtaResult.getCodeRemovalInfo());
  }

  private List<TypeInfo> collectTypeInfos() {
    try {
      List<TypeInfo> typeInfos = new ArrayList<>();
      for (String callGraphPath : inputs) {
        LibraryInfo libraryInfo = LibraryInfo.parseFrom(new FileInputStream(callGraphPath));
        typeInfos.addAll(libraryInfo.getTypeList());
      }
      return typeInfos;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeToFile(String filePath, List<String> lines) {
    CharSink outputSink = Files.asCharSink(new File(filePath), StandardCharsets.UTF_8);
    try {
      outputSink.writeLines(lines);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeToFile(String filePath, CodeRemovalInfo results) {
    try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
      results.writeTo(outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
