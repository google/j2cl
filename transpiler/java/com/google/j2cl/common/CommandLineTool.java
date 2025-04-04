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
package com.google.j2cl.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.MoreFiles;
import com.google.j2cl.common.Problems.FatalError;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/** A base class for command line tool. */
public abstract class CommandLineTool {

  @Option(name = "-help", usage = "print this message")
  protected boolean help = false;

  private final String toolName;
  protected final Problems problems;
  protected Path tempDir;

  protected CommandLineTool(String toolName) {
    this(toolName, new Problems());
  }

  protected CommandLineTool(String toolName, Problems problems) {
    this.toolName = toolName;
    this.problems = problems;
  }

  protected abstract void run();

  protected final int execute(Collection<String> args, PrintStream pw) {
    CmdLineParser parser = new CmdLineParser(this);

    final String usage = "Usage: " + toolName + " <options> <source files>";

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      if (!this.help) {
        String message = "%s\n%s\nuse -help for a list of possible options";
        problems.error(message, e.getMessage(), usage);
        return problems.reportAndGetExitCode(pw);
      }
    }

    if (this.help) {
      String message = "%s\nwhere possible options include:\n%s";
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      parser.printUsage(new PrintStream(outputStream));
      problems.info(message, usage, new String(outputStream.toByteArray(), UTF_8));
      return problems.reportAndGetExitCode(pw);
    }

    try {
      setupTempDir();
      run();
    } catch (Problems.Exit e) {
      // Program aborted due to errors recorded in problems.
    } finally {
      cleanupTempDir();
    }
    return problems.reportAndGetExitCode(pw);
  }

  private void setupTempDir() {
    try {
      tempDir = Files.createTempDirectory(toolName);
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_CREATE_TEMP_DIR, e.getMessage());
    }
  }

  private void cleanupTempDir() {
    try {
      MoreFiles.deleteRecursively(tempDir);
    } catch (IOException e) {
      problems.error("Failed to clean up temp directory: %s", e.getMessage());
    }
  }
}
