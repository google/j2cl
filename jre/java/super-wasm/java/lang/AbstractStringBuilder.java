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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkStringBounds;
import static javaemul.internal.InternalPreconditions.checkStringElementIndex;

import java.util.Arrays;

// TODO(b/244382431): Replace with non-wasm version of the class.
abstract class AbstractStringBuilder {
  static final int INITIAL_CAPACITY = 16;
  private char[] value;
  private int count;
  private boolean shared;
  /*
   * Returns the character array.
   */
  final char[] getValue() {
    return value;
  }
  /*
   * Returns the underlying buffer and sets the shared flag.
   */
  final char[] shareValue() {
    shared = true;
    return value;
  }

  AbstractStringBuilder() {
    value = new char[INITIAL_CAPACITY];
  }

  AbstractStringBuilder(int capacity) {
    value = new char[capacity];
  }

  AbstractStringBuilder(String string) {
    count = string.length();
    shared = false;
    value = new char[count + INITIAL_CAPACITY];
    string.getChars0(0, count, value, 0);
  }

  private void enlargeBuffer(int min) {
    int newCount = ((value.length >> 1) + value.length) + 2;
    char[] newData = new char[min > newCount ? min : newCount];
    System.arraycopy(value, 0, newData, 0, count);
    value = newData;
    shared = false;
  }

  final void appendNull() {
    int newCount = count + 4;
    if (newCount > value.length) {
      enlargeBuffer(newCount);
    }
    value[count++] = 'n';
    value[count++] = 'u';
    value[count++] = 'l';
    value[count++] = 'l';
  }

  final void append0(char[] chars) {
    int newCount = count + chars.length;
    if (newCount > value.length) {
      enlargeBuffer(newCount);
    }
    System.arraycopy(chars, 0, value, count, chars.length);
    count = newCount;
  }

  final void append0(char[] chars, int offset, int length) {
    checkStringBounds(offset, offset + length, chars.length);

    int newCount = count + length;
    if (newCount > value.length) {
      enlargeBuffer(newCount);
    }
    System.arraycopy(chars, offset, value, count, length);
    count = newCount;
  }

  final void append0(char ch) {
    if (count == value.length) {
      enlargeBuffer(count + 1);
    }
    value[count++] = ch;
  }

  final void append0(String string) {
    if (string == null) {
      appendNull();
      return;
    }
    int length = string.length();
    int newCount = count + length;
    if (newCount > value.length) {
      enlargeBuffer(newCount);
    }
    string.getChars0(0, length, value, count);
    count = newCount;
  }

  final void append0(CharSequence s, int start, int end) {
    if (s == null) {
      s = "null";
    }
    int length = end - start;
    int newCount = count + length;
    if (newCount > value.length) {
      enlargeBuffer(newCount);
    } else if (shared) {
      value = Arrays.copyOf(value, value.length);
      shared = false;
    }
    if (s instanceof String) {
      ((String) s).getChars0(start, end, value, count);
    } else if (s instanceof AbstractStringBuilder) {
      AbstractStringBuilder other = (AbstractStringBuilder) s;
      System.arraycopy(other.value, start, value, count, length);
    } else {
      int j = count; // Destination index.
      for (int i = start; i < end; i++) {
        value[j++] = s.charAt(i);
      }
    }
    this.count = newCount;
  }

  public int capacity() {
    return value.length;
  }

  public char charAt(int index) {
    checkStringElementIndex(index, count);
    return value[index];
  }

  final void delete0(int start, int end) {
    // NOTE: StringBuilder#delete(int, int) is specified not to throw if
    // the end index is >= count, as long as it's >= start. This means
    // we have to clamp it to count here.
    if (end > count) {
      end = count;
    }

    checkStringBounds(start, end, count);

    // NOTE: StringBuilder#delete(int, int) throws only if start > count
    // (start == count is considered valid, oddly enough). Since 'end' is
    // already a clamped value, that case is handled here.
    if (end == start) {
      return;
    }
    // At this point we know for sure that end > start.
    int length = count - end;
    if (length >= 0) {
      if (!shared) {
        System.arraycopy(value, end, value, start, length);
      } else {
        char[] newData = new char[value.length];
        System.arraycopy(value, 0, newData, 0, start);
        System.arraycopy(value, end, newData, start, length);
        value = newData;
        shared = false;
      }
    }
    count -= end - start;
  }

  final void deleteCharAt0(int index) {
    delete0(index, index + 1);
  }

  public void ensureCapacity(int min) {
    if (min > value.length) {
      int ourMin = value.length * 2 + 2;
      enlargeBuffer(Math.max(ourMin, min));
    }
  }

  public void getChars(int start, int end, char[] dst, int dstStart) {
    checkStringBounds(start, end, count);

    System.arraycopy(value, start, dst, dstStart, end - start);
  }

  final void insert0(int index, char[] chars) {
    checkStringElementIndex(index, count + 1);
    if (chars.length != 0) {
      move(chars.length, index);
      System.arraycopy(chars, 0, value, index, chars.length);
      count += chars.length;
    }
  }

  final void insert0(int index, char[] chars, int start, int length) {
    checkStringElementIndex(index, count + 1);
    checkStringBounds(start, start + length, chars.length);

    if (length != 0) {
      move(length, index);
      System.arraycopy(chars, start, value, index, length);
      count += length;
    }
  }

  final void insert0(int index, char ch) {
    checkStringElementIndex(index, count + 1);

    move(1, index);
    value[index] = ch;
    count++;
  }

  final void insert0(int index, String string) {
    checkStringElementIndex(index, count + 1);

    if (string == null) {
      string = "null";
    }
    int min = string.length();
    if (min != 0) {
      move(min, index);
      string.getChars0(0, min, value, index);
      count += min;
    }
  }

  final void insert0(int index, CharSequence s, int start, int end) {
    if (s == null) {
      s = "null";
    }

    insert0(index, s.subSequence(start, end).toString());
  }

  public int length() {
    return count;
  }

  private void move(int size, int index) {
    int newCount;
    if (value.length - count >= size) {
      if (!shared) {
        // index == count case is no-op
        System.arraycopy(value, index, value, index + size, count - index);
        return;
      }
      newCount = value.length;
    } else {
      newCount = Math.max(count + size, value.length * 2 + 2);
    }
    char[] newData = new char[newCount];
    System.arraycopy(value, 0, newData, 0, index);
    // index == count case is no-op
    System.arraycopy(value, index, newData, index + size, count - index);
    value = newData;
    shared = false;
  }

  final void replace0(int start, int end, String string) {
    if (end > count) {
      end = count;
    }

    checkStringBounds(start, end, count);

    if (start == end) {
      checkNotNull(string);
      insert0(start, string);
      return;
    }

    int stringLength = string.length();
    int diff = end - start - stringLength;
    if (diff > 0) { // replacing with fewer characters
      if (!shared) {
        // index == count case is no-op
        System.arraycopy(value, end, value, start + stringLength, count - end);
      } else {
        char[] newData = new char[value.length];
        System.arraycopy(value, 0, newData, 0, start);
        // index == count case is no-op
        System.arraycopy(value, end, newData, start + stringLength, count - end);
        value = newData;
        shared = false;
      }
    } else if (diff < 0) {
      // replacing with more characters...need some room
      move(-diff, end);
    } else if (shared) {
      value = Arrays.copyOf(value, value.length);
      shared = false;
    }
    string.getChars0(0, stringLength, value, start);
    count -= diff;
  }

  final void reverse0() {
    if (count < 2) {
      return;
    }
    if (!shared) {
      int end = count - 1;
      char frontHigh = value[0];
      char endLow = value[end];
      boolean allowFrontSur = true, allowEndSur = true;
      for (int i = 0, mid = count / 2; i < mid; i++, --end) {
        char frontLow = value[i + 1];
        char endHigh = value[end - 1];
        boolean surAtFront =
            allowFrontSur
                && frontLow >= 0xdc00
                && frontLow <= 0xdfff
                && frontHigh >= 0xd800
                && frontHigh <= 0xdbff;
        if (surAtFront && (count < 3)) {
          return;
        }
        boolean surAtEnd =
            allowEndSur
                && endHigh >= 0xd800
                && endHigh <= 0xdbff
                && endLow >= 0xdc00
                && endLow <= 0xdfff;
        allowFrontSur = allowEndSur = true;
        if (surAtFront == surAtEnd) {
          if (surAtFront) {
            // both surrogates
            value[end] = frontLow;
            value[end - 1] = frontHigh;
            value[i] = endHigh;
            value[i + 1] = endLow;
            frontHigh = value[i + 2];
            endLow = value[end - 2];
            i++;
            end--;
          } else {
            // neither surrogates
            value[end] = frontHigh;
            value[i] = endLow;
            frontHigh = frontLow;
            endLow = endHigh;
          }
        } else {
          if (surAtFront) {
            // surrogate only at the front
            value[end] = frontLow;
            value[i] = endLow;
            endLow = endHigh;
            allowFrontSur = false;
          } else {
            // surrogate only at the end
            value[end] = frontHigh;
            value[i] = endHigh;
            frontHigh = frontLow;
            allowEndSur = false;
          }
        }
      }
      if ((count & 1) == 1 && (!allowFrontSur || !allowEndSur)) {
        value[end] = allowFrontSur ? endLow : frontHigh;
      }
    } else {
      char[] newData = new char[value.length];
      for (int i = 0, end = count; i < count; i++) {
        char high = value[i];
        if ((i + 1) < count && high >= 0xd800 && high <= 0xdbff) {
          char low = value[i + 1];
          if (low >= 0xdc00 && low <= 0xdfff) {
            newData[--end] = low;
            i++;
          }
        }
        newData[--end] = high;
      }
      value = newData;
      shared = false;
    }
  }

  public void setCharAt(int index, char ch) {
    checkStringElementIndex(index, count);
    if (shared) {
      value = Arrays.copyOf(value, value.length);
      shared = false;
    }
    value[index] = ch;
  }

  public void setLength(int length) {
    checkArgument(length >= 0);
    if (length > value.length) {
      enlargeBuffer(length);
    } else {
      if (shared) {
        char[] newData = new char[value.length];
        System.arraycopy(value, 0, newData, 0, count);
        value = newData;
        shared = false;
      } else {
        if (count < length) {
          Arrays.fill(value, count, length, (char) 0);
        }
      }
    }
    count = length;
  }

  public String substring(int start) {
    checkStringElementIndex(start, count + 1);
    if (start == count) {
      return "";
    }
    // Remove String sharing for more performance
    return new String(value, start, count - start);
  }

  public String substring(int start, int end) {
    checkStringBounds(start, end, count);

    if (start == end) {
      return "";
    }
    // Remove String sharing for more performance
    return new String(value, start, end - start);
  }

  @Override
  public String toString() {
    if (count == 0) {
      return "";
    }
    return new String(value, 0, count);
  }

  public CharSequence subSequence(int start, int end) {
    return substring(start, end);
  }

  public int indexOf(String string) {
    return indexOf(string, 0);
  }

  public int indexOf(String subString, int start) {
    if (start < 0) {
      start = 0;
    }
    int subCount = subString.length();
    if (subCount > 0) {
      if (subCount + start > count) {
        return -1;
      }
      // TODO optimize charAt to direct array access
      char firstChar = subString.charAt(0);
      while (true) {
        int i = start;
        boolean found = false;
        for (; i < count; i++) {
          if (value[i] == firstChar) {
            found = true;
            break;
          }
        }
        if (!found || subCount + i > count) {
          return -1; // handles subCount > count || start >= count
        }
        int o1 = i, o2 = 0;
        while (++o2 < subCount && value[++o1] == subString.charAt(o2)) {
          // Intentionally empty
        }
        if (o2 == subCount) {
          return i;
        }
        start = i + 1;
      }
    }
    return (start < count || start == 0) ? start : count;
  }

  public int lastIndexOf(String string) {
    return lastIndexOf(string, count);
  }

  public int lastIndexOf(String subString, int start) {
    int subCount = subString.length();
    if (subCount <= count && start >= 0) {
      if (subCount > 0) {
        if (start > count - subCount) {
          start = count - subCount; // count and subCount are both
        }
        // >= 1
        // TODO optimize charAt to direct array access
        char firstChar = subString.charAt(0);
        while (true) {
          int i = start;
          boolean found = false;
          for (; i >= 0; --i) {
            if (value[i] == firstChar) {
              found = true;
              break;
            }
          }
          if (!found) {
            return -1;
          }
          int o1 = i, o2 = 0;
          while (++o2 < subCount && value[++o1] == subString.charAt(o2)) {
            // Intentionally empty
          }
          if (o2 == subCount) {
            return i;
          }
          start = i - 1;
        }
      }
      return start < count ? start : count;
    }
    return -1;
  }

  public void trimToSize() {
    if (count < value.length) {
      char[] newValue = new char[count];
      System.arraycopy(value, 0, newValue, 0, count);
      value = newValue;
      shared = false;
    }
  }
}
