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
package kotlinjavainterop;


class JavaClass {
  public static int staticField = 0;
  public int instanceField = 1;
  private int privateFieldWithAccessors = 2;

  public void setPrivateFieldWithAccessors(int privateFieldWithAccessors) {
    this.privateFieldWithAccessors = privateFieldWithAccessors;
  }

  public int getPrivateFieldWithAccessors() {
    return this.privateFieldWithAccessors;
  }

  @SuppressWarnings("unchecked")
  public <T, U, V> U genericMethod(Object v) {
    return (U) v;
  }

  public static String staticMethod(String value) {
    return "JavaClass.staticMethod(" + value + ")";
  }

  public String instanceMethod(String value) {
    return "JavaClass.instanceMethod(" + value + ")";
  }
}
