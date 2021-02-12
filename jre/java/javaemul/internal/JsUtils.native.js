/**
 * @param {*} value
 * @return {boolean}
 */
javaemul_internal_JsUtils.m_isUndefined__java_lang_Object = function(value) {
  javaemul_internal_JsUtils.$clinit();
  return value === undefined;
};

/**
 * @param {*} value
 * @return {*}
 */
javaemul_internal_JsUtils.m_uncheckedCast__java_lang_Object = function(value) {
  javaemul_internal_JsUtils.$clinit();
  return value;
};

/**
 * @param {*} map
 * @param {string} key
 * @return {*}
 */
javaemul_internal_JsUtils.m_getProperty__java_lang_Object__java_lang_String =
    function(map, key) {
  javaemul_internal_JsUtils.$clinit();
  return map[key];
};
