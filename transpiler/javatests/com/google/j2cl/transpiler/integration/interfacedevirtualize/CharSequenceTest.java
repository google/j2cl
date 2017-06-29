/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.transpiler.integration.interfacedevirtualize;

/**
 * Test CharSequence Interface on all devirtualized classes that implement it.
 */
public class CharSequenceTest {
  
  public static void test() {
    final String s = "string";
    CharSequence cs =
        new CharSequence() {
          @Override
          public int length() {
            return s.length();
          }

          @Override
          public char charAt(int i) {
            return s.charAt(i);
          }

          @Override
          public CharSequence subSequence(int i, int j) {
            return s.subSequence(i, j);
          }

          @Override
          public boolean equals(Object c) {
            return s.equals(c);
          }

          @Override
          public int hashCode() {
            return s.hashCode();
          }

          @Override
          public String toString() {
            return s.toString();
          }
        };

    assert s.length() == 6;
    assert cs.length() == 6;

    assert s.charAt(1) == 't';
    assert cs.charAt(1) == 't';

    assert s.subSequence(0, 2).equals("st");
    assert cs.subSequence(0, 2).equals("st");

    assert s.equals("string");
    assert cs.equals("string");

    assert s.hashCode() == "string".hashCode();
    assert cs.hashCode() == "string".hashCode();

    assert s.toString().equals("string");
    assert cs.toString().equals("string");

    assert s.getClass() == String.class;
    assert cs.getClass() != String.class;
  }

  public static void testViaSuper() {
    CharSequence cs =
        new CharSequence() {
          @Override
          public int length() {
            return 0;
          }

          @Override
          public char charAt(int i) {
            return 0;
          }

          @Override
          public CharSequence subSequence(int i, int j) {
            return null;
          }

          @Override
          public boolean equals(Object c) {
            return super.equals(c);
          }

          @Override
          public int hashCode() {
            return super.hashCode();
          }

          @Override
          public String toString() {
            return "sub" + super.toString();
          }
        };

    assert cs.equals(cs);
    assert !cs.equals("string");

    assert cs.hashCode() == System.identityHashCode(cs);
    assert cs.hashCode() != 0;

    assert cs.toString().startsWith("sub");
  }
}

