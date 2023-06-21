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
package com.google.j2cl.tools.bundler;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.bazel.BazelWorker;
import com.google.j2cl.common.bazel.FileCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/** Runs The J2wasmBundler as a worker. */
final class BazelJ2wasmBundler extends BazelWorker {

  private static final int CACHE_SIZE =
      Integer.parseInt(System.getProperty("j2cl.bundler.cachesize", "5000"));

  private static final FileCache<String> moduleContents =
      new FileCache<>(BazelJ2wasmBundler::readModule, CACHE_SIZE);

  @Argument(required = true, usage = "The list of modular output directories", multiValued = true)
  List<String> inputs = null;

  @Option(
      name = "-output",
      required = true,
      metaVar = "<path>",
      usage = "Directory or zip into which to place the bundled output.")
  Path output;

  @Override
  protected void run(Problems problems) {
    createBundle(problems);
  }

  private void createBundle(Problems problems) {
    ImmutableList<String> moduleContents =
        Stream.of(
                Stream.of("(rec"),
                getModuleParts("types"),
                Stream.of(")"),
                getModuleParts("data"),
                getModuleParts("globals"),
                getModuleParts("functions"))
            .reduce(Stream::concat)
            .orElseGet(Stream::empty)
            .collect(toImmutableList());

    writeToFile(output.toString(), moduleContents, problems);
  }

  private Stream<String> getModuleParts(String name) {
    return inputs.stream()
        .map(d -> String.format("%s/%s.wat", d, name))
        .filter(n -> new File(n).exists())
        .map(BazelJ2wasmBundler.moduleContents::get);
  }

  private static String readModule(Path modulePath) throws IOException {
    return java.nio.file.Files.readString(modulePath);
  }

  private static void writeToFile(String filePath, List<String> contents, Problems problems) {
    try {
      Files.asCharSink(new File(filePath), UTF_8).writeLines(contents);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_WRITE_FILE, e.toString());
    }
  }

  public static void main(String[] workerArgs) throws Exception {
    BazelWorker.start(workerArgs, BazelJ2wasmBundler::new);
  }
}
