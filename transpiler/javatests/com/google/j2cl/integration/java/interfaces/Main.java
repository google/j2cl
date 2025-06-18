/*
 * Copyright 2017 Google Inc.
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
package interfaces;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

import com.google.j2objc.annotations.ObjectiveCName;
import interfaces.package1.ChildInPackage1;
import interfaces.package1.ClassInPackage1WithPackagePrivateMethod;
import interfaces.package1.InterfaceInPackage1;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import jsinterop.annotations.JsNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Test basic interface functionality. */
@SuppressWarnings("StaticQualifiedUsingExpression")
@NullMarked
public class Main {
  @Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  @interface J2ktIncompatible {}

  public static void main(String... args) {
    testInterfaceDispatch();
    testInterfaceWithFields();
    testDefaultMethods();
    testDefaultMethods_superCall();
    testDefaultMethods_diamondProperty();
    testDefaultMethods_packagePrivate();
    testStaticMethods();
    testPrivateMethods();
    testAccidentalOverrides();
    testCallWithDifferentNullMarking();
  }

  public interface SomeInterface {
    int run();
  }

  private static class SomeClass implements SomeInterface {
    @Override
    public int run() {
      return 1;
    }
  }

  public static int run(SomeInterface someInterface) {
    return someInterface.run();
  }

  private static void testInterfaceDispatch() {
    SomeClass s = new SomeClass();
    assertTrue(run(s) == 1);
  }

  public interface InterfaceWithFields {
    public int A = 1;
    public static int B = 2;
  }

  private static void testInterfaceWithFields() {
    assertTrue(InterfaceWithFields.A == 1);
    assertTrue(InterfaceWithFields.B == 2);
    InterfaceWithFields i = new InterfaceWithFields() {};
    assertTrue(i.A == 1);
    assertTrue(i.B == 2);
  }

  public static final int COLLECTION_ADD = 1;
  public static final int LIST_ADD = 2;
  public static final int ABSTRACT_COLLECTION_ADD = 3;
  public static final int ANOTHER_STRING_LIST_ADD = 4;
  public static final int ANOTHER_LIST_INTERFACE_ADD = 5;

  interface Collection<T extends @Nullable Object> {
    default int add(T elem) {
      assertTrue(this instanceof Collection);
      return COLLECTION_ADD;
    }
  }

  interface List<T extends @Nullable Object> extends Collection<T> {
    @Override
    default int add(T elem) {
      assertTrue(this instanceof List);
      return LIST_ADD;
    }

    static int returnOne() {
      return 1;
    }
  }

  abstract static class AbstractCollection<T extends @Nullable Object> implements Collection<T> {
    @Override
    public int add(T elem) {
      assertTrue(this instanceof AbstractCollection);
      return ABSTRACT_COLLECTION_ADD;
    }
  }

  static class ACollection<T extends @Nullable Object> implements Collection<T> {}

  abstract static class AbstractList<T extends @Nullable Object> extends AbstractCollection<T>
      implements List<T> {}

  static class AConcreteList<T extends @Nullable Object> extends AbstractList<T> {}

  static class SomeOtherCollection<T extends @Nullable Object> implements Collection<T> {}

  static class SomeOtherList<T extends @Nullable Object> extends SomeOtherCollection<T>
      implements List<T> {}

  // Should inherit List.add  even though the interface is not directly declared.
  static class YetAnotherList<T extends @Nullable Object> extends SomeOtherList<T>
      implements Collection<T> {}

  static class StringList implements List<@Nullable String> {}

  static class YetAnotherStringList extends YetAnotherList<@Nullable String> {}

  static class AnotherStringList implements List<@Nullable String> {
    @Override
    public int add(@Nullable String elem) {
      return ANOTHER_STRING_LIST_ADD;
    }
  }

  interface AnotherListInterface<T extends @Nullable Object> {
    default int add(T elem) {
      assertTrue(this instanceof AnotherListInterface);
      return ANOTHER_LIST_INTERFACE_ADD;
    }
  }

  static class AnotherCollection<T extends @Nullable Object>
      implements List<T>, AnotherListInterface<T> {
    @Override
    public int add(T elem) {
      return AnotherListInterface.super.add(elem);
    }
  }

  abstract static class AbstractCollectionWithDefaults<T extends @Nullable Object>
      implements Collection<T> {}

  static final class FinalCollection<T extends @Nullable Object>
      extends AbstractCollectionWithDefaults<T> {}

  private static void testDefaultMethods() {
    assertTrue(new ACollection<@Nullable Object>().add(null) == COLLECTION_ADD);
    assertTrue(new AConcreteList<@Nullable Object>().add(null) == ABSTRACT_COLLECTION_ADD);
    assertTrue(new SomeOtherCollection<@Nullable Object>().add(null) == COLLECTION_ADD);
    assertTrue(new SomeOtherList<@Nullable Object>().add(null) == LIST_ADD);
    assertTrue(new YetAnotherList<@Nullable Object>().add(null) == LIST_ADD);
    assertTrue(new StringList().add(null) == LIST_ADD);
    assertTrue(new YetAnotherStringList().add(null) == LIST_ADD);
    assertTrue(new AnotherStringList().add(null) == ANOTHER_STRING_LIST_ADD);
    assertTrue(new AnotherCollection<@Nullable Object>().add(null) == ANOTHER_LIST_INTERFACE_ADD);
    assertTrue(new FinalCollection<@Nullable Object>().add(null) == COLLECTION_ADD);
  }

  private static void testStaticMethods() {
    assertTrue(List.returnOne() == 1);
  }

  interface InterfaceWithPrivateMethods {
    default int defaultMethod() {
      return privateInstanceMethod();
    }

    private int privateInstanceMethod() {
      return m();
    }

    private static int privateStaticMethod() {
      return mReturns1.privateInstanceMethod() + 1;
    }

    int m();

    static int callPrivateStaticMethod() {
      return privateStaticMethod();
    }
  }

  private static InterfaceWithPrivateMethods mReturns1 =
      new InterfaceWithPrivateMethods() {
        @Override
        public int m() {
          return 1;
        }
      };

  private static void testPrivateMethods() {
    assertTrue(InterfaceWithPrivateMethods.callPrivateStaticMethod() == 2);
    assertTrue(mReturns1.defaultMethod() == 1);
  }

  private abstract static class AbstractClassWithFinalMethod {
    public final int run() {
      return 2;
    }
  }

  private static class ChildClassWithFinalMethod extends AbstractClassWithFinalMethod
      implements SomeInterface {}

  private static void testAccidentalOverrides() {
    ChildClassWithFinalMethod c = new ChildClassWithFinalMethod();
    AbstractClassWithFinalMethod a = c;
    assertTrue(run(c) == 2);
    assertTrue(c.run() == 2);
    assertTrue(a.run() == 2);
  }

  interface InterfaceWithDefaultMethod {
    default String defaultMethod() {
      return "default-method";
    }
  }

  private static void testDefaultMethods_superCall() {
    abstract class AbstractClass implements InterfaceWithDefaultMethod {}

    class SubClass extends AbstractClass {
      public String defaultMethod() {
        return super.defaultMethod();
      }
    }

    assertEquals("default-method", new SubClass().defaultMethod());
  }

  interface DiamondLeft<T extends DiamondLeft<T>> {
    String NAME = "DiamondLeft";

    // TODO(b/424430558): Explicit annotation is necessary to overcome problem with @ObjCName
    //  annotation and diamond types.
    @ObjectiveCName("nameWith:")
    default String name(@Nullable T t) {
      return NAME;
    }
  }

  interface DiamondRight<T extends DiamondRight<T>> {
    String NAME = "DiamondRight";

    // TODO(b/424430558): Explicit annotation is necessary to overcome problem with @ObjCName
    //  annotation and diamond types.
    @ObjectiveCName("nameWith:")
    default String name(@Nullable T t) {
      return NAME;
    }
  }

  interface Bottom<T extends Bottom<T>> extends DiamondLeft<T>, DiamondRight<T> {
    String NAME = "Bottom";

    @Override
    default String name(@Nullable T t) {
      return NAME;
    }
  }

  static class A<T extends DiamondLeft<T>, V extends DiamondRight<V>>
      implements DiamondLeft<T>, DiamondRight<V> {}

  static class B extends A<B, B> implements Bottom<B> {}

  static class C implements Bottom<C> {
    static String NAME = "C";

    @Override
    public String name(@Nullable C c) {
      return NAME;
    }
  }

  // TODO(b/424430558): J2KT inserts invalid qualifier projection cast:
  //  {@code (dl as DiamondLeft<DiamondLeft<T>>).name(null)}.
  @J2ktIncompatible
  private static void testDefaultMethods_diamondProperty() {
    A<? extends DiamondLeft<?>, ? extends DiamondRight<?>> a = new A<>();
    DiamondLeft<?> dl = a;
    assertEquals(DiamondLeft.NAME, dl.name(null));
    DiamondRight<?> dr = a;
    assertEquals(DiamondRight.NAME, dr.name(null));

    a = new B();
    dl = a;
    assertEquals(Bottom.NAME, dl.name(null));
    dr = a;
    assertEquals(Bottom.NAME, dr.name(null));

    C c = new C();
    dl = c;
    assertEquals(C.NAME, dl.name(null));
    dr = c;
    assertEquals(C.NAME, dr.name(null));
    assertEquals(C.NAME, c.name(null));
  }

  // Fallback for J2kt, which will be called when the method above is stripped out.
  private static void testDefaultMethods_diamondProperty(Object... args) {
    if (!isJ2Kt()) {
      throw new AssertionError();
    }
  }

  abstract static class NullableIterator<E extends @Nullable Object> implements Iterator<E> {}

  private static void testCallWithDifferentNullMarking() {
    NullableIterator<@Nullable String> x =
        new NullableIterator<>() {
          public boolean hasNext() {
            return false;
          }

          public @Nullable String next() {
            return null;
          }
        };
    assertFalse(x.hasNext());

    NullableIterator<@JsNonNull String> x2 = x;
    assertFalse(x2.hasNext());
  }

  private static class SubclassInDifferentPackage extends ClassInPackage1WithPackagePrivateMethod
      implements InterfaceInPackage1 {
    // Since this class is declared in a different package, the package private method
    // ClassInPackage1WithPackagePrivateMethod.m() is not seen when declaring it and the default
    // method from InterfaceInPackage1 is the implementation inherited; even though
    // InterfaceInPackage1 is, as its name says, in the same package.
  }

  private static void testDefaultMethods_packagePrivate() {
    InterfaceInPackage1 i = new ChildInPackage1();
    assertEquals("explicit-override-in-child", i.m());

    // TODO(b/259713749): Remove JVM exclusion when bug is fixed.
    if (!isJvm()) {
      // This tests ends up with an IllegalAccessError at runtime which means that the code
      // is compiled incorrectly or that the way default methods are implemented in the VM does
      // not allow for such scenario.
      i = new SubclassInDifferentPackage();
      assertEquals("default-method-in-package1-m", i.m());
    }
  }
}
