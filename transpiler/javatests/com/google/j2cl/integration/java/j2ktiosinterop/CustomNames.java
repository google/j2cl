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

  @ObjectiveCName("customMethod")
  public void method() {}

  @ObjectiveCName("customIntMethodWithInt:")
  public void intMethod(int i) {}

  @ObjectiveCName("customIndexMethodWithIndex:")
  public void indexMethod(int i) {}

  @ObjectiveCName("customCountMethodWithCount:")
  public void countMethod(int i) {}

  @ObjectiveCName("customStringMethodWithString:")
  public void stringMethod(String s) {}

  @ObjectiveCName("customNameMethodWithName:")
  public void nameMethod(String s) {}

  @ObjectiveCName("customIntStringMethodWithIndex:name:")
  public void intStringMethod(int i, String s) {}

  @ObjectiveCName("customLongMethod")
  public void longMethod(long i) {}

  @ObjectiveCName("customLongStringMethod")
  public void longStringMethod(long i, String s) {}

  @ObjectiveCName("customStaticMethod")
  public static void staticMethod() {}

  @ObjectiveCName("customStaticIntMethodWithIndex:")
  public static void staticIntMethod(int i) {}

  @ObjectiveCName("customStaticIntStringMethodWithIndex:name:")
  public static void staticIntStringMethod(int i, String s) {}

  @ObjectiveCName("customStaticLongMethod")
  public static void staticLongMethod(long i) {}

  @ObjectiveCName("customStaticLongStringMethod")
  public static void staticLongStringMethod(long i, String s) {}
}
