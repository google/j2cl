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
package com.google.gwt.emultest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for all of GWT's emul suites.
 */
public class AllTests {

    public static Test suite() {
      TestSuite suite = new TestSuite("All Emul tests");
      suite.addTest(BigDecimalSuite.suite());
      suite.addTest(BigIntegerSuite.suite());
      suite.addTest(CollectionsSuite.suite());
      suite.addTest(EmulSuite.suite());
    suite.addTest(EmulJava8Suite.suite());
      suite.addTest(TreeMapSuiteSub.suite());
      suite.addTest(TreeSetSuiteSub.suite());
      return suite;
    }
 }
