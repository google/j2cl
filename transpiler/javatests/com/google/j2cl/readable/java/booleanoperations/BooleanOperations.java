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
package booleanoperations;

public class BooleanOperations {
  @SuppressWarnings("unused")
  private static void acceptBoolean(boolean b) {
    // does nothing
  }

  @SuppressWarnings({"unused", "cast"})
  public void test() {
    boolean ls = true;
    boolean rs = true;
    boolean r = true;
    boolean t = !!!true;

    // Assignment.
    r = ls == rs;
    r = ls != rs;
    r = ls ^ rs;
    r = ls & rs;
    r = ls | rs;
    r = ls && rs;
    r = ls || rs;
    r = ls = rs;

    // Compound assignment.
    r ^= rs;
    r &= rs;
    r |= rs;

    // Method invocation.
    acceptBoolean(ls == rs);
    acceptBoolean(ls != rs);
    acceptBoolean(ls ^ rs);
    acceptBoolean(ls & rs);
    acceptBoolean(ls | rs);
    acceptBoolean(ls && rs);
    acceptBoolean(ls || rs);
    acceptBoolean(ls = rs);

    // Cast.
    Boolean br;
    br = (Boolean) (ls == rs);
    br = (Boolean) (ls != rs);
    br = (Boolean) (ls ^ rs);
    br = (Boolean) (ls & rs);
    br = (Boolean) (ls | rs);
    br = (Boolean) (ls && rs);
    br = (Boolean) (ls || rs);
    br = (Boolean) (ls = rs);

    // Conditional
    if (ls == rs) {
      r = true;
    }
    if (ls != rs) {
      r = true;
    }
    if (ls ^ rs) {
      r = true;
    }
    if (ls & rs) {
      r = true;
    }
    if (ls | rs) {
      r = true;
    }
    if (ls && rs) {
      r = true;
    }
    if (ls || rs) {
      r = true;
    }

    // Compound assignment with enclosing instance.
    class Outer {
      boolean b;

      class Inner {
        {
          b |= true;
        }
      }
    }
    final Outer finalOuter = new Outer();
    finalOuter.b |= true;

    Outer outer = new Outer();
    outer.b |= (outer = null) == null;

  }
}
