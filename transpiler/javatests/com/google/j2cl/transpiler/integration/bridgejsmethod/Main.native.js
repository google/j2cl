/**
 * @param {*} o
 * @param {*} arg
 * @return {*}
 * @public
 */
Main.callFun = function(o, arg) {
  return o.fun(arg);
};

/**
 * @param {*} o
 * @param {*} t
 * @param {*} s
 * @return {boolean}
 * @public
 */
Main.callBar = function(o, t, s) {
  return o.bar(t, s);
}
