/**
 * Test that native methods (both instance and static method)
 * are converted to executable javascript functions.
 *
 * See ClassWithNativeMethod.java
 */

goog.require('goog.testing.jsunit');

function testVoidNativeMethod() {
    var instance = new ClassWithNativeMethod();

    assertNotUndefined(instance.nativeMethod);

    instance.nativeMethod('foo');
    assertEquals('foo', instance.field);
}

function testNativeMethodWithResult() {
    var instance = new ClassWithNativeMethod();

    assertNotUndefined(instance.nativeMethodWithResult);

    var result = instance.nativeMethodWithResult();
    assertEquals('nativeMethodWithResult', result);
}

function testNonNativeMethodNotConverted() {
    var instance = new ClassWithNativeMethod();
    assertUndefined(instance.nonNativeMethod);
}

function testVoidStaticNativeMethod() {
    assertNotUndefined(ClassWithNativeMethod.staticNative);

    ClassWithNativeMethod.staticNative('foo');
    assertEquals('foo', ClassWithNativeMethod.staticField);
}

function testStaticNativeMethodWithResult() {
    assertNotUndefined(ClassWithNativeMethod.staticNativeWithResult);

    var result = ClassWithNativeMethod.staticNativeWithResult();
    assertEquals('staticNativeWithResult', result);
}

function testNonNativeStaticMethodNotConverted() {
    // non native static method not converted
    assertUndefined(ClassWithNativeMethod.staticNotNative);
}
