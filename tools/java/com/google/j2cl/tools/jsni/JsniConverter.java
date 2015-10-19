/*
 * Copyright 2015 Google Inc.
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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.flags.Flag;
import com.google.common.flags.FlagSpec;
import com.google.common.flags.Flags;
import com.google.common.flags.InvalidFlagValueException;

import java.io.File;

/**
 * Main class for the converter. This class will parse the different flags passed as arguments and
 * instantiate the different Objects needed for the conversion.
 */
public class JsniConverter {
  @FlagSpec(name = "verbose", help = "Display debug messages")
  private static final Flag<Boolean> isVerboseFlag = Flag.value(false);
  @FlagSpec(name = "output_file", help = "The path and base filename for the output zip file")
  private static final Flag<String> outputFileFlag = Flag.value("");

  public static void main(String[] args) throws InvalidFlagValueException {
    String[] fileNames = Flags.parseAndReturnLeftovers(args);

    validateFlags();

    File[] inputFiles = getInputFiles(fileNames);

    new JsniConverter(outputFileFlag.get()).convert(inputFiles);
  }

  static void log(String message, Object... args) {
    if (isVerboseFlag.get()) {
      System.out.println(String.format(message, args));
    }
  }

  private static File[] getInputFiles(String[] fileNames) throws InvalidFlagValueException {
    if (fileNames.length == 0) {
      throw new InvalidFlagValueException("Path to java file(s) to convert is(are) missing");
    }

    File[] inputFiles = new File[fileNames.length];

    for (int i = 0; i < fileNames.length; i++) {
      File file = new File(fileNames[i]);

      if (!file.exists()) {
        throw new InvalidFlagValueException(String.format("File %s doesn't exist", fileNames[i]));
      }

      inputFiles[i] = file;
    }

    return inputFiles;
  }

  private static void validateFlags() throws InvalidFlagValueException {
    if (Strings.isNullOrEmpty(outputFileFlag.get())) {
      throw new InvalidFlagValueException(
          "Path to the output zip file is missing. Use --output_file flag to specify a path where "
              + "the result zip file will be written.");
    }
  }

  private final String outputFile;

  public JsniConverter(String outputFile) {
    this.outputFile = outputFile;
  }

  public void convert(File[] javaFiles) {
    Multimap<String, JsniMethod> jsniMethodsByTypes = ArrayListMultimap.create();
    for (File file : javaFiles) {
      log("Converting %s", file);
      NativeMethodParser parser = new NativeMethodParser();
      jsniMethodsByTypes.putAll(parser.parse(file));
    }
    new NativeJsFilesWriter(outputFile).write(jsniMethodsByTypes);
  }
}
