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
package com.google.j2cl.jre;

import com.google.j2cl.jre.java8.math.BigIntegerConvertTest;
import com.google.j2cl.jre.java8.util.ArraysTest;
import com.google.j2cl.jre.java8.util.Base64Test;
import com.google.j2cl.jre.java8.util.ComparatorTest;
import com.google.j2cl.jre.java8.util.DoubleSummaryStatisticsTest;
import com.google.j2cl.jre.java8.util.IntSummaryStatisticsTest;
import com.google.j2cl.jre.java8.util.LongSummaryStatisticsTest;
import com.google.j2cl.jre.java8.util.OptionalDoubleTest;
import com.google.j2cl.jre.java8.util.OptionalIntTest;
import com.google.j2cl.jre.java8.util.OptionalLongTest;
import com.google.j2cl.jre.java8.util.OptionalTest;
import com.google.j2cl.jre.java8.util.PrimitiveIteratorTest;
import com.google.j2cl.jre.java8.util.SpliteratorsTest;
import com.google.j2cl.jre.java8.util.StringJoinerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE emulations. */
@RunWith(Suite.class)
@SuiteClasses({
  // -- java.math
  BigIntegerConvertTest.class,

  // -- java.util
  ArraysTest.class,
  Base64Test.class,
  ComparatorTest.class,
  OptionalTest.class,
  OptionalIntTest.class,
  OptionalLongTest.class,
  OptionalDoubleTest.class,
  PrimitiveIteratorTest.class,
  SpliteratorsTest.class,
  StringJoinerTest.class,
  DoubleSummaryStatisticsTest.class,
  IntSummaryStatisticsTest.class,
  LongSummaryStatisticsTest.class,

})
public class EmulJava8Suite {}
