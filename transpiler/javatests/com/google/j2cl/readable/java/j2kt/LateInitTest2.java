/*
 * Copyright 2023 Google Inc.
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
package j2kt;

import static org.junit.Assert.assertEquals;

import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LateInitTest2 extends TestCase {

  private String uninitialized;

  @Override
  public void setUp() {
    uninitialized = "I have been initialized";
  }

  public void testLateInit2() {
    assertEquals(uninitialized, "I have been initialized");
  }
}
