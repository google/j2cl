/**
 * @return {number}
 */
__class.varargsLengthThruArguments = function() {
  return arguments.length;
};

/**
 * @return {*}
 */
__class.callGetVarargsSlotUsingJsName = function() {
  return __class.getVarargsSlot(2, '1', '2', '3', '4');
};

/**
 * @return {number}
 */
__class.callSumAndMultiply = function() {
  return __class.sumAndMultiply(2, 10, 20);
};

/**
 * @return {number}
 */
__class.callSumAndMultiplyInt = function() {
  return __class.sumAndMultiplyInt(3, 2, 8);
};

/**
 * @param {Function} f
 * @return {*}
 */
__class.callAFunction = function(f) {
  return f(2, null, null, f, null);
};
