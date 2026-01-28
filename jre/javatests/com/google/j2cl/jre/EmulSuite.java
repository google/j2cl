/*
 * Copyright 2008 Google Inc.
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

import com.google.j2cl.jre.java.io.BufferedWriterTest;
import com.google.j2cl.jre.java.io.ByteArrayInputStreamTest;
import com.google.j2cl.jre.java.io.ByteArrayOutputStreamTest;
import com.google.j2cl.jre.java.io.FilterInputStreamTest;
import com.google.j2cl.jre.java.io.FilterOutputStreamTest;
import com.google.j2cl.jre.java.io.InputStreamTest;
import com.google.j2cl.jre.java.io.OutputStreamTest;
import com.google.j2cl.jre.java.io.OutputStreamWriterTest;
import com.google.j2cl.jre.java.io.PrintStreamTest;
import com.google.j2cl.jre.java.io.PrintWriterTest;
import com.google.j2cl.jre.java.io.StringWriterTest;
import com.google.j2cl.jre.java.io.WriterTest;
import com.google.j2cl.jre.java.math.MathContextTest;
import com.google.j2cl.jre.java.math.RoundingModeTest;
import com.google.j2cl.jre.java.nio.charset.CharsetTest;
import com.google.j2cl.jre.java.nio.charset.StandardCharsetsTest;
import com.google.j2cl.jre.java.security.MessageDigestTest;
import com.google.j2cl.jre.java.util.Base64Test;
import com.google.j2cl.jre.java.util.ComparatorTest;
import com.google.j2cl.jre.java.util.DoubleSummaryStatisticsTest;
import com.google.j2cl.jre.java.util.IntSummaryStatisticsTest;
import com.google.j2cl.jre.java.util.LongSummaryStatisticsTest;
import com.google.j2cl.jre.java.util.ObjectsTest;
import com.google.j2cl.jre.java.util.OptionalDoubleTest;
import com.google.j2cl.jre.java.util.OptionalIntTest;
import com.google.j2cl.jre.java.util.OptionalLongTest;
import com.google.j2cl.jre.java.util.OptionalTest;
import com.google.j2cl.jre.java.util.PrimitiveIteratorTest;
import com.google.j2cl.jre.java.util.RandomTest;
import com.google.j2cl.jre.java.util.SpliteratorsTest;
import com.google.j2cl.jre.java.util.StringJoinerTest;
import com.google.j2cl.jre.java.util.stream.CollectorsTest;
import com.google.j2cl.jre.java.util.stream.DoubleStreamTest;
import com.google.j2cl.jre.java.util.stream.IntStreamTest;
import com.google.j2cl.jre.java.util.stream.LongStreamTest;
import com.google.j2cl.jre.java.util.stream.StreamSupportTest;
import com.google.j2cl.jre.java.util.stream.StreamTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE emulations. */
@RunWith(Suite.class)
@SuiteClasses({
  // -- java.io
  BufferedWriterTest.class,
  ByteArrayInputStreamTest.class,
  ByteArrayOutputStreamTest.class,
  FilterInputStreamTest.class,
  FilterOutputStreamTest.class,
  InputStreamTest.class,
  OutputStreamTest.class,
  OutputStreamWriterTest.class,
  PrintStreamTest.class,
  PrintWriterTest.class,
  StringWriterTest.class,
  WriterTest.class,

  // -- java.math
  // BigDecimal is tested in {@link BigDecimalSuite}
  // BigInteger is tested in {@link BigIntegerSuite}
  RoundingModeTest.class,
  MathContextTest.class,

  // -- java.nio
  CharsetTest.class,
  StandardCharsetsTest.class,

  // -- java.security
  MessageDigestTest.class,

  // -- java.util
  Base64Test.class,
  ComparatorTest.class,
  ObjectsTest.class,
  RandomTest.class,
  OptionalTest.class,
  OptionalDoubleTest.class,
  OptionalIntTest.class,
  OptionalLongTest.class,
  PrimitiveIteratorTest.class,
  SpliteratorsTest.class,
  StringJoinerTest.class,
  DoubleSummaryStatisticsTest.class,
  IntSummaryStatisticsTest.class,
  LongSummaryStatisticsTest.class,

  // -- java.util.stream
  DoubleStreamTest.class,
  IntStreamTest.class,
  LongStreamTest.class,
  StreamTest.class,
  StreamSupportTest.class,
  CollectorsTest.class,
})
public class EmulSuite {}
