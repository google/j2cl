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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

@ObjectiveCName("NewObjCName")
public class ObjCName {

  // Instance fields.
  public int publicField;
  protected int protectedField;
  int packagePrivateField;
  private int privateField;

  // Static fields.
  public static int publicStaticField;
  protected static int protectedStaticField;
  static int packagePrivateStaticField;
  private static int privateStaticField;

  // Fields with reference types.
  public static Object objectField;
  public static String stringField;
  public static ObjCName selfField;
  public static Iterable<String> iterable;
  public static Iterator<String> iterator;
  public static Collection collection;

  // Fields with reserved names.
  public static int alloc;
  public static int init;
  public static int extern;
  public static int inline;
  public static int NULL;

  public ObjCName() {}

  @ObjectiveCName("initWithInteger")
  public ObjCName(int i) {}

  public ObjCName(int i, String s) {}

  @ObjectiveCName("InnerClassNewName")
  public class InnerClassOldName {}

  @ObjectiveCName("newFoo")
  public static void foo() {}

  @ObjectiveCName("newProtectedFoo")
  protected static void protectedFoo() {}

  @ObjectiveCName("newFooFromInt:")
  public static void newFoo(int i) {}

  @ObjectiveCName("newFooFromInt:withInteger:")
  public static void foo(String s, int i) {}

  public static void foo(String s, String i) {}

  public static void alloc() {}

  public static void allocFoo() {}

  public static void allocatedFoo() {}

  public static void initFoo() {}

  public static void newFoo() {}

  public static void copyFoo() {}

  public static void mutableCopyFoo() {}

  public static void reservedParamNames(int extern, int struct, int register, int inline) {}

  public static String nativeTypes(String s, Object o, Number n) {
    return s;
  }

  public static void iterableType(Iterable<String> l) {}

  public static void iteratorType(Iterator<String> l) {}

  public static void listIteratorType(ListIterator<String> l) {}

  public static void collectionType(Collection<String> l) {}

  public static void listType(List<String> l) {}

  public static void setType(Set<String> l) {}

  public static void mapType(Map<String, String> l) {}

  public static void linkedListType(LinkedList<String> l) {}

  public static void hashSetType(HashSet<String> l) {}

  public static void hashMapType(HashMap<String, String> l) {}

  public enum Foo {
    allocFoo,
    initFoo,
    newFoo,
    copyFoo,
    mutableCopyFoo,
    register,
    struct,
    NULL,
    YES,
    NO;

    public static Foo withOrdinal(int ordinal) {
      return Foo.values()[ordinal];
    }
  }
}
