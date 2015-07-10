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
package com.google.j2cl.transpiler.integration.instanceofarray;

/**
 * Test instanceof array.
 */
public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    Object object = new Object();
    assert object instanceof Object;
    assert !(object instanceof Object[]);
    assert !(object instanceof Object[][]);
    assert !(object instanceof String[]);
    assert !(object instanceof String[][]);

    Object objects1d = new Object[1];
    assert objects1d instanceof Object;
    assert objects1d instanceof Object[];
    assert !(objects1d instanceof Object[][]);
    assert !(objects1d instanceof String[]);
    assert !(objects1d instanceof String[][]);

    Object strings1d = new String[1];
    assert strings1d instanceof Object;
    assert strings1d instanceof Object[];
    assert !(strings1d instanceof Object[][]);
    assert strings1d instanceof String[];
    assert !(strings1d instanceof String[][]);

    Object objects2d = new Object[1][1];
    assert objects2d instanceof Object;
    assert objects2d instanceof Object[];
    assert objects2d instanceof Object[][];
    assert !(objects2d instanceof String[]);
    assert !(objects2d instanceof String[][]);

    Object strings2d = new String[1][1];
    assert strings2d instanceof Object;
    assert strings2d instanceof Object[];
    assert strings2d instanceof Object[][];
    assert !(strings2d instanceof String[]);
    assert strings2d instanceof String[][];
  }
}
