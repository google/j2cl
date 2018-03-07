goog.module('com.google.j2cl.junit.integration.stacktrace.data.NativeStacktraceTest.ThrowingJsClass');

class ThrowingJsClass {
  throwEventually() {
    this.method1();
  }

  method1() {
    this.method2();
  }

  method2() {
    this.method3();
  }

  method3() {
    // We are throwing a Js error here instead of a Java exception, since
    // this exposes a bug in JsCompiler (b/63400239) that does not account
    // for removing the 'new' in the column number.
    // We still use the text here that the Java exception would have produced
    // so that our stack trace asserter does not need to deal with this
    // special case.
    throw new Error('java.lang.RuntimeException: __the_message__!');
  }
}

exports = ThrowingJsClass;