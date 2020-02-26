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
package com.google.j2cl.junit.integration.junit4.data;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** A unit test to test processing handling of overridden methods. */
@RunWith(JUnit4.class)
public class IgnoreTest extends IgnoreTestParent {
  @Test
  @Override
  public void testOverriddenWithTest() {}

  @Override
  public void testOverriddenWithoutTest() {}

  @Ignore
  @Test
  @Override
  public void testOverriddenWithTestAndIgnore() {}

  @Ignore
  @Override
  public void testOverriddenWithIgnoreButNoTest() {}
}
