/**
 * @param {*} o
 * @param {*} arg
 * @return {*}
 * @public
 */
__class.callFun = function(o, arg) {
  return o.fun(arg);
};

/**
 * @param {*} o
 * @param {*} t
 * @param {*} s
 * @return {boolean}
 * @public
 */
__class.callBar = function(o, t, s) {
  return o.bar(t, s);
}
