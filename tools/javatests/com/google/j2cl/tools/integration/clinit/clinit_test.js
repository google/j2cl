/**
 * Test that $clinit is called by static native methods.
 */

goog.require('goog.testing.jsunit');

function testClinitMethodCalledByStaticNativeMethod() {
  assertEquals(clinit_called, false);
  Main.m_staticNativeMethod();
  assertEquals(clinit_called, true);
}
