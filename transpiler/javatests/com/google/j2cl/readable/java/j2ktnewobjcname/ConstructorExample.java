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
package j2ktnewobjcname;

import com.google.j2objc.annotations.ObjectiveCName;

public class ConstructorExample {
  public static class NotAnnotated {
    public NotAnnotated() {}

    public NotAnnotated(int i) {}

    public NotAnnotated(int i, String s) {}

    public NotAnnotated(int i, String s, Object id) {}
  }

  public static class ExplicitParams {
    @ObjectiveCName("init")
    public ExplicitParams() {}

    @ObjectiveCName("initWithIndex:")
    public ExplicitParams(int i) {}

    @ObjectiveCName("initWithIndex:withName:")
    public ExplicitParams(int i, String s) {}

    @ObjectiveCName("initWithIndex:withName:withObject:")
    public ExplicitParams(int i, String s, Object id) {}
  }

  // Overloading constructors with implicit Obj-C params is not legal as it leads to name conflicts
  // between "C" functions in J2ObjCompat files.
  public static class ImplicitParams {
    @ObjectiveCName("init1")
    public ImplicitParams() {}

    @ObjectiveCName("init2")
    public ImplicitParams(int i) {}

    @ObjectiveCName("init3")
    public ImplicitParams(int i, String s) {}

    @ObjectiveCName("init4")
    public ImplicitParams(int i, String s, Object id) {}
  }
}
