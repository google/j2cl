/*
 * Copyright 2017 Google Inc.
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

package com.google.j2cl.tools.gwtincompatible;

import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

/** Flags for the @GwtIncompatible Stripping tool. */
public class StripperFlags {
  @Argument(metaVar = "<source files .java|.srcjar>", usage = "source files")
  protected List<String> files = new ArrayList<>();

  @Option(
    name = "-classpath",
    aliases = {"-cp"},
    metaVar = "<path>",
    usage = "Specifies where to find user class files and annotation processors."
  )
  protected String classpath = "";

  @Option(
    name = "-sourcepath",
    metaVar = "<file>",
    usage = "Specifies where to find input source files."
  )
  protected String sourcepath = "";

  @Option(
    name = "-d",
    metaVar = "<file>",
    usage = "The location into which to place output srcjar."
  )
  protected String outputPath;

  @Option(
    name = "-encoding",
    metaVar = "<encoding>",
    usage = "Specifies character encoding used by source files."
  )
  protected String encoding = System.getProperty("file.encoding", "UTF-8");

  @Option(
    name = "-source",
    metaVar = "<release>",
    usage = "Specifies source compatibility level (1.7, 1.8, etc)."
  )
  protected String source = "1.8";

  protected static StripperFlags parse(String[] args) {
    StripperFlags flags = new StripperFlags();
    CmdLineParser parser = new CmdLineParser(flags);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      String message = e.getMessage() + "\n";
      message += "Valid options: \n" + parser.printExample(OptionHandlerFilter.ALL);
      message += "\nuse -help for a list of possible options in more details";
      System.out.println(message);
    }
    return flags;
  }
}
