/*
 * Copyright 2022 Google Inc.
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
package j2kt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class AccidentalOverride {
  abstract class SuperClass {
    public void accidentalOverride() {}
  }

  interface Interface {
    default void accidentalOverride() {}

    default void abstractAccidentalOverride() {}
  }

  class TestImplicitOverride extends SuperClass implements Interface {
    // Kotlin needs explicit override to resolve conflicting method.
  }

  class TestExplicitOverride extends SuperClass implements Interface {
    @Override
    public void accidentalOverride() {
      super.accidentalOverride();
    }
  }

  abstract class SubClass extends SuperClass {
    public abstract void abstractAccidentalOverride();
  }

  abstract class TestImplicitOverrideInAbstractClass extends SubClass implements Interface {
    // Kotlin needs explicit abstract override to resolve conflicting method.
  }

  abstract class TestExplicitOverrideInAbstractClass extends SubClass implements Interface {
    @Override
    public void accidentalOverride() {
      super.accidentalOverride();
    }
  }
}

@NullMarked
interface Parent {
  <T extends String> void test(T t);
}

@NullMarked
interface Child extends Parent {
  // TODO(b/395588204): Uncomment when fixed.
  // This method is valid override in Java, but in Kotlin it's accidental override.
  // @Override
  // void test(String s);
}
