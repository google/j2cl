/**
 * @param {number} value
 * @return {number}
 */
java_lang_IntegralToString.toDoubleFromUnsignedInt = function(value) {
  // Might return a number that is larger than int32
  return (value >>> 0);
};
