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

import static com.google.j2cl.common.SourceUtils.checkSourceFiles;

import com.google.j2cl.common.CommandLineTool;
import com.google.j2cl.common.Problems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * A javac-like command line driver for stripping code annotated with "incompatible" annotations
 * (e.g. @GwtIncompatible).
 */
public final class GwtIncompatibleStripperCommandLineRunner extends CommandLineTool {
  @Argument(metaVar = "<source files .java|.srcjar>", usage = "source files")
  List<String> files = new ArrayList<>();

  @Option(
      name = "-d",
      metaVar = "<file>",
      usage = "The directory or zip file into which to place the output.")
  Path outputPath = Paths.get(".");

  @Option(
      name = "-annotation",
      metaVar = "<annotation>",
      usage = "The name of hte annotation to strip; defaults to 'GwtIncompatible'")
  String annotation = "GwtIncompatible";

  private GwtIncompatibleStripperCommandLineRunner() {
    super("gwt-incompatible-stripper");
  }

  @Override
  protected void run(Problems problems) {
    checkSourceFiles(problems, files, ".java", ".srcjar", ".jar");
    GwtIncompatibleStripper.strip(files, outputPath, problems, annotation);
  }

  public static int run(String[] args) {
    return new GwtIncompatibleStripperCommandLineRunner().execute(args);
  }

  public static void main(String[] args) {
    System.exit(run(args));
  }
}
