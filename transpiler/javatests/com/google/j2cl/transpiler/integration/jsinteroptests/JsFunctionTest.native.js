/**
 * @param {*} fn
 * @return {*}
 * @public
 */
JsFunctionTest.callAsFunctionNoArgument = function(fn) {
  return (/** @type {Function} */ (fn))();
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callAsFunction = function(fn, arg) {
  return (/** @type {Function} */ (fn))(arg);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callWithFunctionApply = function(fn, arg) {
  return fn.apply(null, [arg]);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callWithFunctionCall = function(fn, arg) {
  return fn.call(null, arg);
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @param {number} value
 * @public
 */
JsFunctionTest.setField = function(object, fieldName, value) {
  object[fieldName] = value;
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @return {number}
 * @public
 */
JsFunctionTest.getField = function(object, fieldName) {
  return object[fieldName];
};

/**
 * @param {*} object
 * @param {string} functionName
 * @return {number}
 * @public
 */
JsFunctionTest.callIntFunction = function(object, functionName) {
  return object[functionName]();
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createMyJsFunction = function() {
  var myFunction = function(a) { return a; };
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createReferentialFunction = function() {
  function myFunction() {
    return myFunction;
  }
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createFunction = function() {
  var fun = function(a) { return a; };
  return fun;
};

/**
 * @return {*}
 * @public
 */
JsFunctionTest.createObject = function() {
  var a = {};
  return a;
};

/**
 * @param {*} object
 * @param {?string} fieldName
 * @return {boolean}
 * @public
 */
JsFunctionTest.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};
