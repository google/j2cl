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

package java.lang;

import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtProperty;

@KtNative("kotlin.Throwable")
public class Throwable {

  public Throwable() {}

  public Throwable(String detailMessage) {}

  public Throwable(String detailMessage, Throwable cause) {}

  public Throwable(Throwable cause) {}

  // TODO(b/222269323): Find out if we need to support this.
  // protected Throwable(String detailMessage, Throwable cause, boolean enableSuppression, boolean
  // writableStackTrace) {}

  // J2KT: Disabled
  // public native Throwable fillInStackTrace();

  @KtProperty
  public native String getMessage();

  // TODO(b/222269323): Property does not exist in Kotlin/is not final in Java -> needs review.
  @KtProperty
  public native String getLocalizedMessage();

  @KtProperty
  public native StackTraceElement[] getStackTrace();

  // J2KT: Disabled
  // public native void setStackTrace(StackTraceElement[] trace);

  /**
   * Writes a printable representation of this {@code Throwable}'s stack trace to the {@code
   * System.err} stream.
   */
  public native void printStackTrace();

  // TODO(b/223584513): Temporarily disabled to avoid PrintStream dependency
  // public native void printStackTrace(PrintStream err);
  //
  // public native void printStackTrace(PrintWriter err);

  @Override
  public native String toString();

  // J2KT: Disabled.
  // public native Throwable initCause(Throwable throwable);

  @KtProperty
  public native Throwable getCause();

  public final native void addSuppressed(Throwable throwable);

  public final native Throwable[] getSuppressed();
}
