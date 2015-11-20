/**
 * @param {number} v
 * @return {BigInteger}
 * @public
 */
BigIntegerViolator.m_fromDouble__double = function(v) {
  BigIntegerViolator.$clinit();
  return BigInteger.m_valueOf__double_$pp_java_math(v);
};
