/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.sizetest;

import com.google.ads.testing.testmetricsrecorder.SpongeMetricsRecorder;
import com.google.common.collect.Maps;
import com.google.testing.javascript.JavascriptSizeHelper;
import com.google.testing.util.TestRecorderProperties;
import com.google.testing.util.TestUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A binary size test that reports results to sponge.
 *
 * This is pretty much a fork of {@code DocsJavaScriptSizeTestCase} but adapted to support J2CL
 * integration test structure.
 */
public class J2clJavaScriptSizeTest extends TestCase implements TestRecorderProperties {

  private final Map<String, Object> measurements = Maps.newHashMap();

  private static final String BASE_BINARY_DIR =
      "/google3/third_party/java_src/j2cl/transpiler/"
          + "javatests/com/google/j2cl/transpiler/integration/";

  public void testJavaScriptSizes() throws IOException {
    List<File> binaryDirs = new ArrayList<>();
    File baseDir = new File(TestUtil.getSrcDir() + BASE_BINARY_DIR);
    for (File subfile : baseDir.listFiles()) {
      if (subfile.isDirectory()) {
        binaryDirs.add(subfile);
      }
    }

    getAndReportJavaScriptSizes("J2clJsSize", binaryDirs);
  }

  public static TestSuite suite() throws Exception {
    TestSuite testSuite = new TestSuite();
    testSuite.setName("J2clJavaScriptSizes");
    testSuite.addTestSuite(J2clJavaScriptSizeTest.class);
    return testSuite;
  }

  private void getAndReportJavaScriptSizes(String benchmarkName, Iterable<File> binaryDirs)
      throws IOException {

    JavascriptSizeHelper sizeHelper = new JavascriptSizeHelper();

    for (File binaryDir : binaryDirs) {
      pushToMeasurementResults(
          binaryDir.getName(), sizeHelper.getJavascriptFileSize(binaryDir, false /* no gzip */));
    }

    SpongeMetricsRecorder recorder = new SpongeMetricsRecorder(benchmarkName);
    for (Map.Entry<String, Object> sample : measurements.entrySet()) {
      recorder.addMeasurement(sample.getKey(), (Long) sample.getValue());
    }
    recorder.uploadDataToSponge();
  }

  private void pushToMeasurementResults(String name, Map<String, Long> results) {
    for (Entry<String, Long> entry : results.entrySet()) {
      measurements.put(name + "_minopt_js", entry.getValue());
    }
  }

  @Override
  public Map<String, Object> getTestRecorderProperties() {
    return measurements;
  }
}
