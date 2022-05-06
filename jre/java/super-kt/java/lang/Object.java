/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import javaemul.internal.annotations.KtNative;

@KtNative("kotlin.Any")
public class Object {
  public Object() {}

  // TODO(b/222269323): Decide what to do with clone(). This does not exist in kotlin.Any.
  // protected native Object clone() throws CloneNotSupportedException;

  public native boolean equals(Object o);

  public final native Class<?> getClass();

  public native int hashCode();

  // J2KT: No built-in support for monitors.
  // public final native void notify();
  // public final native void notifyAll();

  public native String toString();

  // J2KT: No built-in support for monitors.
  // public native final void wait() throws InterruptedException;
  // public native final void wait(long millis) throws InterruptedException;
  // public final native void wait(long millis, int nanos) throws InterruptedException;
}
