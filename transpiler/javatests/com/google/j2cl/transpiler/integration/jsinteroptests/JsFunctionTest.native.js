/**
 * @param {*} fn
 * @return {*}
 * @public
 */
__class.callAsFunctionNoArgument = function(fn) {
  return (/** @type {Function} */ (fn))();
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
__class.callAsFunction = function(fn, arg) {
  return (/** @type {Function} */ (fn))(arg);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
__class.callWithFunctionApply = function(fn, arg) {
  return fn.apply(null, [arg]);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
__class.callWithFunctionCall = function(fn, arg) {
  return fn.call(null, arg);
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @param {number} value
 * @public
 */
__class.setField = function(object, fieldName, value) {
  object[fieldName] = value;
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @return {number}
 * @public
 */
__class.getField = function(object, fieldName) {
  return object[fieldName];
};

/**
 * @param {*} object
 * @param {string} functionName
 * @return {number}
 * @public
 */
__class.callIntFunction = function(object, functionName) {
  return object[functionName]();
};

/**
 * @return {Function}
 * @public
 */
__class.createMyJsFunction = function() {
  var myFunction = function(a) { return a; };
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
__class.createReferentialFunction = function() {
  function myFunction() { return myFunction; }
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
__class.createFunction = function() {
  var fun = function(a) { return a; };
  return fun;
};

/**
 * @return {*}
 * @public
 */
__class.createObject = function() {
  var a = {};
  return a;
};

/**
 * @param {*} object
 * @param {?string} fieldName
 * @return {boolean}
 * @public
 */
__class.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};
