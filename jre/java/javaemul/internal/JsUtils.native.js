/**
 * @param {number} value
 * @return {number}
 */
javaemul_internal_JsUtils.toDoubleFromUnsignedInt = function(value) {
  // Might return a number that is larger than int32
  return (value >>> 0);
};

/**
 * @param {*} value
 * @return {boolean}
 */
javaemul_internal_JsUtils.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {*} value
 * @return {*}
 */
javaemul_internal_JsUtils.uncheckedCast = function(value) {
  return value;
};

/**
 * @param {*} map
 * @param {string} key
 * @return {*}
 */
javaemul_internal_JsUtils.getProperty = function(map, key) {
  return map[key];
};
