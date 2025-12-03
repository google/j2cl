/*
 * Copyright 2025 Google Inc.
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

import java.io.File;
import java.nio.file.Path;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.OptionHandlerRegistry;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.Messages;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * A wrapper around args4j parser that resolves relative paths against provided working directory.
 */
public final class CommandLineParser extends CmdLineParser {

  static {
    // Override the default handler for Path options to use our RelativePathOptionHandler.
    OptionHandlerRegistry.getRegistry()
        .registerHandler(Path.class, RelativePathOptionHandler.class);
  }

  private final Path workdir;

  public CommandLineParser(Object bean, Path workdir) {
    super(bean);
    this.workdir = workdir;
  }

  /** An option handler that resolves relative paths against provided working directory. */
  public static final class RelativePathOptionHandler extends OneArgumentOptionHandler<Path> {
    public RelativePathOptionHandler(
        CmdLineParser parser, OptionDef option, Setter<? super Path> setter) {
      super(parser, option, setter);
    }

    @Override
    protected Path parse(String argument) throws CmdLineException {
      Path workdir = ((CommandLineParser) this.owner).workdir;
      try {
        return workdir.resolve(argument);
      } catch (Exception e) {
        throw new CmdLineException(owner, Messages.ILLEGAL_PATH, argument);
      }
    }
  }

  /**
   * An option handler that parses a list of paths separated by the system path separator and
   * resolves relative paths against provided working directory.
   */
  public static class MultiPathOptionHandler extends DelimitedOptionHandler<Path> {
    public MultiPathOptionHandler(
        CmdLineParser parser, OptionDef option, Setter<? super Path> setter) {
      super(
          parser,
          option,
          setter,
          File.pathSeparator,
          new RelativePathOptionHandler(parser, option, setter));
    }
  }
}
