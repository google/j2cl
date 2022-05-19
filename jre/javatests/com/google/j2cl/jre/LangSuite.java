/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.jre.java.lang.ThrowableTest;
import com.google.j2cl.jre.java.lang.TypeTest;
import com.google.j2cl.jre.java.lang.reflect.ArrayTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/** Test JRE java.lang emulation. */
@RunWith(Suite.class)
@SuiteClasses({
  ArrayTest.class,
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
  TypeTest.class,
})
public class LangSuite {}
