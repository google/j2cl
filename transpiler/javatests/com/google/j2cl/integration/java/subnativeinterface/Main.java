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
package subnativeinterface;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests Java types implementing native JsType interfaces. */
public class Main {
  public static void main(String... args) {
    testImplementation();
    testAccidentalImplementation();
    testGenericImplementation();
    testSubInterfaceImplementation();
    testJsTypeSingleJavaConcreteInterface();
    testJsPropertyBridges();
    testJsPropertyBridgesSubclass();
    testJsPropertyAccidentalOverrideSuperCall();
    testJsPropertyRemovedAccidentalOverrideSuperCall();
    testJsPropertyRemovedAccidentalOverrideReadWrite();
  }

  @JsType(isNative = true, namespace = "subnativeinterface")
  interface MyNativeInterface {
    String getName();

    @JsOverlay
    default String describe() {
      return "MyNativeInterface(" + getName() + ")";
    }
  }

  @JsType(isNative = true, namespace = "subnativeinterface")
  interface MyNativeSubInterface extends MyNativeInterface {}

  @JsType(isNative = true, namespace = "subnativeinterface")
  interface MyNativeInterfaceWithJsMembers {
    @JsMethod(name = "renamed")
    String mightBeRenamed();

    @JsProperty
    String getProperty();

    @JsProperty
    void setProperty(String value);
  }

  @JsType(isNative = true, namespace = "subnativeinterface")
  interface MyNativeGenericInterface<T> {
    T echo(T t);
  }

  static class MyNativeInterfaceImpl implements MyNativeInterface, MyNativeInterfaceWithJsMembers {
    private String property;

    @Override
    public String getName() {
      return "MyNativeInterfaceImpl";
    }

    @Override
    public String mightBeRenamed() {
      return "MyNativeInterfaceImpl.mightBeRenamed";
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public void setProperty(String value) {
      property = value;
    }
  }

  static class MyNativeInterfaceImplSubclass extends MyNativeInterfaceImpl {
    @Override
    public String getName() {
      return "MyNativeInterfaceImplSubclass";
    }
  }

  @JsType
  static class MyNativeSubInterfaceJsTypeImpl implements MyNativeSubInterface {
    @Override
    public String getName() {
      return "MyNativeSubInterfaceJsTypeImpl";
    }
  }

  enum MyNativeInterfaceEnum implements MyNativeInterface {
    INSTANCE;

    @Override
    public String getName() {
      return "MyNativeInterfaceEnum";
    }
  }

  interface MyJavaInterface {
    String getName();
  }

  static class MyJavaAndNativeInterfaceImpl implements MyJavaInterface, MyNativeInterface {
    @Override
    public String getName() {
      return "MyJavaAndNativeInterfaceImpl";
    }
  }

  abstract static class MyNativeInterfaceAbstractImpl
      implements MyNativeInterface, MyNativeInterfaceWithJsMembers {
    @Override
    public String mightBeRenamed() {
      return "MyNativeInterfaceAbstractImpl.mightBeRenamed";
    }
  }

  static class MyNativeInterfaceConcreteImpl extends MyNativeInterfaceAbstractImpl {
    private String property;

    @Override
    public String getName() {
      return "MyNativeInterfaceConcreteImpl";
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public void setProperty(String value) {
      property = value;
    }
  }

  static final class MyNativeInterfaceFinalConcreteImpl extends MyNativeInterfaceAbstractImpl {
    private String property;

    @Override
    public String getName() {
      return "MyNativeInterfaceFinalConcreteImpl";
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public void setProperty(String value) {
      property = value;
    }
  }

  private static void testImplementation() {
    MyNativeInterface jsImpl = createJsImpl("JsImpl");
    assertEquals("JsImpl", callGetName(jsImpl));
    assertEquals("JsImpl", jsImpl.getName());
    assertEquals("MyNativeInterface(JsImpl)", jsImpl.describe());

    MyNativeInterfaceImpl impl = new MyNativeInterfaceImpl();
    assertMyNativeInterface(impl, "MyNativeInterfaceImpl");
    assertMyNativeInterfaceWithJsMembers(impl, "MyNativeInterfaceImpl.mightBeRenamed");
    assertMyNativeInterface(new MyNativeInterfaceImplSubclass(), "MyNativeInterfaceImplSubclass");
    assertMyNativeInterface(new MyNativeSubInterfaceJsTypeImpl(), "MyNativeSubInterfaceJsTypeImpl");
    assertMyNativeInterface(MyNativeInterfaceEnum.INSTANCE, "MyNativeInterfaceEnum");

    MyJavaAndNativeInterfaceImpl javaAndNativeImpl = new MyJavaAndNativeInterfaceImpl();
    assertMyNativeInterface(javaAndNativeImpl, "MyJavaAndNativeInterfaceImpl");
    assertEquals("MyJavaAndNativeInterfaceImpl", ((MyJavaInterface) javaAndNativeImpl).getName());

    MyNativeInterfaceAbstractImpl concreteImpl = new MyNativeInterfaceConcreteImpl();
    assertMyNativeInterface(concreteImpl, "MyNativeInterfaceConcreteImpl");
    assertMyNativeInterfaceWithJsMembers(
        concreteImpl, "MyNativeInterfaceAbstractImpl.mightBeRenamed");
    // Calls through the abstract class resolve to the methods of the native interface.
    assertEquals("MyNativeInterfaceConcreteImpl", concreteImpl.getName());
    assertEquals("MyNativeInterface(MyNativeInterfaceConcreteImpl)", concreteImpl.describe());

    MyNativeInterfaceFinalConcreteImpl finalConcreteImpl = new MyNativeInterfaceFinalConcreteImpl();
    assertMyNativeInterface(finalConcreteImpl, "MyNativeInterfaceFinalConcreteImpl");
    assertMyNativeInterfaceWithJsMembers(
        finalConcreteImpl, "MyNativeInterfaceAbstractImpl.mightBeRenamed");

    assertMyNativeInterface(
        new MyNativeInterface() {
          @Override
          public String getName() {
            return "AnonymousImpl";
          }
        },
        "AnonymousImpl");
  }

  static class NonJsBase {
    private String property;

    public String getName() {
      return "NonJsBase";
    }

    public final String mightBeRenamed() {
      return "NonJsBase.mightBeRenamed";
    }

    public final String getProperty() {
      return property;
    }

    public final void setProperty(String value) {
      property = value;
    }
  }

  static class MyNativeInterfaceAccidentalImpl extends NonJsBase
      implements MyNativeInterface, MyNativeInterfaceWithJsMembers {}

  // Repro b/588567907
  static final class MyNativeInterfaceFinalAccidentalImpl extends NonJsBase
      implements MyNativeInterface, MyNativeInterfaceWithJsMembers {}

  static class MyNativeInterfaceAccidentalImplSubclass extends MyNativeInterfaceAccidentalImpl {
    @Override
    public String getName() {
      return "MyNativeInterfaceAccidentalImplSubclass";
    }
  }

  static class MyNativeSubInterfaceAccidentalImpl extends NonJsBase
      implements MyNativeSubInterface {}

  @JsType
  static class JsTypeBase {
    public String getName() {
      return "JsTypeBase";
    }
  }

  static class MyNativeInterfaceAccidentalJsTypeImpl extends JsTypeBase
      implements MyNativeInterface {}

  private static void testAccidentalImplementation() {
    MyNativeInterfaceAccidentalImpl accidentalImpl = new MyNativeInterfaceAccidentalImpl();
    assertMyNativeInterface(accidentalImpl, "NonJsBase");
    assertMyNativeInterfaceWithJsMembers(accidentalImpl, "NonJsBase.mightBeRenamed");
    MyNativeInterfaceFinalAccidentalImpl finalAccidentalImpl =
        new MyNativeInterfaceFinalAccidentalImpl();
    assertMyNativeInterface(finalAccidentalImpl, "NonJsBase");
    assertMyNativeInterfaceWithJsMembers(finalAccidentalImpl, "NonJsBase.mightBeRenamed");
    assertMyNativeInterface(
        new MyNativeInterfaceAccidentalImplSubclass(), "MyNativeInterfaceAccidentalImplSubclass");
    assertMyNativeInterface(new MyNativeInterfaceAccidentalJsTypeImpl(), "JsTypeBase");
    assertMyNativeInterface(new MyNativeSubInterfaceAccidentalImpl(), "NonJsBase");
  }

  static class StringEcho implements MyNativeGenericInterface<String> {
    @Override
    public String echo(String s) {
      return "StringEcho: " + s;
    }
  }

  abstract static class AbstractStringEcho implements MyNativeGenericInterface<String> {}

  static class ConcreteStringEcho extends AbstractStringEcho {
    @Override
    public String echo(String s) {
      return "ConcreteStringEcho: " + s;
    }
  }

  static class GenericEcho<T> implements MyNativeGenericInterface<T> {
    @Override
    public T echo(T t) {
      return t;
    }
  }

  static class NumberEcho<T extends Number> implements MyNativeGenericInterface<T> {
    @Override
    public T echo(T t) {
      return t;
    }
  }

  static class NonJsStringEcho {
    public String echo(String s) {
      return "NonJsStringEcho: " + s;
    }
  }

  static class AccidentalStringEcho extends NonJsStringEcho
      implements MyNativeGenericInterface<String> {}

  static class NonJsGenericEcho<T> {
    public T echo(T t) {
      return t;
    }
  }

  static class AccidentalGenericEcho extends NonJsGenericEcho<String>
      implements MyNativeGenericInterface<String> {}

  interface StringEchoInterface {
    String echo(String s);
  }

  // Repro b/588568727
  static class AccidentalGenericEchoWithOtherInterface extends NonJsGenericEcho<String>
      implements MyNativeGenericInterface<String>, StringEchoInterface {}

  static class NativeEcho implements MyNativeGenericInterface<MyNativeInterface> {
    @Override
    public MyNativeInterface echo(MyNativeInterface o) {
      return createJsImpl("NativeEcho: " + o.getName());
    }
  }

  private static void testGenericImplementation() {
    assertEcho(new StringEcho(), "hello", "StringEcho: hello");
    assertEcho(new ConcreteStringEcho(), "hello", "ConcreteStringEcho: hello");
    Object o = new Object();
    assertEcho(new GenericEcho<>(), o, o);
    assertEcho(new NumberEcho<Integer>(), 42, 42);
    assertEcho(new AccidentalStringEcho(), "hello", "NonJsStringEcho: hello");
    assertEcho(new AccidentalGenericEcho(), "hello", "hello");
    assertEcho(new AccidentalGenericEchoWithOtherInterface(), "hello", "hello");
    assertEcho(s -> "lambda: " + s, "hello", "lambda: hello");
    assertEcho(String::toUpperCase, "hello", "HELLO");
    assertEquals(
        "NativeEcho: hello",
        ((MyNativeInterface) callEcho(new NativeEcho(), createJsImpl("hello"))).getName());
  }

  interface MyJavaSubInterface extends MyNativeInterface {}

  @JsType
  interface MyJsTypeSubInterface extends MyNativeInterface {}

  interface MyJavaSubInterfaceWithDefault extends MyNativeInterface {
    @Override
    default String getName() {
      return "MyJavaSubInterfaceWithDefault";
    }
  }

  static class MyJavaSubInterfaceImpl implements MyJavaSubInterface {
    @Override
    public String getName() {
      return "MyJavaSubInterfaceImpl";
    }
  }

  static class MyJsTypeSubInterfaceImpl implements MyJsTypeSubInterface {
    @Override
    public String getName() {
      return "MyJsTypeSubInterfaceImpl";
    }
  }

  static class MyJavaSubInterfaceWithDefaultImpl implements MyJavaSubInterfaceWithDefault {}

  static class MyJavaSubInterfaceAccidentalImpl extends NonJsBase implements MyJavaSubInterface {}

  private static void testSubInterfaceImplementation() {
    MyJavaSubInterface impl = new MyJavaSubInterfaceImpl();
    assertMyNativeInterface(impl, "MyJavaSubInterfaceImpl");
    // Calls through the Java subinterface resolve to the methods of the native interface.
    assertEquals("MyJavaSubInterfaceImpl", impl.getName());
    assertMyNativeInterface(new MyJsTypeSubInterfaceImpl(), "MyJsTypeSubInterfaceImpl");
    assertMyNativeInterface(
        new MyJavaSubInterfaceWithDefaultImpl(), "MyJavaSubInterfaceWithDefault");
    assertMyNativeInterface(new MyJavaSubInterfaceAccidentalImpl(), "NonJsBase");
    MyJavaSubInterface lambda = () -> "MyJavaSubInterface lambda";
    assertMyNativeInterface(lambda, "MyJavaSubInterface lambda");
  }

  private static void testJsTypeSingleJavaConcreteInterface() {
    // Create a couple of instances and use the objects in some way to avoid complete pruning
    // of JavaConcrete
    assertTrue(new JsTypeJavaConcrete() != new JsTypeJavaConcrete());
    assertSame(5, new JsTypeJavaConcrete().m());
    assertSame(3, ((JsTypeInterfaceWithSingleJavaConcrete) nativeObjectImplementingM()).m());
  }

  @JsType(
      isNative = true,
      namespace = "subnativeinterface.JsTypeTest",
      name = "InterfaceWithSingleJavaConcrete")
  interface JsTypeInterfaceWithSingleJavaConcrete {
    int m();
  }

  static class JsTypeJavaConcrete implements JsTypeInterfaceWithSingleJavaConcrete {
    @Override
    public int m() {
      return 5;
    }
  }

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native Object nativeObjectImplementingM();

  private static final int SET_PARENT_X = 500;
  private static final int GET_PARENT_X = 1000;
  private static final int GET_X = 100;
  private static final int SET_X = 50;

  private static void testJsPropertyBridges() {
    JsPropertyMyNativeJsTypeInterface object =
        new JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge();

    object.setX(3);
    assertEquals(3 + 150, object.getX());
    assertEquals(3 + SET_X, ((JsPropertyAccidentaImplementor) object).x);

    JsPropertyAccidentaImplementor accidentaImplementor = (JsPropertyAccidentaImplementor) object;

    accidentaImplementor.setX(3);
    assertEquals(3 + 150, accidentaImplementor.getX());
    assertEquals(3 + 150, getProperty(object, "x"));
    assertEquals(3 + SET_X, accidentaImplementor.x);

    setProperty(object, "x", 4);
    assertEquals(4 + 150, accidentaImplementor.getX());
    assertEquals(4 + 150, getProperty(object, "x"));
    assertEquals(4 + SET_X, accidentaImplementor.x);

    assertEquals(3 + 4 + SET_X, accidentaImplementor.sum(3));
  }

  @JsType(
      isNative = true,
      namespace = "subnativeinterface.JsPropertyTest",
      name = "MyNativeJsTypeInterface")
  interface JsPropertyMyNativeJsTypeInterface {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);

    int sum(int bias);
  }

  static class JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge
      extends JsPropertyAccidentaImplementor implements JsPropertyMyNativeJsTypeInterface {}

  abstract static class JsPropertyAccidentaImplementor {
    private int x;

    public int getX() {
      return x + GET_X;
    }

    public void setX(int x) {
      this.x = x + SET_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  private static void testJsPropertyBridgesSubclass() {
    JsPropertyMyNativeJsTypeInterface object =
        new JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass();

    object.setX(3);
    assertEquals(3 + 150, object.getX());

    JsPropertyOtherAccidentalImplementer simple = (JsPropertyOtherAccidentalImplementer) object;

    simple.setX(3);
    assertEquals(3 + GET_X + SET_X, simple.getX());
    assertEquals(3 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(
        3 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((JsPropertyOtherAccidentalImplementer) object).x);

    setProperty(object, "x", 4);
    assertEquals(4 + GET_X + SET_X, simple.getX());
    assertEquals(4 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(
        4 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((JsPropertyOtherAccidentalImplementer) object).x);

    JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass subclass =
        (JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object;

    subclass.setParentX(5);
    assertEquals(8 + SET_PARENT_X, simple.sum(3));
    assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY());
    assertEquals(
        4 + SET_X, ((JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(5 + SET_PARENT_X, ((JsPropertyOtherAccidentalImplementer) object).x);
  }

  static class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed
      extends JsPropertyOtherAccidentalImplementer implements JsPropertyMyNativeJsTypeInterface {}

  abstract static class JsPropertyOtherAccidentalImplementer {
    private int x;

    public int getX() {
      return x + GET_PARENT_X;
    }

    public void setX(int x) {
      this.x = x + SET_PARENT_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  static class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass
      extends JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed {
    private int y;

    @Override
    public int getX() {
      return y + GET_X;
    }

    @Override
    public void setX(int y) {
      this.y = y + SET_X;
    }

    public void setParentX(int value) {
      super.setX(value);
    }

    public int getXPlusY() {
      return super.getX() + y;
    }
  }

  private static void testJsPropertyAccidentalOverrideSuperCall() {
    JsPropertyAccidentalOverrideProperty object = new JsPropertyAccidentalOverrideProperty();
    assertEquals(50, object.getX());
    assertEquals(50, getProperty(object, "x"));
  }

  @JsType(
      isNative = true,
      namespace = "subnativeinterface.JsPropertyTest",
      name = "AccidentalOverridePropertyJsTypeInterface")
  interface JsPropertyAccidentalOverridePropertyJsTypeInterface {
    @JsProperty
    int getX();
  }

  static class JsPropertyAccidentalOverridePropertyBase {
    public int getX() {
      return 50;
    }
  }

  static class JsPropertyAccidentalOverrideProperty extends JsPropertyAccidentalOverridePropertyBase
      implements JsPropertyAccidentalOverridePropertyJsTypeInterface {}

  private static void testJsPropertyRemovedAccidentalOverrideSuperCall() {
    JsPropertyRemovedAccidentalOverrideProperty object =
        new JsPropertyRemovedAccidentalOverrideProperty();
    // If the accidental override here were not removed the access to property x would result in
    // an infinite loop
    assertEquals(55, object.getX());
    assertEquals(55, getProperty(object, "x"));
  }

  @JsType
  static class JsPropertyRemovedAccidentalOverridePropertyBase {
    private JsPropertyRemovedAccidentalOverridePropertyBase() {}

    @JsProperty
    public int getX() {
      return 55;
    }
  }

  static class JsPropertyRemovedAccidentalOverrideProperty
      extends JsPropertyRemovedAccidentalOverridePropertyBase
      implements JsPropertyAccidentalOverridePropertyJsTypeInterface {}

  private static void testJsPropertyRemovedAccidentalOverrideReadWrite() {
    JsPropertyMyNativeJsTypeInterface object =
        new JsPropertyRemovedAccidentalOverrideReadWriteProperty();
    // If the accidental overrides here were not removed the accesses to property x would result in
    // an infinite loop
    object.setX(3);
    assertEquals(3 + 150, object.getX());
    assertEquals(3 + 150, getProperty(object, "x"));

    setProperty(object, "x", 4);
    assertEquals(4 + 150, object.getX());
    assertEquals(4 + 150, getProperty(object, "x"));

    assertEquals(3 + 4 + SET_X, object.sum(3));
  }

  @JsType
  static class JsPropertyRemovedAccidentalOverrideReadWritePropertyBase {
    private int x;

    @JsProperty
    public int getX() {
      return x + GET_X;
    }

    @JsProperty
    public void setX(int x) {
      this.x = x + SET_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  static class JsPropertyRemovedAccidentalOverrideReadWriteProperty
      extends JsPropertyRemovedAccidentalOverrideReadWritePropertyBase
      implements JsPropertyMyNativeJsTypeInterface {}

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native int getProperty(Object object, String name);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native void setProperty(Object object, String name, int value);

  /** Checks calls on a Java object from JavaScript and from Java through the native interface. */
  private static void assertMyNativeInterface(MyNativeInterface o, String name) {
    assertEquals(name, callGetName(o));
    // Calls through native interfaces in Java go through JavaScript.
    assertEquals(name, o.getName());
    assertEquals("MyNativeInterface(" + name + ")", o.describe());
    assertSame(o, passThrough(o));
  }

  private static void assertMyNativeInterfaceWithJsMembers(
      MyNativeInterfaceWithJsMembers o, String renamedResult) {
    assertEquals(renamedResult, callRenamed(o));
    assertEquals(renamedResult, o.mightBeRenamed());
    writeProperty(o, "set from JS");
    assertEquals("set from JS", o.getProperty());
    o.setProperty("set from Java");
    assertEquals("set from Java", readProperty(o));
  }

  private static <T> void assertEcho(MyNativeGenericInterface<T> o, T value, T expected) {
    assertEquals(expected, callEcho(o, value));
    assertEquals(expected, o.echo(value));
  }

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native MyNativeInterface createJsImpl(String name);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native String callGetName(MyNativeInterface o);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native String callRenamed(MyNativeInterfaceWithJsMembers o);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native String readProperty(MyNativeInterfaceWithJsMembers o);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native void writeProperty(MyNativeInterfaceWithJsMembers o, String value);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native Object callEcho(MyNativeGenericInterface<?> o, Object value);

  @JsMethod(namespace = "subnativeinterface.helper")
  private static native Object passThrough(Object o);
}
