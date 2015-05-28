/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A dummy compiler class that emits an empy JavaScript for every given Java file.
 */
public class J2Cl {
  public static void main(String[] args) throws IOException {
    // currently arguments have to be in the exact order, the real compiler won't care

    // args:
    // -out (where to write output (directory)
    // -deps (jars with deps comma seperated)
    // -i (list of input files comma seperated)

    System.out.println("Starting compiler");

    if (args.length != 6) {
      throw new RuntimeException("wrong arguments number");
    }

    // first one is out
    String outputFolderString = args[1];
    File outputFolder = new File(outputFolderString);
    outputFolder.mkdirs();

    // skip deps

    // get input files
    List<String> inputFiles = Splitter.on(",").splitToList(args[5]);

    for (String inputFile : inputFiles) {
      String jsFile = inputFile.substring(0, inputFile.length() - "java".length()) + "js";
      File f = new File(outputFolder, jsFile);
      System.out.println("Compiling " + f.getAbsolutePath());
      File parentFile = new File(f.getParent());
      parentFile.mkdirs();
      Files.write("// generated js file from " + inputFile + "\n", f, Charsets.UTF_8);
    }
  }
}
