/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.benchmarks.jre;

import com.google.apps.xplat.regex.RegExp;
import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import com.google.j2cl.benchmarks.jre.helper.RegexConstants;
import javax.annotation.Nullable;

/**
 * Benchmark xplat RegExp matching performance when inputs are guaranteed to not match after 11
 * characters.
 */
public class RegExpNonMatchingBenchmark extends AbstractBenchmark {

  private String[] inputs;
  private RegExp regex;

  @Nullable
  @Override
  public Object run() {
    for (String input : inputs) {
      if (regex.test(input)) {
        throw new AssertionError();
      }
    }
    return null;
  }

  @Override
  public void setupOneTime() {
    regex = RegExp.compile(RegexConstants.ALL_ALPHANUMERIC_PATTERN);
    inputs =
        new String[] {
          RegexConstants.SHORT_NON_MATCHING,
          RegexConstants.MEDIUM_NON_MATCHING,
          RegexConstants.LONG_NON_MATCHING,
          RegexConstants.EXTRA_LONG_NON_MATCHING,
        };
  }
}
