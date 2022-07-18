/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang;

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;

// TODO(b/223774683): Java Number should implement Serializable. Kotlin Number doesn't.
/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Number.html">the official Java
 * API doc</a> for details.
 */
@KtNative(value = "kotlin.Number", bridgeWith = "javaemul.lang.JavaNumber")
public abstract class Number {

  public Number() {}

  @KtName("toByte")
  public native byte byteValue();

  @KtName("toDouble")
  public abstract double doubleValue();

  @KtName("toFloat")
  public abstract float floatValue();

  @KtName("toInt")
  public abstract int intValue();

  @KtName("toLong")
  public abstract long longValue();

  @KtName("toShort")
  public native short shortValue();
}
