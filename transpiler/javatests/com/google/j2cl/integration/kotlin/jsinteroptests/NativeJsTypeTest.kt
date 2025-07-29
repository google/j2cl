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
@file:Suppress("KotlinConstantConditions")

package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

/** Tests native JsType functionality. */
object NativeJsTypeTest {
  fun testAll() {
    testClassLiterals()
    testGetClass()
    testEqualityOptimization()
    testClassLiterals()
    testNativeJsTypeWithOverlay()
    testNativeJsTypeWithStaticIntializer()
    testNativeInnerClass()
    testSpecialNativeInstanceOf()
    testForwaringMethodsOnNativeClasses()
    testUninitializedStaticOverlayField()
    testVariableExternCollision()
    testAliasExternCollision()
    testBridgesNativeSubclass()
    testCallNamespaceAsFunction()
    testAccessNamespaceAsProperty()
  }

  @JsType(isNative = true)
  internal class MyNativeJsType {
    @Override external override fun hashCode(): Int
  }

  @JsType(isNative = true) private interface MyNativeJsTypeInterface

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class NativeObject : MyNativeJsTypeInterface

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private class FinalNativeObject : MyNativeJsTypeInterface

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface MyNativeJsTypeInterfaceOnlyOneConcreteImplementor

  fun testClassLiterals() {
    assertEquals("<native object>", MyNativeJsType::class.java.name)
    // Native classes have all the same class literal.
    assertEquals(MyNativeJsType::class.java, NativeObject::class.java)

    // native interfaces and native classes have different class literals to satisfy Java semantics
    // with respect to Class.isInterface etc.
    assertNotEquals(MyNativeJsType::class.java, MyNativeJsTypeInterface::class.java)
    // Native interfaces have all the same class literal.
    assertEquals(
      MyNativeJsTypeInterface::class.java,
      MyNativeJsTypeInterfaceOnlyOneConcreteImplementor::class.java,
    )

    // Native arrays have all the same class literal, and is Object[]::class.java.
    assertEquals(Array<Any?>::class.java, Array<MyNativeJsType>::class.java)
    assertEquals(Array<MyNativeJsType>::class.java, Array<MyNativeJsType>::class.java)
    assertEquals(Array<MyNativeJsType>::class.java, Array<MyNativeJsTypeInterface>::class.java)
    assertEquals(Array<MyNativeJsType>::class.java, Array<Array<MyNativeJsType>>::class.java)
    assertEquals(
      Array<MyNativeJsType>::class.java,
      Array<Array<MyNativeJsTypeInterface>>::class.java,
    )
  }

  fun testGetClass() {
    val o: Any = createNativeObjectWithoutToString()
    assertEquals(MyNativeJsType::class.java, o.javaClass)

    val nativeInterface = createNativeObjectWithoutToString() as MyNativeJsTypeInterface
    assertEquals(MyNativeJsType::class.java, nativeInterface.javaClass)

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject1 as
    // exact and inlining Object.javaClass implementation.
    val nativeObject1 = NativeObject()
    assertEquals(MyNativeJsType::class.java, nativeObject1.javaClass)

    // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject2 as
    // exact and inlining Object.javaClass implementation.
    val nativeObject2: FinalNativeObject = createNativeObject()
    assertEquals(MyNativeJsType::class.java, nativeObject2.javaClass)

    assertEquals(Array<MyNativeJsType>::class.java, createNativeArray().javaClass)
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class AnotherFinalNativeObject : MyNativeJsTypeInterface

  private fun same(thisObject: Any?, thatObject: Any?): Boolean = thisObject === thatObject

  fun testEqualityOptimization() {
    // Makes sure that == does not get optimized away due to static class incompatibility.

    val finalNativeObject = FinalNativeObject()

    val anotherFinalNativeObject = (finalNativeObject as Any) as AnotherFinalNativeObject
    // DeadCodeElimination could optimize statically to false due to type incompatibility, which
    // could happen if both variables were marked as exact.
    assertTrue(same(anotherFinalNativeObject, finalNativeObject))
  }

  fun testToString() {
    val nativeObjectWithToString: Any? = createNativeObjectWithToString()
    assertEquals("Native type", nativeObjectWithToString.toString())

    val nativeObjectWithoutToString: Any? = createNativeObjectWithoutToString()
    assertEquals("[object Object]", nativeObjectWithoutToString.toString())

    val nativeArray: Any? = createNativeArray()
    assertEquals("", nativeArray.toString())
  }

  @JsMethod @JvmStatic private external fun createNativeObject(): FinalNativeObject

  @JsMethod @JvmStatic private external fun createNativeObjectWithToString(): MyNativeJsType

  @JsMethod(name = "createNativeObject")
  @JvmStatic
  private external fun createNativeObjectWithoutToString(): MyNativeJsType

  @JsMethod @JvmStatic private external fun createNativeArray(): Any

  @JsType(isNative = true, name = "NativeJsTypeWithOverlay")
  internal class NativeJsTypeWithOverlay {
    companion object {
      @JsOverlay @JvmField val x: Int = 2

      @JsMethod(namespace = JsPackage.GLOBAL, name = "Object.keys")
      @JvmStatic
      external fun keys(o: NativeObject?): Array<String>

      @JsOverlay
      @JvmStatic
      fun hasM(obj: Any?): Boolean {
        for (k in keys(obj as NativeObject)) {
          if ("m" == k) {
            return true
          }
        }
        return false
      }
    }

    external fun hasOwnProperty(name: String): Boolean

    @JsOverlay fun hasM(): Boolean = hasOwnProperty("m")

    @JvmField var k: Int = definedExternally

    @JsOverlay
    fun setK(k: Int): NativeJsTypeWithOverlay {
      this.k = k
      return this
    }
  }

  @JsMethod
  @JvmStatic
  private external fun createNativeJsTypeWithOverlayWithM(): NativeJsTypeWithOverlay

  fun testNativeJsTypeWithOverlay() {
    val o: NativeJsTypeWithOverlay = createNativeJsTypeWithOverlayWithM()
    assertTrue(o.hasM())
    assertTrue(NativeJsTypeWithOverlay.hasM(o))
    assertEquals(2, NativeJsTypeWithOverlay.x)
    assertEquals(42, o.setK(3).setK(42).k)
  }

  @JsType(isNative = true)
  internal class NativeJsTypeWithStaticInitializationAndFieldAccess {
    companion object {
      @JsOverlay @JvmField val o: Any? = 3
    }
  }

  @JsType(isNative = true)
  internal class NativeJsTypeWithStaticInitializationAndStaticOverlayMethod {
    companion object {
      @JsOverlay @JvmField val o: Any? = 4

      @JsOverlay @JvmStatic fun getObject(): Any? = o
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod {
    companion object {
      @JsOverlay @JvmField val o: Any? = 5

      @JsOverlay @JvmStatic fun getObject(): Any? = o

      init {
        clinitCalled++
      }
    }
  }

  @JvmStatic private var clinitCalled = 0

  fun testNativeJsTypeWithStaticIntializer() {
    assertEquals(3 as Int?, NativeJsTypeWithStaticInitializationAndFieldAccess.o)
    assertEquals(0, clinitCalled)
    assertEquals(4 as Int?, NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject())
    assertEquals(
      5 as Int?,
      NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod.Companion.getObject(),
    )
    assertEquals(1, clinitCalled)
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  internal class NativeFunction

  @JsMethod @JvmStatic private external fun createFunction(): Any?

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") internal class NativeArray

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  internal class NativeNumber

  @JsMethod @JvmStatic private external fun createNumber(): Any?

  @JsMethod @JvmStatic private external fun createBoxedNumber(): Any?

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  internal class NativeString

  @JsMethod @JvmStatic private external fun createBoxedString(): Any?

  @JsFunction
  fun interface SomeFunctionInterface {
    fun m()
  }

  internal class SomeFunction : SomeFunctionInterface {
    override fun m() {}
  }

  fun testSpecialNativeInstanceOf() {
    val aJsFunction: Any? = SomeFunction()
    // True cases.
    assertTrue(aJsFunction is NativeFunction)
    assertTrue(aJsFunction is SomeFunctionInterface)
    assertTrue(aJsFunction is NativeObject)
    // False cases.
    assertFalse(aJsFunction is NativeArray)
    assertFalse(aJsFunction is NativeNumber)
    assertFalse(aJsFunction is NativeString)

    val anotherFunction: Any? = createFunction()
    // True cases.
    assertTrue(anotherFunction is NativeFunction)
    assertTrue(anotherFunction is SomeFunctionInterface)
    assertTrue(anotherFunction is NativeObject)
    // False cases.
    assertFalse(anotherFunction is NativeArray)
    assertFalse(anotherFunction is NativeNumber)
    assertFalse(anotherFunction is NativeString)

    val aString: Any? = "Hello"
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(aString instanceof NativeString);
    // False cases.
    assertFalse(aString is NativeFunction)
    assertFalse(aString is NativeObject)
    assertFalse(aString is NativeArray)
    assertFalse(aString is NativeNumber)

    val aBoxedString: Any? = createBoxedString()
    // True cases.
    // Note that boxed strings are (surprisingly) not strings but objects.
    assertTrue(aBoxedString is NativeObject)
    // False cases.
    assertFalse(aBoxedString is NativeFunction)
    assertFalse(aBoxedString is NativeArray)
    assertFalse(aBoxedString is NativeNumber)
    // TODO(b/31271239): uncomment
    // assertFalse(aBoxedString is NativeString);

    val anArray: Any? = emptyArray<String>()
    // True cases.
    assertTrue(anArray is NativeArray)
    assertTrue(anArray is NativeObject)
    // False cases.
    assertFalse(anArray is NativeFunction)
    assertFalse(anArray is NativeNumber)
    assertFalse(anArray is NativeString)

    val aNativeArray: Any? = createNativeArray()
    // True cases.
    assertTrue(aNativeArray is NativeArray)
    assertTrue(anArray is NativeObject)
    // False cases.
    assertFalse(aNativeArray is NativeFunction)
    assertFalse(aNativeArray is NativeNumber)
    assertFalse(aNativeArray is NativeString)

    val aNumber: Any? = 3.0
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(aNumber is NativeNumber);
    // False cases.
    assertFalse(aNumber is NativeArray)
    assertFalse(aNumber is NativeObject)
    assertFalse(aNumber is NativeFunction)
    assertFalse(aNumber is NativeString)

    val anotherNumber: Any? = createNumber()
    // True cases.
    // TODO(b/31271239): uncomment
    // assertTrue(anotherNumber is NativeNumber);
    // False cases.
    assertFalse(anotherNumber is NativeArray)
    assertFalse(anotherNumber is NativeObject)
    assertFalse(anotherNumber is NativeFunction)
    assertFalse(anotherNumber is NativeString)

    val aBoxedNumber: Any? = createBoxedNumber()
    // True cases.
    assertTrue(aBoxedNumber is NativeObject)
    // False cases.
    // TODO(b/31271239): uncomment
    // assertFalse(aBoxedNumber is NativeNumber);
    assertFalse(aBoxedNumber is NativeArray)
    assertFalse(aBoxedNumber is NativeFunction)
    assertFalse(aBoxedNumber is NativeString)

    val anObject: Any? = Any()
    // True cases.
    assertTrue(anObject is NativeObject)
    // False cases.
    assertFalse(anObject is NativeNumber)
    assertFalse(anObject is NativeArray)
    assertFalse(anObject is NativeFunction)
    assertFalse(anObject is NativeString)

    val nullObject: Any? = null

    assertFalse(nullObject is NativeObject)
    assertFalse(nullObject is NativeArray)
    assertFalse(nullObject is NativeFunction)
    assertFalse(nullObject is NativeString)
    assertFalse(nullObject is NativeNumber)

    val undefined: Any? = getUndefined()
    assertFalse(undefined is NativeObject)
    assertFalse(undefined is NativeArray)
    assertFalse(undefined is NativeFunction)
    assertFalse(undefined is NativeString)
    assertFalse(undefined is NativeNumber)
  }

  @JsProperty(namespace = JsPackage.GLOBAL) @JvmStatic private external fun getUndefined(): Any?

  @SuppressWarnings("unused")
  fun testVariableExternCollision() {
    val Int8Array: Any? = null
    // A variable name that would collide with an extern.
    assertNotNull(getInt8ArrayBytesPerElement())
  }

  @JsProperty(namespace = JsPackage.GLOBAL, name = "Int8Array.BYTES_PER_ELEMENT")
  @JvmStatic
  private external fun getInt8ArrayBytesPerElement(): Double

  fun testAliasExternCollision() {
    val unused: Float32Array = Float32Array()
    // make sure it is referenced hence aliased.
    assertNotNull(getFloat32ArrayBytesPerElement())
  }

  internal class Float32Array {}

  @JsProperty(namespace = JsPackage.GLOBAL, name = "Float32Array.BYTES_PER_ELEMENT")
  @JvmStatic
  private external fun getFloat32ArrayBytesPerElement(): Any?

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal interface NativeInterface {
    fun add(element: String?)
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal open class NativeSuperClass {
    external fun add(element: String?)

    external fun remove(element: String?): Boolean
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class NativeSubClassAccidentalOverride : NativeSuperClass(), NativeInterface

  @JsMethod @JvmStatic private external fun createNativeSubclass(): NativeSubClassAccidentalOverride

  fun testForwaringMethodsOnNativeClasses() {
    val subClass: NativeSubClassAccidentalOverride = createNativeSubclass()
    subClass.add("Hi")
    assertTrue(subClass.remove("Hi"))
    assertFalse(subClass.remove("Hi"))
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class NativeClassWithStaticOverlayFields {
    companion object {
      // Kotlin requires properties to be explicitly initialized
      // @JsOverlay @JvmStatic val uninitializedString: String
      // @JsOverlay @JvmStatic val uninitializedInt: Int
      @JsOverlay @JvmField val initializedInt: Int = 5
    }
  }

  fun testUninitializedStaticOverlayField() {
    // Kotlin requires properties to be explicitly initialized
    // assertEquals(0, NativeClassWithStaticOverlayFields.uninitializedInt);
    assertEquals(5, NativeClassWithStaticOverlayFields.initializedInt)
    // Kotlin requires properties to be explicitly initialized
    // assertNull(NativeClassWithStaticOverlayFields.uninitializedString);
  }

  @JsType(isNative = true, name = "MyNativeJsType")
  internal class MyNativeJsTypeWithInner {
    @JsType(isNative = true)
    internal class Inner(n: Int) {
      @JvmField val n: Int = definedExternally
    }
  }

  // This is implemented in NativeJsTypeTest.native.js on purpose to make sure that the reference to
  // the enclosing module is considered a Java type and all the scheme to avoid dependency
  // cycles is correct.
  @JsType(isNative = true, name = "MyNativeJsType.Inner")
  internal class MyNativeJsTypeInner(n: Int) {
    @JsProperty external fun getN(): Int
  }

  fun testNativeInnerClass() {
    var o = MyNativeJsTypeWithInner.Inner(3)
    assertTrue(o is MyNativeJsTypeWithInner.Inner)
    assertTrue((o as Any) is MyNativeJsTypeInner)
    assertEquals(3, o.n)
    o = (MyNativeJsTypeInner(4) as Any) as MyNativeJsTypeWithInner.Inner
    assertEquals(4, o.n)
  }

  internal interface InterfaceWithDefaultMethods {
    fun method(): String

    fun defaultMethod(): String = "default-method"
  }

  private abstract class AbstractSuperWithPackagePrivateMethod
  @JsConstructor
  internal constructor() {
    internal open fun packagePrivateMethod() = "package-private"
  }

  @JsType(namespace = "woo.NativeJsTypeTest", name = "AbstractJavaSuperclass")
  private abstract class AbstractSuperClassForJs :
    AbstractSuperWithPackagePrivateMethod(), InterfaceWithDefaultMethods {
    public abstract override fun packagePrivateMethod(): String

    abstract override fun method(): String
  }

  fun testBridgesNativeSubclass() {
    val nativeSubclass: AbstractSuperClassForJs = createBridgesNativeSubclass()
    // Tests that the default method is implemented.
    assertEquals("default-method", nativeSubclass.defaultMethod())

    assertEquals("package-private-override", nativeSubclass.packagePrivateMethod())
    // Tests package private bridge.
    assertEquals(
      "package-private-override",
      ((nativeSubclass as Any) as AbstractSuperWithPackagePrivateMethod).packagePrivateMethod(),
    )

    assertEquals("method", nativeSubclass.method())
    // Tests JsMethod bridge.
    assertEquals("method", ((nativeSubclass as Any) as InterfaceWithDefaultMethods).method())
  }

  @JsMethod(namespace = "woo.NativeJsTypeTest.NativeBridgesSubclass", name = "create")
  @JvmStatic
  private external fun createBridgesNativeSubclass(): AbstractSuperClassForJs

  @JsMethod(namespace = "woo.NativeJsTypeTest.FunctionNamespace", name = "")
  @JvmStatic
  private external fun callFunctionNamespace(input: String): String

  fun testCallNamespaceAsFunction() {
    assertEquals("foobar", callFunctionNamespace("foo"))
  }

  @JsProperty(namespace = "woo.NativeJsTypeTest.PropertyNamespace", name = "")
  @JvmStatic
  private external fun getPropertyNamespace(): String

  fun testAccessNamespaceAsProperty() {
    assertEquals("foo", getPropertyNamespace())
  }
}
