/*
 * Copyright 2014 Google Inc.
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

/** StringBuilder performance test with long strings. */
public class StringBuilderLongStringsBenchmark extends AbstractBenchmark {

  private String[] array;

  @Override
  public Object run() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < array.length; i++) {
      builder.append(array[i]);
    }
    return builder.toString();
  }

  @Override
  public void setupOneTime() {
    int length = 1000;
    array = new String[length];

    for (int i = 0; i < length; i++) {
      array[i] =
          "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + "this_is_some_string_that_has_some_length_to_see_if_this_makes_a_difference"
              + i;
    }
  }
}
