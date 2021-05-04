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

/** A modifiable {@link CharSequence sequence of characters} for use in creating strings. */
public final class StringBuilder extends AbstractStringBuilder
    implements Appendable, CharSequence, Serializable {

  public StringBuilder() {}

  public StringBuilder(int capacity) {
    super(capacity);
  }

  public StringBuilder(CharSequence seq) {
    super(seq.toString());
  }

  public StringBuilder(String str) {
    super(str);
  }

  public StringBuilder append(boolean b) {
    append0(b ? "true" : "false");
    return this;
  }

  public StringBuilder append(char c) {
    append0(c);
    return this;
  }

  public StringBuilder append(short s) {
    IntegralToString.appendInt(this, s);
    return this;
  }

  public StringBuilder append(byte b) {
    IntegralToString.appendInt(this, b);
    return this;
  }

  public StringBuilder append(int i) {
    IntegralToString.appendInt(this, i);
    return this;
  }

  public StringBuilder append(long l) {
    IntegralToString.appendLong(this, l);
    return this;
  }

  public StringBuilder append(float f) {
    RealToString.appendFloat(this, f);
    return this;
  }

  public StringBuilder append(double d) {
    RealToString.appendDouble(this, d);
    return this;
  }

  public StringBuilder append(Object obj) {
    if (obj == null) {
      appendNull();
    } else {
      append0(obj.toString());
    }
    return this;
  }

  public StringBuilder append(String str) {
    append0(str);
    return this;
  }

  public StringBuilder append(StringBuffer sb) {
    if (sb == null) {
      appendNull();
    } else {
      append0(sb.getValue(), 0, sb.length());
    }
    return this;
  }

  public StringBuilder append(char[] chars) {
    append0(chars);
    return this;
  }

  public StringBuilder append(char[] str, int offset, int len) {
    append0(str, offset, len);
    return this;
  }

  public StringBuilder append(CharSequence csq) {
    if (csq == null) {
      appendNull();
    } else {
      append0(csq, 0, csq.length());
    }
    return this;
  }

  public StringBuilder append(CharSequence csq, int start, int end) {
    append0(csq, start, end);
    return this;
  }

  public StringBuilder appendCodePoint(int codePoint) {
    append0(Character.toChars(codePoint));
    return this;
  }

  public StringBuilder delete(int start, int end) {
    delete0(start, end);
    return this;
  }

  public StringBuilder deleteCharAt(int index) {
    deleteCharAt0(index);
    return this;
  }

  public StringBuilder insert(int offset, boolean b) {
    insert0(offset, b ? "true" : "false");
    return this;
  }

  public StringBuilder insert(int offset, char c) {
    insert0(offset, c);
    return this;
  }

  public StringBuilder insert(int offset, int i) {
    insert0(offset, Integer.toString(i));
    return this;
  }

  public StringBuilder insert(int offset, long l) {
    insert0(offset, Long.toString(l));
    return this;
  }

  public StringBuilder insert(int offset, float f) {
    insert0(offset, Float.toString(f));
    return this;
  }

  public StringBuilder insert(int offset, double d) {
    insert0(offset, Double.toString(d));
    return this;
  }

  public StringBuilder insert(int offset, Object obj) {
    insert0(offset, obj == null ? "null" : obj.toString());
    return this;
  }

  public StringBuilder insert(int offset, String str) {
    insert0(offset, str);
    return this;
  }

  public StringBuilder insert(int offset, char[] ch) {
    insert0(offset, ch);
    return this;
  }

  public StringBuilder insert(int offset, char[] str, int strOffset, int strLen) {
    insert0(offset, str, strOffset, strLen);
    return this;
  }

  public StringBuilder insert(int offset, CharSequence s) {
    insert0(offset, s == null ? "null" : s.toString());
    return this;
  }

  public StringBuilder insert(int offset, CharSequence s, int start, int end) {
    insert0(offset, s, start, end);
    return this;
  }

  public StringBuilder replace(int start, int end, String string) {
    replace0(start, end, string);
    return this;
  }

  public StringBuilder reverse() {
    reverse0();
    return this;
  }
}
