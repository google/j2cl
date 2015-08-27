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

/**
 * Contains utils method used by test classes.
 */
public class ToolsTestUtils {

  private ToolsTestUtils() {
  }

  public static File[] getDataFiles(String... dataFileNames) {
    File[] files = new File[dataFileNames.length];

    int i = 0;
    for (String fileName : dataFileNames) {
      files[i++] = getDataFile(fileName);
    }

    return files;
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
