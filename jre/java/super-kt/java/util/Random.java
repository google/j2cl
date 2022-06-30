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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * INCLUDES MODIFICATIONS BY RICHARD ZSCHECH AS WELL AS GOOGLE.
 */
package java.util;

import javaemul.internal.annotations.KtNative;
import jsinterop.annotations.JsNonNull;

/** Kotlin Random is immutable. To implement setSeed, we need to wrap it. */
@KtNative("java.util.Random")
public class Random {

  public Random() {}

  public Random(long seed) {}

  public native boolean nextBoolean();

  public native void nextBytes(byte @JsNonNull [] buf);

  public native double nextDouble();

  public native float nextFloat();

  public native double nextGaussian();

  public native int nextInt();

  public native int nextInt(int n);

  public native long nextLong();

  public native void setSeed(long seed);
}
