/**
 * Test that native methods (both instance and static methods) are converted
 * and added to the right prototype.
 *
 * See Outer.java
 */

goog.require('goog.testing.jsunit');

function testInnerInnerStaticMethod() {
    assertNotUndefined(Outer$Inner$InnerInner.staticInnerInner);

    assertEquals(
        'staticInnerInner',
        Outer$Inner$InnerInner.staticInnerInner()
    );
}

function testInnerInnerClassMethod() {
    var instance = new Outer$Inner$InnerInner();

    assertNotUndefined(instance.innerInner);

    assertEquals('innerInner', instance.innerInner());
}

function testInnerStaticMethod() {
    assertNotUndefined(Outer$Inner.staticInner);

    assertEquals('staticInner', Outer$Inner.staticInner());
}

function testInnerClassMethod() {
    var instance = new Outer$Inner();

    assertNotUndefined(instance.inner);

    assertEquals('inner', instance.inner());
}

function testNonNativeMethodNotConverted() {
    assertUndefined(Outer$Inner$InnerInner.staticNotNativeMethod);

    var inner = new Outer$Inner();
    assertUndefined(inner.nonNativeMethod);

    assertUndefined(Outer$Inner.staticNotNativeMethod);

    var innerInner = new Outer$Inner$InnerInner();
    assertUndefined(innerInner.nonNativeMethod);
}
