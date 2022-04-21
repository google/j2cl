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

import com.google.j2cl.jre.java.math.BigIntegerAddTest;
import com.google.j2cl.jre.java.math.BigIntegerAndTest;
import com.google.j2cl.jre.java.math.BigIntegerCompareTest;
import com.google.j2cl.jre.java.math.BigIntegerConstructorsTest;
import com.google.j2cl.jre.java.math.BigIntegerDivideTest;
import com.google.j2cl.jre.java.math.BigIntegerHashCodeTest;
import com.google.j2cl.jre.java.math.BigIntegerModPowTest;
import com.google.j2cl.jre.java.math.BigIntegerMultiplyTest;
import com.google.j2cl.jre.java.math.BigIntegerNotTest;
import com.google.j2cl.jre.java.math.BigIntegerOperateBitsTest;
import com.google.j2cl.jre.java.math.BigIntegerOrTest;
import com.google.j2cl.jre.java.math.BigIntegerSubtractTest;
import com.google.j2cl.jre.java.math.BigIntegerToStringTest;
import com.google.j2cl.jre.java.math.BigIntegerXorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test JRE emulation of BigInteger.
 */
@RunWith(Suite.class)
@SuiteClasses({
  BigIntegerAddTest.class,
  BigIntegerAndTest.class,
  BigIntegerCompareTest.class,
  BigIntegerConstructorsTest.class,
  BigIntegerDivideTest.class,
  BigIntegerHashCodeTest.class,
  BigIntegerModPowTest.class,
  BigIntegerMultiplyTest.class,
  BigIntegerNotTest.class,
  BigIntegerOperateBitsTest.class,
  BigIntegerOrTest.class,
  BigIntegerSubtractTest.class,
  BigIntegerToStringTest.class,
  BigIntegerXorTest.class,
})
public class BigIntegerSuite { }
