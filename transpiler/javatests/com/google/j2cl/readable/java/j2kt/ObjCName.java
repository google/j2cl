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
import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

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
  public static int val;
  public static int var;
  public static int fun;
  public static int alloc;
  public static int init;
  public static int extern;
  public static int inline;
  public static int NULL;

  public static int allocFoo;
  public static int initFoo;
  public static int newFoo;

  // Compile-time constants
  public static final boolean BOOLEAN_FALSE = false;
  public static final boolean BOOLEAN_TRUE = true;

  public static final char CHAR = 'a';
  public static final char CHAR_APOSTROPHE = '\'';
  public static final char CHAR_BACKSLASH = '\\';
  public static final char CHAR_UNICODE = 0x1231;

  public static final byte BYTE = 123;
  public static final short SHORT = (short) 123;

  public static final int INT = 123;
  public static final int INT_MIN_VALUE = Integer.MIN_VALUE;
  public static final int INT_MAX_VALUE = Integer.MAX_VALUE;

  public static final long LONG = 123L;
  public static final long LONG_MIN_VALUE = Long.MIN_VALUE;
  public static final long LONG_MAX_VALUE = Long.MAX_VALUE;

  public static final float FLOAT = 123f;
  public static final float FLOAT_ZER0 = 0f;
  public static final float FLOAT_NEGATIVE_ZERO = -0f;
  public static final float FLOAT_NAN = Float.NaN;
  public static final float FLOAT_NEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
  public static final float FLOAT_POSITIVE_INFINITY = Float.POSITIVE_INFINITY;
  public static final float FLOAT_MIN_VALUE = Float.MIN_VALUE;
  public static final float FLOAT_MIN_NORMAL = Float.MIN_NORMAL;
  public static final float FLOAT_MIN_EXPONENT = Float.MIN_EXPONENT;
  public static final float FLOAT_MAX_VALUE = Float.MAX_VALUE;
  public static final float FLOAT_MAX_EXPONENT = Float.MAX_EXPONENT;

  public static final double DOUBLE = 123.0;
  public static final double DOUBLE_ZER0 = 0.0;
  public static final double DOUBLE_NEGATIVE_ZERO = -0.0;
  public static final double DOUBLE_NAN = Double.NaN;
  public static final double DOUBLE_NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
  public static final double DOUBLE_POSITIVE_INFINITY = Double.POSITIVE_INFINITY;
  public static final double DOUBLE_MIN_VALUE = Double.MIN_VALUE;
  public static final double DOUBLE_MIN_NORMAL = Double.MIN_NORMAL;
  public static final double DOUBLE_MIN_EXPONENT = Double.MIN_EXPONENT;
  public static final double DOUBLE_MAX_VALUE = Double.MAX_VALUE;
  public static final double DOUBLE_MAX_EXPONENT = Double.MAX_EXPONENT;

  public static final String STRING = "foo";

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

  public static void val() {}

  public static void var() {}

  public static void fun() {}

  public static void alloc() {}

  public static void allocBar() {}

  public static void allocatedBar() {}

  public static void init() {}

  public static void initialize() {}

  public static void initBar() {}

  public static void newBar() {}

  public static void copyBar() {}

  public static void mutableCopyBar() {}

  public static void reservedParamNames(int extern, int struct, int register, int inline) {}

  public static String nativeTypes(String s, Object o, Number n) {
    return s;
  }

  public static void numberType(Number l) {}

  public static void booleanType(Boolean l) {}

  public static void characterType(Character l) {}

  public static void byteType(Byte l) {}

  public static void shortType(Short l) {}

  public static void integerType(Integer l) {}

  public static void longType(Long l) {}

  public static void floatType(Float l) {}

  public static void doubleType(Double l) {}

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

  public static void nonNativeJavaType(StringJoiner l) {}

  public enum Foo {
    val,
    var,
    fun,
    alloc,
    init,
    initialize,
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

  public static class SubCollection<E> extends AbstractList<E> {
    @Override
    public E get(int index) {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }

    public static void staticMethod() {}

    public static class InnerClass {
      public static void staticMethod() {}
    }
  }

  public static class NonCollection {
    public static void staticMethod() {}
  }
}
