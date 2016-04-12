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
import com.google.j2cl.common.PackageInfoCache;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.JdtParser;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Main class for the converter. This class will parse the different flags passed as arguments and
 * instantiate the different Objects needed for the conversion.
 */
public class JsniConverter {
  @FlagSpec(name = "verbose", help = "Display debug messages")
  private static final Flag<Boolean> isVerboseFlag = Flag.value(false);

  @FlagSpec(name = "class_path", help = "The file paths to dependency jars.")
  private static final Flag<List<String>> classPathFlag = Flag.stringCollector();

  @FlagSpec(name = "output_file", help = "The path and base filename for the output zip file")
  private static final Flag<String> outputFileFlag = Flag.value("");

  @FlagSpec(name = "excludes", help = "The paths of files whose JSNI to exclude.")
  private static final Flag<List<String>> excludesFlag = Flag.stringCollector();

  private final Errors errors = new Errors();

  public static void main(String[] args) throws InvalidFlagValueException {
    String[] fileNames = Flags.parseAndReturnLeftovers(args);

    validateFlags(fileNames);

    new JsniConverter(outputFileFlag.get())
        .convert(
            Arrays.asList(fileNames), classPathFlag.get(), new HashSet<String>(excludesFlag.get()));
  }

  static void log(String message, Object... args) {
    if (isVerboseFlag.get()) {
      System.out.println(String.format(message, args));
    }
  }

  private static void validateFlags(String[] fileNames) throws InvalidFlagValueException {
    if (Strings.isNullOrEmpty(outputFileFlag.get())) {
      throw new InvalidFlagValueException(
          "Path to the output zip file is missing. Use --output_file flag to specify a path where "
              + "the result zip file will be written.");
    }

    if (!classPathFlag.get().isEmpty()) {
      for (String classPathEntry : classPathFlag.get()) {
        if (!new File(classPathEntry).exists()) {
          throw new InvalidFlagValueException(
              String.format("File %s doesn't exist", classPathEntry));
        }
      }
    }

    if (fileNames.length == 0) {
      throw new InvalidFlagValueException("Path to java file(s) to convert is(are) missing");
    }

    for (String fileName : fileNames) {
      if (!new File(fileName).exists()) {
        throw new InvalidFlagValueException(String.format("File %s doesn't exist", fileName));
      }
    }
  }

  private static Map<String, CompilationUnit> getCompilationUnitsByPath(
      List<String> javaFileNames, List<String> classPathEntries) {
    // Since this tool is currently for a one-time extraction of GWT's standard library JSNI there
    // is no special care being taken to ensure that the classpath is being properly constructed.
    // This may result in some JDT parse errors, but since we are not checking the resulting Error
    // object they are effectively being ignored.
    Errors errors = new Errors();
    JdtParser jdtParser =
        new JdtParser(
            "1.8", classPathEntries, new ArrayList<>(), new ArrayList<>(), "UTF-8", errors);
    jdtParser.setIncludeRunningVMBootclasspath(true);
    Map<String, CompilationUnit> compilationUnitsByPath = jdtParser.parseFiles(javaFileNames);
    errors.maybeReportAndExit();
    return compilationUnitsByPath;
  }

  private final String outputFile;

  public JsniConverter(String outputFile) {
    this.outputFile = outputFile;
  }

  public void convert(
      List<String> javaFileNames, List<String> classPathEntries, Set<String> excludeFileNames) {
    Multimap<String, JsniMethod> jsniMethodsByType = ArrayListMultimap.create();

    PackageInfoCache.init(classPathEntries, errors);
    errors.maybeReportAndExit();

    for (Entry<String, CompilationUnit> entry :
        getCompilationUnitsByPath(javaFileNames, classPathEntries).entrySet()) {
      if (excludeFileNames.contains(entry.getKey())) {
        continue;
      }

      log("Converting %s", entry.getKey());
      jsniMethodsByType.putAll(
          NativeMethodExtractor.getJsniMethodsByType(entry.getKey(), entry.getValue()));
    }

    new NativeJsFilesWriter(outputFile).write(jsniMethodsByType);
  }
}
