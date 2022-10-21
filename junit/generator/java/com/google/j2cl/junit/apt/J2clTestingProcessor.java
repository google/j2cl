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
package com.google.j2cl.junit.apt;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;

/** The J2ClTestingProcessor emits a js_unit test JavaScript file for every JUnit4 test. */
@AutoService(Processor.class)
@SupportedOptions({J2clTestingProcessor.JAVAC_OPTS_FLAG_TEST_PLATFORM})
public class J2clTestingProcessor extends BasicAnnotationProcessor {

  static final String JAVAC_OPTS_FLAG_TEST_PLATFORM = "testPlatform";

  private J2clTestingProcessingStep step;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    step = new J2clTestingProcessingStep(processingEnv);
    return Lists.newArrayList(step);
  }

  @Override
  protected void postRound(RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      step.writeSummary();
    }
  }
}
