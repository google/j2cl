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

import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GrandChildWithNewMethodsTest extends ChildWithNewMethodsTest {

  @Override
  @Before
  public void childSetUp() {
    TestCaseLogger.log("setUpChildOverridden");
  }

  @Override
  @After
  public void childTearDown() {
    TestCaseLogger.log("tearDownChildOverridden");
  }

  @Before
  public void grandChildSetUp() {
    TestCaseLogger.log("grandChildSetUp");
  }

  @After
  public void grandChildTearDown() {
    TestCaseLogger.log("grandChildTearDown");
  }

  @Test
  public void testGrandChild1() {
    TestCaseLogger.log("testGrandChild");
  }

  @Test
  public void testGrandChild2() {
    TestCaseLogger.log("testGrandChild");
  }

  @Override
  @Test(expected = NullPointerException.class)
  public void testChild2() {
    TestCaseLogger.log("testGrandChild");
    throw new NullPointerException();
  }
}
