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
package com.google.j2cl.transpiler.integration.assertsimple;

/**
 * Test method body, assert statement, and binary expression
 * with number literals work fine.
 */
public class Main {
  public static void main(String[] args) {
    assert 1 + 2 == 3;

    try {
      assert 2 == 3;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage() == null;
    }

    try {
      assert 2 == 3 : getDescription();
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("custom message");
    }

    try {
      assert 2 == 3 : new RuntimeException("42");
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getCause().getMessage().equals("42");
    }

    try {
      assert 2 == 3 : 42;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("42");
    }

    try {
      assert 2 == 3 : 42L;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("42");
    }

    try {
      assert 2 == 3 : true;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("true");
    }

    try {
      assert 2 == 3 : 'g';
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      // TODO(b/37799560): fix char handling.
      // assert expected.getMessage().equals("g");
    }
  }

  private static Object getDescription() {
    return new Object() {
      @Override
      public String toString() {
        return "custom message";
      }
    };
  }
}
