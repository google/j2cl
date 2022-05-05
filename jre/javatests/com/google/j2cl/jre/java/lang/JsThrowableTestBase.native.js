/**
 * @param {*} e
 * @return {void}
 */
JsThrowableTestBase.throwNative = function(e) {
  JsThrowableTestBase.$clinit();
  throw e;
};

/**
 * @param {function()} thrower
 * @return {*}
 */
JsThrowableTestBase.catchNative = function(thrower) {
  JsThrowableTestBase.$clinit();
  try {
    thrower();
  } catch (e) {
    return e;
  }
};
