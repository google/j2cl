/*
 * Copyright 2010 Google Inc.
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
package com.google.j2cl.jre;

import com.google.j2cl.jre.java.math.BigDecimalArithmeticTest;
import com.google.j2cl.jre.java.math.BigDecimalCompareTest;
import com.google.j2cl.jre.java.math.BigDecimalConstructorsTest;
import com.google.j2cl.jre.java.math.BigDecimalConvertTest;
import com.google.j2cl.jre.java.math.BigDecimalScaleOperationsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test JRE emulation of BigDecimal.
 */
@RunWith(Suite.class)
@SuiteClasses({
  BigDecimalArithmeticTest.class,
  BigDecimalCompareTest.class,
  BigDecimalConstructorsTest.class,
  BigDecimalConvertTest.class,
  BigDecimalScaleOperationsTest.class,
})
public class BigDecimalSuite { }
