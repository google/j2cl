/**
 * @param {*} o
 * @return {number}
 * @public
 */
__class.callFoo = function(o) {
  return o.foo();
};

/**
 * @param {*} o
 * @return {number}
 * @public
 */
__class.accessField = function(o) {
  return o.field;
}
