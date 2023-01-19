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

import com.google.j2objc.annotations.ObjectiveCName;

@ObjectiveCName("NewObjCName")
public class ObjCName {

  public ObjCName() {}

  @ObjectiveCName("initWithInteger")
  public ObjCName(int i) {}

  public ObjCName(int i, String s) {}

  @ObjectiveCName("InnerClassNewName")
  public class InnerClassOldName {}

  @ObjectiveCName("newFoo")
  public void foo() {}

  @ObjectiveCName("newProtectedFoo")
  protected void protectedFoo() {}

  @ObjectiveCName("newFooFromInt:")
  public void newFoo(int i) {}

  @ObjectiveCName("newFooFromInt:withInteger:")
  public void foo(String s, int i) {}

  public void foo(String s, String i) {}

  public static void allocFoo() {}

  public static void initFoo() {}

  public static void newFoo() {}

  public static void copyFoo() {}

  public static void mutableCopyFoo() {}

  public static void params(int extern, int struct, int register, int inline) {}

  public enum Foo {
    allocFoo,
    initFoo,
    newFoo,
    copyFoo,
    mutableCopyFoo,
    register,
    struct
  }
}
