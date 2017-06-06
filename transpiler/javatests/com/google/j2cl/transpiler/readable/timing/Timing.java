/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.readable.timing;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.util.List;

/**
 * An example that exists to pull in Guava so that JSCompiler's slow type check time of translated
 * Guava code can be examined.
 */
public class Timing {

  public static boolean run() {
    // Use both Guava collections and base libraries, just so the source and BUILD validators don't
    // complain.
    List<String> fooStrings = Lists.newArrayList("foo");
    List<String> barStrings = Lists.newArrayList("bar");
    return Objects.equal(fooStrings, barStrings);
  }
}
