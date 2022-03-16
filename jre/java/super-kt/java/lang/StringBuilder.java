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

import java.io.Serializable;
import javaemul.internal.annotations.KtNative;

@KtNative("kotlin.text.StringBuilder")
public final class StringBuilder implements Appendable, CharSequence, Serializable {
  public StringBuilder() {}

  public StringBuilder(int capacity) {}

  public StringBuilder(CharSequence seq) {}

  public StringBuilder(String str) {}

  public native StringBuilder append(boolean b);

  public native StringBuilder append(char c);

  public native StringBuilder append(int i);

  public native StringBuilder append(long l);

  public native StringBuilder append(float f);

  public native StringBuilder append(double d);

  public native StringBuilder append(Object obj);

  public native StringBuilder append(String str);

  // TODO(b/224969395): Add once StringBuffer is available
  // public native StringBuilder append(StringBuffer sb);

  public native StringBuilder append(char[] chars);

  public native StringBuilder append(char[] str, int offset, int len);

  public native StringBuilder append(CharSequence csq);

  public native StringBuilder append(CharSequence csq, int start, int end);

  public native StringBuilder appendCodePoint(int codePoint);

  public native char charAt(int index);

  public native StringBuilder delete(int start, int end);

  public native StringBuilder deleteCharAt(int index);

  public native StringBuilder insert(int offset, boolean b);

  public native StringBuilder insert(int offset, char c);

  public native StringBuilder insert(int offset, int i);

  public native StringBuilder insert(int offset, long l);

  public native StringBuilder insert(int offset, float f);

  public native StringBuilder insert(int offset, double d);

  public native StringBuilder insert(int offset, Object obj);

  public native StringBuilder insert(int offset, String str);

  public native StringBuilder insert(int offset, char[] ch);

  public native StringBuilder insert(int offset, char[] str, int strOffset, int strLen);

  public native StringBuilder insert(int offset, CharSequence s);

  public native StringBuilder insert(int offset, CharSequence s, int start, int end);

  public native int length();

  public native StringBuilder replace(int start, int end, String string);

  public native StringBuilder reverse();

  public native CharSequence subSequence(int start, int end);

  @Override
  public native String toString();
}
