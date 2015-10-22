/**
 * Test that native methods (both instance and static method)
 * are converted to executable javascript functions.
 *
 * See ClassWithNativeMethod.java
 */

goog.require('goog.testing.jsunit');

function testVoidNativeMethod() {
  var instance = new ClassWithNativeMethod();

  assertNotUndefined(instance.m_nativeMethod__java_lang_String);

  instance.m_nativeMethod__java_lang_String('foo');
  assertEquals('foo', instance.field);
}

function testNativeMethodWithResult() {
  var instance = new ClassWithNativeMethod();

  assertNotUndefined(instance.m_nativeMethodWithResult);

  var result = instance.m_nativeMethodWithResult();
  assertEquals('nativeMethodWithResult', result);
}

function testNonNativeMethodNotConverted() {
  var instance = new ClassWithNativeMethod();
  assertUndefined(instance.nonNativeMethod);
}

function testVoidStaticNativeMethod() {
  assertNotUndefined(ClassWithNativeMethod.m_staticNative__java_lang_String);

  ClassWithNativeMethod.m_staticNative__java_lang_String('foo');
  assertEquals('foo', ClassWithNativeMethod.staticField);
}

function testStaticNativeMethodWithResult() {
  assertNotUndefined(ClassWithNativeMethod.m_staticNativeWithResult);

  var result = ClassWithNativeMethod.m_staticNativeWithResult();
  assertEquals('staticNativeWithResult', result);
}

function testNonNativeStaticMethodNotConverted() {
  // non native static method not converted
  assertUndefined(ClassWithNativeMethod.staticNotNative);
}
