/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.LongUtils$impl');


let Long = goog.require('nativebootstrap.Long$impl');
let Primitives = goog.require('vmbootstrap.primitives.Primitives$impl');


/**
 * Defines utility static functions that map from transpiled Long instantiation
 * and arithmetic operations to some particular Long emulation library. (In this
 * case Closure's goog.math.Long)
 * <p>
 * For performance consider replacing these utility functions with ones that
 * route to native JS's double when the application doesn't need > 2^53 longs.
 * <p>
 * Does not expose any global static fields such as ZERO or ONE since the
 * initialization of their values would execute the Long class's constructor and
 * if the constructor appeared to have side effects (for instance if it was
 * maintaining a private cache of Long instances) then the Long class would
 * become unstrippable even if these global constants were never used.
 */
class LongUtils {
  /**
   * Compares two Longs.
   * @param {!Long} a the first long for comparison.
   * @param {!Long} b the second long for comparison.
   * @return {number} 0 if they are the same, 1 if the this is greater, and -1
   *     if the given one is greater.
   */
  static $compare(a, b) { return a.compare(b); }

  /**
   * @param {string} longString A Long valueLong represented in readable string
   *          format.
   * @param {number=} opt_radix The radix in which the text is written.
   * @return {!Long} A Long corresponding to the given string.
   * @public
   */
  static $fromString(longString, opt_radix) {
    return Long.fromString(longString, opt_radix);
  }

  /**
   * @param {number} longInDouble A double holding a small enough long value.
   * @return {!Long} A Long corresponding to the given double.
   * @public
   */
  static $fromInt(longInDouble) { return Long.fromInt(longInDouble); }

  /**
   * @param {number} value A double value.
   * @return {!Long} A Long corresponding to the given double.
   * @public
   */
  static $fromNumber(value) { return Long.fromNumber(value); }

  /**
   * @param {Long} longValue A Long value.
   * @return {number} A 32-bit integer corresponding to the given Long.
   * @public
   */
  static $toInt(longValue) { return longValue.toInt(); }

  /**
   * @param {Long} longValue A Long value.
   * @return {number} The closest floating-point value to the given Long.
   * @public
   */
  static $toNumber(longValue) { return longValue.toNumber(); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The bitwise and of left and right.
   * @public
   */
  static $and(leftLong, rightLong) { return leftLong.and(rightLong); }

  /**
   * @param {!Long} valueLong The value to bitwise not.
   * @return {!Long} The bitwise not of the given value.
   * @public
   */
  static $not(valueLong) { return valueLong.not(); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The division of left by right.
   * @public
   */
  static $divide(leftLong, rightLong) {
    LongUtils.checkDivisorZero(rightLong);
    return leftLong.div(rightLong);
  }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether the values in left and right are equal.
   * @public
   */
  static $equals(leftLong, rightLong) { return leftLong.equals(rightLong); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether left is larger than right.
   * @public
   */
  static $greater(leftLong, rightLong) {
    return leftLong.greaterThan(rightLong);
  }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether left is larger than or equal to right.
   * @public
   */
  static $greaterEquals(leftLong, rightLong) {
    return leftLong.greaterThanOrEqual(rightLong);
  }

  /**
   * @param {!Long} valueLong The starting value to shift.
   * @param {number} numBits The number of bits to shift to the left.
   * @return {!Long} A Long representing the left shifted value.
   * @public
   */
  static $leftShift(valueLong, numBits) { return valueLong.shiftLeft(numBits); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether left is smaller than right.
   * @public
   */
  static $less(leftLong, rightLong) { return leftLong.lessThan(rightLong); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether left is smaller than or equal to right.
   * @public
   */
  static $lessEquals(leftLong, rightLong) {
    return leftLong.lessThanOrEqual(rightLong);
  }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The Long value resulting from subtracting right
   *         from left.
   * @public
   */
  static $minus(leftLong, rightLong) { return leftLong.subtract(rightLong); }

  /**
   * @param {!Long} valueLong The value to negate.
   * @return {!Long} The Long value after multiplying by -1.
   * @public
   */
  static $negate(valueLong) { return valueLong.negate(); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {boolean} Whether left is not equal to right.
   * @public
   */
  static $notEquals(leftLong, rightLong) {
    return leftLong.notEquals(rightLong);
  }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The bitwise or of left and right.
   * @public
   */
  static $or(leftLong, rightLong) { return leftLong.or(rightLong); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The Long value resulting from adding left and
   *         right.
   * @public
   */
  static $plus(leftLong, rightLong) { return leftLong.add(rightLong); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The Long remainder of left divided by right.
   * @public
   */
  static $remainder(leftLong, rightLong) {
    LongUtils.checkDivisorZero(rightLong);
    return leftLong.modulo(rightLong);
  }

  /**
   * @param {!Long} valueLong The starting value to shift.
   * @param {number} numBits The number of bits to shift to the right.
   * @return {!Long} A Long representing the right shifted value.
   * @public
   */
  static $rightShiftSigned(valueLong, numBits) {
    return valueLong.shiftRight(numBits);
  }

  /**
   * @param {!Long} valueLong The starting value to shift.
   * @param {number} numBits The number of bits to shift to the right.
   * @return {!Long} A Long representing the right shifted value.
   * @public
   */
  static $rightShiftUnsigned(valueLong, numBits) {
    return valueLong.shiftRightUnsigned(numBits);
  }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The Long value resulting from multiplying left
   *         and right.
   * @public
   */
  static $times(leftLong, rightLong) { return leftLong.multiply(rightLong); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The bitwise xor of left and right.
   * @public
   */
  static $xor(leftLong, rightLong) { return leftLong.xor(rightLong); }

  /**
   * @param {!Long} valueLong The value to get high bits from.
   * @return {number} The high 32-bits as a signed value.
   * @public
   */
  static $getHighBits(valueLong) { return valueLong.getHighBits(); }

  /**
   * @param {!Long} valueLong The long value to get low bits from.
   * @return {number} The low 32-bits as a signed value.
   * @public
   */
  static $getLowBits(valueLong) { return valueLong.getLowBits(); }

  /**
   * @param {!Long} valueLong The long value.
   * @return {string} The textual representation of this value.
   * @public
   */
  static $toString(valueLong) { return valueLong.toString(); }

  /**
   * If the divisor is 0 we throw an arithmetic exception.
   * @param {!Long} divisor
   * @private
   */
  static checkDivisorZero(divisor) {
    if (divisor.isZero()) {
      Primitives.$throwArithmeticException();
    }
  }
};


/**
 * Exported class.
 */
exports = LongUtils;
