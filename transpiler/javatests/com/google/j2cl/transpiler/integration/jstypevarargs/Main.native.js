/**
 * @return {?number}
 */
__class.callSumAndMultiply = function() {
  return __class.sumAndMultiply(1, 1, 2);
};

/**
 * @return {number}
 */
__class.callF1 = function() {
  return __class.f1(10, 1, 2);
};

/**
 * @return {number}
 */
__class.callF2 = function() {
  return __class.f2(1, 2);
};

/**
 * @param {__class} m
 * @return {number}
 */
__class.callF3 = function(m) {
  return m.f3(10, 1, 2);
};

/**
 * @param {__class} m
 * @return {number}
 */
__class.callF4 = function(m) {
  return m.f4(1, 2);
};

/**
 * @param {*} a
 * @return {number}
 */
__class.callJsFunction = function(a) {
  return (/**@type {Function}*/ (a))(0, null, null);
};
