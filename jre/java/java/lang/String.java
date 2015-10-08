/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/String.html">the
 * official Java API doc</a> for details.
 * TODO: implements Comparable, CharSequence
 */
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {

  // Object overrides

  @Override
  public boolean equals(Object o) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  @Override
  public int hashCode() {
    // Super-source replaced.
    throw new RuntimeException();
  }

  @Override
  public String toString() {
    // Super-source replaced.
    throw new RuntimeException();
  }

  // Char Sequence Implementation

  public int length() {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public char charAt(int index) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public CharSequence subSequence(int start, int end) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  // Comparable Implementation

  public int compareTo(String s) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  // (Subset of) Public Methods Implementation

  public static String valueOf(Object o) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public String substring(int start, int endIndex) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public String substring(int start) {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public String trim() {
    // Super-source replaced.
    throw new RuntimeException();
  }
}
