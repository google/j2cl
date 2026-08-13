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

import com.google.j2objc.annotations.GenerateObjCCompanion;
import com.google.j2objc.annotations.ObjectiveCName;
import com.google.j2objc.annotations.SwiftName;

@ObjectiveCName("Custom")
@SwiftName("CustomSwift")
@GenerateObjCCompanion
public final class CustomNames {
  @ObjectiveCName("initWithIndex:")
  @SwiftName("init(index:)")
  public CustomNames(int i) {}

  @ObjectiveCName("initWithIndex:name:")
  @SwiftName("init(index:name:)")
  public CustomNames(int i, String s) {}

  @ObjectiveCName("init")
  @SwiftName("init()")
  public CustomNames() {}

  @ObjectiveCName("init2")
  @SwiftName("init2(long:)")
  public CustomNames(long i) {}

  @ObjectiveCName("init3")
  @SwiftName("init3(long:with:)")
  public CustomNames(long i, String s) {}

  @ObjectiveCName("customMethod")
  @SwiftName("customMethod()")
  public void method() {}

  @ObjectiveCName("customIntMethodWithInt:")
  @SwiftName("customIntMethod(withInt:)")
  public void intMethod(int i) {}

  @ObjectiveCName("customIndexMethodWithIndex:")
  @SwiftName("customIndexMethod(withIndex:)")
  public void indexMethod(int i) {}

  @ObjectiveCName("customCountMethodWithCount:")
  @SwiftName("customCountMethod(withCount:)")
  public void countMethod(int i) {}

  @ObjectiveCName("customStringMethodWithString:")
  @SwiftName("customStringMethod(withString:)")
  public void stringMethod(String s) {}

  @ObjectiveCName("customNameMethodWithName:")
  @SwiftName("customNameMethod(withName:)")
  public void nameMethod(String s) {}

  @ObjectiveCName("customIntStringMethodWithIndex:name:")
  @SwiftName("customIntStringMethod(withIndex:name:)")
  public void intStringMethod(int i, String s) {}

  @ObjectiveCName("customLongMethod")
  @SwiftName("customLongMethod(withLong:)")
  public void longMethod(long i) {}

  @ObjectiveCName("customLongStringMethod")
  @SwiftName("customLongStringMethod(withLong:with:)")
  public void longStringMethod(long i, String s) {}

  @ObjectiveCName("customCustomNamesMethod")
  @SwiftName("customCustomNamesMethod(with:)")
  public void customNamesMethod(CustomNames c) {}

  @ObjectiveCName("customDefaultNamesMethod")
  @SwiftName("customDefaultNamesMethod(with:)")
  public void defaultNamesMethod(DefaultNames c) {}

  @SwiftName("customSwiftStringMethod(with:)")
  public void swiftStringMethod(String s) {}

  @ObjectiveCName("customObjectiveCStringMethodWithString:")
  public void objectiveCStringMethod(String s) {}

  @ObjectiveCName("customStaticMethod")
  @SwiftName("customStaticMethod()")
  public static void staticMethod() {}

  @ObjectiveCName("customStaticIntMethodWithIndex:")
  @SwiftName("customStaticIntMethod(withIndex:)")
  public static void staticIntMethod(int i) {}

  @ObjectiveCName("customStaticIntStringMethodWithIndex:name:")
  @SwiftName("customStaticIntStringMethod(withIndex:name:)")
  public static void staticIntStringMethod(int i, String s) {}

  @ObjectiveCName("customStaticLongMethod")
  @SwiftName("customStaticLongMethod(withLong:)")
  public static void staticLongMethod(long i) {}

  @ObjectiveCName("customStaticLongStringMethod")
  @SwiftName("customStaticLongStringMethod(withLong:with:)")
  public static void staticLongStringMethod(long i, String s) {}

  @ObjectiveCName("lowercase:")
  @SwiftName("lowercase(_:)")
  public void lowercase(String t) {}

  @ObjectiveCName("staticlowercase:")
  @SwiftName("staticlowercase(_:)")
  public static void staticlowercase(String s) {}

  // Reproduction case for b/516712739
  @ObjectiveCName("eventApiColorIndexFor:")
  public static void toEventApiColorIndex(String eventColor) {}
}
