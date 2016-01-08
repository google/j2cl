/**
 * @return {?number}
 */
Main.callSumAndMultiply = function() {
  return Main.sumAndMultiply(1, 1, 2);
};

/**
 * @return {number}
 */
Main.callF1 = function() {
  return Main.f1(10, 1, 2);
};

/**
 * @return {number}
 */
Main.callF2 = function() {
  return Main.f2(1, 2);
};

/**
 * @param {Main} m
 * @return {number}
 */
Main.callF3 = function(m) {
  return m.f3(10, 1, 2);
};

/**
 * @param {Main} m
 * @return {number}
 */
Main.callF4 = function(m) {
  return m.f4(1, 2);
};

/**
 * @param {*} a
 * @return {number}
 */
Main.callJsFunction = function(a) {
  return (/**@type {Function}*/ (a))(0, null, null);
};
