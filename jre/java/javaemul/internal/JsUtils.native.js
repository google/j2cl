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
 * @param {number|undefined} value
 * @return {number}
 */
javaemul_internal_JsUtils.coerceToInt = function(value) {
  return value || 0;
};

/**
 * @param {*} map
 * @param {string} key
 * @return {*}
 */
javaemul_internal_JsUtils.getProperty = function(map, key) {
  return map[key];
};

/**
 * @param {*} map
 * @param {string} key
 * @param {*} value
 */
javaemul_internal_JsUtils.setProperty = function(map, key, value) {
  map[key] = value;
};