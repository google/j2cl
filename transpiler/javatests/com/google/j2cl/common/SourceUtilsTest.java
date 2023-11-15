/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.common;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SourceUtilsTest {

  private static final String[][] JAVA_PATHS_GOOD = {
    {"java", "com/google/foo/Foo"},
    {"javatests", "com/google/foo/FooTest"},
    {"java", "com/google/foo/java/Foo"},
    {"javatests", "com/google/foo/java/FooTest"},
    {"experimental/java", "com/google/foo/Foo"},
    {"experimental/javatests", "com/google/foo/FooTest"},
    {"experimental/users/x/java", "com/google/foo/Foo"},
    {"experimental/users/x/javatests", "com/google/foo/FooTest"},
    {"third_party/java", "foo/org/foo/Foo"},
    {"third_party/java", "blah/java/org/foo/Foo"},
    {"third_party/java", "blah/java/org/foo/java/Foo"},
    {"third_party/java/org/foo/super", "org/foo/Foo"},
    {"third_party/java/org/foo/super-j2cl", "org/foo/Foo"},
    {"third_party/java/org/foo/super", "org/foo/java/Foo"},
    {"third_party/java/org/foo/super-j2cl", "org/foo/java/Foo"},
    {"third_party/java/org/foo/super", "org/foo/super/Foo"},
    {"third_party/java/org/foo/super-j2cl", "org/foo/super/Foo"},
    {"external/org/foo/super", "org/foo/Foo"},
    {"external/org/foo/super-j2cl", "org/foo/Foo"},
  };

  private static final String[] JAVA_PATHS_BAD = {
    "java", "javatests", "java/", "foo/java", "abc/foo/Bar"
  };

  @Test
  public void testGetJavaPath_Good() {
    for (int i = 0; i < JAVA_PATHS_GOOD.length; i++) {
      String path = JAVA_PATHS_GOOD[i][0] + "/" + JAVA_PATHS_GOOD[i][1];
      String expectedPath = JAVA_PATHS_GOOD[i][1];
      assertThat(SourceUtils.getJavaPath(path)).isEqualTo(expectedPath);
    }
  }

  @Test
  public void testGetJavaPath_Bad() {
    for (int i = 0; i < JAVA_PATHS_BAD.length; i++) {
      assertThat(SourceUtils.getJavaPath(JAVA_PATHS_BAD[i])).isEqualTo(JAVA_PATHS_BAD[i]);
    }
  }
}
