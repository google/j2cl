/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
Main.callAsFunction = function(fn, arg) {
  return (/** @type {Function} */ (fn))(arg);
};

/**
 * @return {MyJsFunctionInterface | Function}
 * @public
 */
Main.createMyJsFunction = function() {
  let myFn = function(a) { return a; };
  return myFn;
}
