/*
 * Copyright 2024 Google Inc.
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
package j2ktiosinterop;

import com.google.j2objc.annotations.ObjectiveCName;

@ObjectiveCName("Custom")
public final class CustomNames {
  @ObjectiveCName("initWithIndex:")
  public CustomNames(int i) {}

  @ObjectiveCName("initWithIndex:name:")
  public CustomNames(int i, String s) {}

  @ObjectiveCName("init")
  public CustomNames() {}

  @ObjectiveCName("init2")
  public CustomNames(long i) {}

  @ObjectiveCName("init3")
  public CustomNames(long i, String s) {}

  @ObjectiveCName("custom")
  public void method() {}

  @ObjectiveCName("customWithIndex:")
  public void method(int i) {}

  @ObjectiveCName("customWithIndex:name:")
  public void method(int i, String s) {}

  @ObjectiveCName("custom")
  public void method(long i) {}

  @ObjectiveCName("custom")
  public void method(long i, String s) {}

  @ObjectiveCName("staticCustom")
  public static void staticMethod() {}

  @ObjectiveCName("staticCustomWithIndex:")
  public static void staticMethod(int i) {}

  @ObjectiveCName("staticCustomWithIndex:name:")
  public static void staticMethod(int i, String s) {}

  @ObjectiveCName("staticCustom2")
  public static void staticMethod(long i) {}

  @ObjectiveCName("staticCustom3")
  public static void staticMethod(long i, String s) {}
}
