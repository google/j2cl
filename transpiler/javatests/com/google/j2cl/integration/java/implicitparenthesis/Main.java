/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package implicitparenthesis;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Inserting unboxing method calls will be mangled if implicit parenthesis are not inserted.
 */
public class Main {

  public static void main(String... args) {
    examples();
    nonExamples();
  }

  private static void examples() {
    int primitiveInt;
    Integer boxedInt;

    // Ternary
    {
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = primitiveInt == 5 ? new Integer(15) : new Integer(30);
      assertTrue(primitiveInt == 15);
    }

    // Binary operator, compound assignment
    {
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = boxedInt += primitiveInt;
      assertTrue(primitiveInt == 15);
    }

    // Binary operator, assignment
    {
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = boxedInt = primitiveInt;
      assertTrue(primitiveInt == 5);
    }

    // Postfix operator
    {
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = boxedInt++;
      assertTrue(primitiveInt == 10);
    }

    // Prefix operator
    {
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = ++boxedInt;
      assertTrue(primitiveInt == 11);
    }
  }

  private static void nonExamples() {
    int primitiveInt;
    Integer boxedInt;
    Boolean boxedBoolean;

    // Binary operator, arithmetic
    {
      // The arithmetic input and output must already be in primitive form so no boxing needs to be
      // inserted before the assignment.
      primitiveInt = 5;
      boxedInt = new Integer(10);
      primitiveInt = boxedInt - primitiveInt;
      assertTrue(primitiveInt == 5);
    }

    // Binary operator, comparison.
    {
      // Boolean is devirtualized so this can't really fail, but it's here for completeness.
      primitiveInt = 5;
      boxedInt = new Integer(10);
      boxedBoolean = true;
      boxedBoolean = boxedInt == primitiveInt;
      assertTrue(boxedBoolean == false);
    }
  }
}
