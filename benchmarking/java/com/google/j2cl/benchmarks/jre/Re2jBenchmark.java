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

import com.google.j2cl.benchmarking.framework.AbstractBenchmark;
import com.google.j2cl.benchmarks.jre.helper.RegexConstants;
import com.google.re2j.Pattern;
import javax.annotation.Nullable;

/** Benchmark RE2/J matching performance. */
public class Re2jBenchmark extends AbstractBenchmark {

  private String[] inputs;
  private Pattern regex;

  @Nullable
  @Override
  public Object run() {
    for (String input : inputs) {
      if (!regex.matches(input)) {
        throw new AssertionError();
      }
    }
    return null;
  }

  @Override
  public void setupOneTime() {
    regex = Pattern.compile(RegexConstants.ALL_ALPHANUMERIC_PATTERN);
    inputs =
        new String[] {
          RegexConstants.SHORT,
          RegexConstants.MEDIUM,
          RegexConstants.LONG,
          RegexConstants.EXTRA_LONG,
        };
  }
}
