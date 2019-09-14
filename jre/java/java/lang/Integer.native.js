/**
 * @param {number} value
 * @return {number}
 */
java_lang_Integer.m_toUnsigned__int = function(value) {
  java_lang_Integer.$clinit();
  // Might return a number that is larger than int32
  return (value >>> 0);
};
