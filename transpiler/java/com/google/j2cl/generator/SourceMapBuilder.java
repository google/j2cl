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
package com.google.j2cl.generator;

import com.google.j2cl.ast.sourcemap.SourcePosition;

import org.apache.commons.lang3.tuple.Pair;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Maintains the source mapping information used to build source maps.
 */
public class SourceMapBuilder {
  // TODO: replace the Pair with a plain SourcePosition
  private final SortedMap<SourcePosition, Pair<String, SourcePosition>>
      javaSourceInfoByOutputSourceInfo = new TreeMap<>();

  public void addMapping(
      String name, SourcePosition inputSourcePosition, SourcePosition outputSourcePosition) {
    javaSourceInfoByOutputSourceInfo.put(outputSourcePosition, Pair.of(name, inputSourcePosition));
  }

  public SortedMap<SourcePosition, Pair<String, SourcePosition>> getMappings() {
    return javaSourceInfoByOutputSourceInfo;
  }
}
