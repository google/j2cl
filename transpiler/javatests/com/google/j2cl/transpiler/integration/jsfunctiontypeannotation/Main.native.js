/**
 * @param {?function(?number, ?number):?number} fn
 * @return {?number}
 * @public
 */
Main.callOnFunction = function(fn) {
  return fn(1.1, 1.1);
}
