/**
 * Test that native methods (both instance and static methods) are converted
 * and added to the right prototype.
 *
 * See Outer.java
 */

goog.require('goog.testing.jsunit');

function testInnerInnerStaticMethod() {
  assertNotUndefined(Outer$Inner$InnerInner.m_staticInnerInner);

  assertEquals('staticInnerInner', Outer$Inner$InnerInner.m_staticInnerInner());
}

function testInnerInnerClassMethod() {
  var instance = new Outer$Inner$InnerInner();

  assertNotUndefined(instance.m_innerInner);

  assertEquals('innerInner', instance.m_innerInner());
}

function testInnerStaticMethod() {
  assertNotUndefined(Outer$Inner.m_staticInner);

  assertEquals('staticInner', Outer$Inner.m_staticInner());
}

function testInnerClassMethod() {
  var instance = new Outer$Inner();

  assertNotUndefined(instance.m_inner);

  assertEquals('inner', instance.m_inner());
}

function testNonNativeMethodNotConverted() {
  assertUndefined(Outer$Inner$InnerInner.m_staticNotNativeMethod);

  var inner = new Outer$Inner();
  assertUndefined(inner.m_nonNativeMethod);

  assertUndefined(Outer$Inner.m_staticNotNativeMethod);

  var innerInner = new Outer$Inner$InnerInner();
  assertUndefined(innerInner.m_nonNativeMethod);
}
