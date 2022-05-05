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
import com.google.j2cl.jre.java.io.WriterTest;
import com.google.j2cl.jre.java.lang.BooleanTest;
import com.google.j2cl.jre.java.lang.ByteTest;
import com.google.j2cl.jre.java.lang.CharacterTest;
import com.google.j2cl.jre.java.lang.DoubleEqualsSemanticsTest;
import com.google.j2cl.jre.java.lang.DoubleTest;
import com.google.j2cl.jre.java.lang.FloatEqualsSemanticsTest;
import com.google.j2cl.jre.java.lang.FloatTest;
import com.google.j2cl.jre.java.lang.IntegerTest;
import com.google.j2cl.jre.java.lang.LongTest;
import com.google.j2cl.jre.java.lang.MathTest;
import com.google.j2cl.jre.java.lang.ObjectTest;
import com.google.j2cl.jre.java.lang.ShortTest;
import com.google.j2cl.jre.java.lang.StringBufferTest;
import com.google.j2cl.jre.java.lang.StringTest;
import com.google.j2cl.jre.java.lang.SystemTest;
import com.google.j2cl.jre.java.lang.ThreadLocalTest;
import com.google.j2cl.jre.java.lang.ThrowableStackTraceEmulTest;
import com.google.j2cl.jre.java.lang.ThrowableTest;
import com.google.j2cl.jre.java.lang.TypeTest;
import com.google.j2cl.jre.java.lang.reflect.ArrayTest;
import com.google.j2cl.jre.java.math.MathContextTest;
import com.google.j2cl.jre.java.math.MathContextWithObfuscatedEnumsTest;
import com.google.j2cl.jre.java.math.RoundingModeTest;
import com.google.j2cl.jre.java.nio.charset.CharsetTest;
import com.google.j2cl.jre.java.nio.charset.StandardCharsetsTest;
import com.google.j2cl.jre.java.security.MessageDigestTest;
import com.google.j2cl.jre.java.sql.SqlDateTest;
import com.google.j2cl.jre.java.sql.SqlTimeTest;
import com.google.j2cl.jre.java.sql.SqlTimestampTest;
import com.google.j2cl.jre.java.util.ComparatorTest;
import com.google.j2cl.jre.java.util.DateTest;
import com.google.j2cl.jre.java.util.ObjectsTest;
import com.google.j2cl.jre.java.util.RandomTest;
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
  WriterTest.class,

  // -- java.lang
  BooleanTest.class,
  ByteTest.class,
  CharacterTest.class,
  DoubleTest.class,
  DoubleEqualsSemanticsTest.class,
  FloatTest.class,
  FloatEqualsSemanticsTest.class,
  IntegerTest.class,
  LongTest.class,
  MathTest.class,
  ObjectTest.class,
  ShortTest.class,
  StringBufferTest.class,
  StringTest.class,
  SystemTest.class,
  ThreadLocalTest.class,
  ThrowableTest.class,
  ThrowableStackTraceEmulTest.class,
  TypeTest.class,

  // java.lang.reflect
  ArrayTest.class,

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

  // -- java.sql
  SqlDateTest.class,
  SqlTimeTest.class,
  SqlTimestampTest.class,

  // -- java.util
  ComparatorTest.class,
  DateTest.class,
  ObjectsTest.class,
  RandomTest.class,

  // Put last to reduce number of times the test framework switches modules
  MathContextWithObfuscatedEnumsTest.class,
})
public class EmulSuite {}
