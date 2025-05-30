/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.gwtincompatible;

import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.bazel.BazelWorker;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** Runs The @GwtIncompatible stripper as a worker. */
final class BazelGwtIncompatibleStripper extends BazelWorker {
  @Argument(metaVar = "<source files .java|.srcjar>", usage = "source files")
  List<String> files = new ArrayList<>();

  @Option(
      name = "-d",
      required = true,
      metaVar = "<file>",
      usage = "The directory or zip file into which to place the output.")
  Path outputPath;

  @Option(
      name = "-annotation",
      metaVar = "<annotation>",
      usage = "The name(s) of annotations to strip; defaults to 'GwtIncompatible'")
  List<String> annotations = new ArrayList<>();

  @Override
  protected void run() {
    if (annotations.isEmpty()) {
      annotations.add("GwtIncompatible");
    }
    Path sourceJarDir = SourceUtils.deriveDirectory(this.outputPath, "_source_jars");
    GwtIncompatibleStripper.strip(
        files.stream().map(workdir::resolve).map(Path::toString),
        workdir.resolve(outputPath),
        sourceJarDir,
        problems,
        annotations);
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelGwtIncompatibleStripper::new);
  }
}
