/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal;

final class Ref {
  static <T> OfObject<T> createRef(T initializer) {
    return new OfObject<T>(initializer);
  }

  static class OfObject<T> {
    T element;

    OfObject(T element) {
      this.element = element;
    }
  }

  static OfByte createRef(byte initializer) {
    return new OfByte(initializer);
  }

  static class OfByte {
    byte element;

    OfByte(byte element) {
      this.element = element;
    }
  }

  static OfShort createRef(short initializer) {
    return new OfShort(initializer);
  }

  static class OfShort {
    short element;

    OfShort(short element) {
      this.element = element;
    }
  }

  static OfInt createRef(int initializer) {
    return new OfInt(initializer);
  }

  static class OfInt {
    int element;

    OfInt(int element) {
      this.element = element;
    }
  }

  static OfLong createRef(long initializer) {
    return new OfLong(initializer);
  }

  static class OfLong {
    long element;

    OfLong(long element) {
      this.element = element;
    }
  }

  static OfFloat createRef(float initializer) {
    return new OfFloat(initializer);
  }

  static class OfFloat {
    float element;

    OfFloat(float element) {
      this.element = element;
    }
  }

  static OfDouble createRef(double initializer) {
    return new OfDouble(initializer);
  }

  static class OfDouble {
    double element;

    OfDouble(double element) {
      this.element = element;
    }
  }

  static OfChar createRef(char initializer) {
    return new OfChar(initializer);
  }

  static class OfChar {
    char element;

    OfChar(char element) {
      this.element = element;
    }
  }

  static OfBoolean createRef(boolean initializer) {
    return new OfBoolean(initializer);
  }

  static class OfBoolean {
    boolean element;

    OfBoolean(boolean element) {
      this.element = element;
    }
  }

  private Ref() {}
}
