/**
 * Test if one uses the JsMethod annotation on his native methods,
 * the corresponding javascript methods are well renamed.
 *
 * See AnnotatedMethod.java
 */
goog.require('goog.testing.jsunit');

function testStaticNativeAnnotedMethod() {
  assertNotUndefined(AnnotatedMethod.staticMethod);
  assertEquals('annotatedStaticNativeMethod', AnnotatedMethod.staticMethod());
}

function testStaticNativeMethodWithFqnAnnotation() {
  assertNotUndefined(AnnotatedMethod.fqnStaticMethod);
  assertEquals('fqnAnnotatedStaticNativeMethod',
               AnnotatedMethod.fqnStaticMethod());
}

function testNativeAnnotedMethod() {
  var instance = new AnnotatedMethod();

  assertNotUndefined(instance.nativeMethod);
  assertEquals('annotatedNativeMethod', instance.nativeMethod());
}

function testNativeMethodWithFqnAnnotation() {
  var instance = new AnnotatedMethod();

  assertNotUndefined(instance.fqnNativeMethod);
  assertEquals('fqnAnnotatedNativeMethod', instance.fqnNativeMethod());
}

function testNativeMethodWithEmptyAnnotation() {
  var instance = new AnnotatedMethod();

  assertNotUndefined(instance.notRenamedNativeMethod);
  assertEquals('notRenamedNativeMethod', instance.notRenamedNativeMethod());
}
