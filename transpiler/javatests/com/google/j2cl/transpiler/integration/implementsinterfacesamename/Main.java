/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.transpiler.integration.implementsinterfacesamename;

/**
 * A class which implements a generic interface with the same name.
 */
public class Main
    implements com.google.j2cl.transpiler.integration.implementsinterfacesamename.foo.Main<String> {
  public static void main(String... args) {
    // Tests that implementers of generic interfaces are correctly marked as such.
    assert new Main()
        instanceof com.google.j2cl.transpiler.integration.implementsinterfacesamename.foo.Main;
  }
}
