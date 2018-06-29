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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.FrontendUtils;
import com.google.j2cl.frontend.FrontendUtils.FileInfo;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.List;

/**
 * This tool comments out source code elements annotated with @GwtIncompatible so that they are
 * ignored by tools taking that source as input such as the _java_library compile and the
 * j2cl_transpile action.
 */
public class Stripper {

  public static void main(String... args) {
    System.exit(Stripper.strip(args).reportAndGetExitCode(System.err));
  }

  static Problems strip(String[] args) {
    try {
      Problems problems = new Problems();
      StripperFlags flags = StripperFlags.parse(args, problems);
      FileSystem outputZipFileSystem = FrontendUtils.initZipOutput(flags.outputPath, problems);
      List<FileInfo> allPaths =
          FrontendUtils.getAllSources(flags.files, problems)
              .filter(f -> f.targetPath().endsWith(".java"))
              .collect(ImmutableList.toImmutableList());
      JavaPreprocessor.preprocessFiles(allPaths, outputZipFileSystem.getPath("/"), problems);

      try {
        outputZipFileSystem.close();
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_CLOSE_ZIP, e.getMessage());
      }
      return problems;
    } catch (Problems.Exit e) {
      return e.getProblems();
    }
  }
}
