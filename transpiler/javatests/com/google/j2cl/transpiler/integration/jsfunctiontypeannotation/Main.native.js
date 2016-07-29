/**
 * @param {?function(?number, ?number):?number} fn
 * @return {?number}
 * @public
 */
__class.callOnFunction = function(fn) {
  return fn(1.1, 1.1);
}
