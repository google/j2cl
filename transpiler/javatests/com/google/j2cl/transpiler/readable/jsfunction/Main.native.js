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
 * @return {!Function}
 * @public
 */
__class.createMyJsFunction = function() {
  let myFn = function(a) { return a; };
  return myFn;
}
