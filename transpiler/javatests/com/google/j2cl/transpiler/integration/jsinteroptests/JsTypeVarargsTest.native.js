/**
 * @return {number}
 */
JsTypeVarargsTest.varargsMethod1 = function() {
  return arguments.length;
};

/**
 * @return {*}
 */
JsTypeVarargsTest.callVarargsMethod3FromJSNI = function() {
  return JsTypeVarargsTest.varargsMethod3(2, '1', '2', '3', '4');
};

/**
 * @return {number}
 */
JsTypeVarargsTest.callSumAndMultiply = function() {
  return JsTypeVarargsTest.sumAndMultiply(2, 10, 20);
};

/**
 * @return {number}
 */
JsTypeVarargsTest.callSumAndMultiplyInt = function() {
  return JsTypeVarargsTest.sumAndMultiplyInt(3, 2, 8);
};

/**
 * @param {Function} f
 * @return {*}
 */
JsTypeVarargsTest.callAFunction = function(f) {
  return f(2, null, null, f, null);
};
