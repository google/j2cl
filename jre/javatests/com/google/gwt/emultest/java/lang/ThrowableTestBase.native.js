/**
 * @param {*} e
 * @return {void}
 */
ThrowableTestBase.throwNative = function(e) {
  ThrowableTestBase.$clinit();
  throw e;
};

/**
 * @param {function()} thrower
 * @return {*}
 */
ThrowableTestBase.catchNative = function(thrower) {
  ThrowableTestBase.$clinit();
  try {
    thrower();
  } catch (e) {
    return e;
  }
};

const JavaLangThrowable = goog.require("java.lang.Throwable");

/**
 * @param {*} wrapped
 * @return {!JavaLangThrowable}
 */
ThrowableTestBase.createJsException = function(wrapped) {
  ThrowableTestBase.$clinit();
  return JavaLangThrowable.of(wrapped);
};
