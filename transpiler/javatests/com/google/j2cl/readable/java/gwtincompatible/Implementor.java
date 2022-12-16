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
package gwtincompatible;

public class Implementor extends Interface.ClassWithAbstractMethod
    implements Interface, Interface.NestedInterface {
  @GwtIncompatible
  @Override
  public void incompatible() {}

  @GwtIncompatible
  @Override
  public void nestedIncompatible() {}

  @GwtIncompatible
  @Override
  public void incompatibleFromClass() {}

  public void compatibleMethod() {
    int a = 4; // Show that the source map line numbers are preserved correctly.
  }

  enum SomeEnum {
    COMPATIBLE {
      @Override
      void method() {}
    },
    @GwtIncompatible
    INCOMPATIBLE {
      @Override
      void method() {}
    };

    abstract void method();
  }
}
