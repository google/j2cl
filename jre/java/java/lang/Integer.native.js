/**
 * @param {number} value
 * @return {number}
 */
java_lang_Integer.m_toUnsigned__int_$p_java_lang_Integer = function(value) {
  java_lang_Integer.$clinit();
  // Might return a number that is larger than int32
  return (value >>> 0);
};
