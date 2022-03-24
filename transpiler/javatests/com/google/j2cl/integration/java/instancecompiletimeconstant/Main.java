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
package instancecompiletimeconstant;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  private static class Parent {
    public final String parentCompileTimeConstantString = "987";
    public final byte parentCompileTimeConstantByte = 123;
    public final short parentCompileTimeConstantShort = 123;
    public final int parentCompileTimeConstantInt = 123;
    public final long parentCompileTimeConstantLong = 123L;
    public String parentRegularString = "987";
    public int parentRegularInt = 123;

    public Parent() {
      checkFieldsInitialized();
    }

    /**
     * If this check() is called then the control flow was main -> Parent.constructor() ->
     * Parent.checkFieldsInitialized() and as a result both instance fields A and B should already
     * be initialized.
     */
    public void checkFieldsInitialized() {
      assertTrue(parentCompileTimeConstantString != null);
      assertTrue(parentCompileTimeConstantByte != 0);
      assertTrue(parentCompileTimeConstantShort != 0);
      assertTrue(parentCompileTimeConstantInt != 0);
      assertTrue(parentCompileTimeConstantLong != 0L);

      // Parent's non compile time constant fields *have* been initialized because of the time at
      // which check was called!
      assertTrue(parentRegularString != null);
      assertTrue(parentRegularInt != 0);
    }
  }

  private static class Child extends Parent {
    public final float childCompileTimeConstantFloat = 123;
    public final double childCompileTimeConstantDouble = 123;
    public final char childCompileTimeConstantChar = 123;
    public final boolean childCompileTimeConstantBoolean = true;
    public String childRegularString = "987";
    public int childRegularInt = 123;

    public Child() {
      super();
    }

    /**
     * If this check() is called then the control flow was main -> Child.constructor()->
     * Parent.constructor() -> Child.checkFieldsInitialized() and as a result both of Parent
     * instance fields A and B should already be initialized BUT only final Child instance fields
     * should be initialized.
     */
    @Override
    public void checkFieldsInitialized() {
      super.checkFieldsInitialized();

      assertTrue(childCompileTimeConstantFloat != 0);
      assertTrue(childCompileTimeConstantDouble != 0);
      assertTrue(childCompileTimeConstantChar != 0);
      assertTrue(childCompileTimeConstantBoolean != false);

      // Child's non compile time constant fields have not been initialized!
      assertTrue(childRegularString == null);
      assertTrue(childRegularInt == 0);
    }
  }

  @SuppressWarnings("unused")
  public static void main(String... args) {
    Child child = new Child();
  }
}
