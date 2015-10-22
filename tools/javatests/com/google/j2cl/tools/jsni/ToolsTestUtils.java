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

import com.google.devtools.build.runtime.Runfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains util methods used by test classes.
 */
public class ToolsTestUtils {

  private ToolsTestUtils() {
  }

  public static List<String> getDataFilePaths(String... dataFileNames) {
    List<String> filePaths = new ArrayList<>();

    for (String dataFileName : dataFileNames) {
      filePaths.add(getDataFilePath(dataFileName));
    }

    return filePaths;
  }

  public static File getDataFile(String dataFileName) {
    return Runfiles.packageRelativeLocation(
        "third_party/java_src/j2cl/tools/javatests",
        JsniConverterArgsTest.class,
        "testdata/" + dataFileName);
  }

  public static String getDataFilePath(String dataFileName) {
    return getDataFile(dataFileName).getPath();
  }
}
