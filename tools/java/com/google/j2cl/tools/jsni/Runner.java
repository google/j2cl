/*
 * Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import com.google.j2cl.common.Problems;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/** Collects the command line arguments and run the JsniConverter, */
public class Runner {
  @Option(name = "--classpath", usage = "The file paths to dependency jars.")
  List<String> classPathEntries = new ArrayList<>();

  @Option(
    name = "--output_file",
    usage = "The path and base filename for the output zip file",
    required = true
  )
  String outputFile = null;

  @Option(name = "--excludes", usage = "The paths of files whose JSNI to exclude.")
  List<String> excludeFileNames = new ArrayList<>();

  @Argument(required = true, multiValued = true, usage = "list of source files to convert")
  List<String> javaFileNames = new ArrayList<>();

  private void run() {
    JsniConverter jsniConverter = new JsniConverter(outputFile);

    try {
      jsniConverter.convert(javaFileNames, classPathEntries, new HashSet<String>(excludeFileNames));

    } catch (Problems.Exit e) {
      jsniConverter.getProblems().report(System.out, System.err);
    }
  }

  public static void main(String[] args) throws CmdLineException {
    Runner runner = new Runner();

    CmdLineParser parser = new CmdLineParser(runner);
    parser.parseArgument(args);

    runner.validateFlags(parser);

    runner.run();
  }

  private void validateFlags(CmdLineParser parser) throws CmdLineException {
    for (String classPathEntry : classPathEntries) {
      if (!new File(classPathEntry).exists()) {
        throw new CmdLineException(parser, String.format("File %s doesn't exist", classPathEntry));
      }
    }

    for (String fileName : javaFileNames) {
      if (!new File(fileName).exists()) {
        throw new CmdLineException(parser, String.format("File %s doesn't exist", fileName));
      }
    }
  }
}
