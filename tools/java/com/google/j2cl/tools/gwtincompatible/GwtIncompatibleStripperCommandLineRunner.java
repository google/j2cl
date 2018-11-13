/*
 * Copyright 2018 Google Inc.
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

import static com.google.j2cl.common.FrontendUtils.checkSourceFiles;

import com.google.j2cl.common.CommandLineTool;
import com.google.j2cl.common.Problems;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** A javac-like command line driver for @GwtIncompatible stripper. */
final class GwtIncompatibleStripperCommandLineRunner extends CommandLineTool {
  @Argument(metaVar = "<source files .java|.srcjar>", usage = "source files")
  protected List<String> files = new ArrayList<>();

  @Option(
      name = "-d",
      required = true,
      metaVar = "<file>",
      usage = "The location into which to place output srcjar.")
  protected String outputPath;

  private GwtIncompatibleStripperCommandLineRunner() {
    super("gwt-incompatible-stripper");
  }

  @Override
  protected Problems run() {
    checkSourceFiles(files, ".java", ".srcjar", ".jar");
    return GwtIncompatibleStripper.strip(files, outputPath);
  }

  public static int run(String[] args) {
    return new GwtIncompatibleStripperCommandLineRunner().execute(args);
  }

  public static void main(String[] args) {
    System.exit(run(args));
  }
}
