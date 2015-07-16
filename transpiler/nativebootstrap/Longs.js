goog.module('nativebootstrap.LongsModule');


var Long = goog.require('goog.math.Long');


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
class Longs {
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
   * @param {!Long} valueLong The value to decrement.
   * @return {!Long} The Long value one smaller than the given value.
   * @public
   */
  static $decrement(valueLong) { return valueLong.subtract(Long.getOne()); }

  /**
   * @param {!Long} leftLong The left Long in the operation.
   * @param {!Long} rightLong The right Long in the operation.
   * @return {!Long} The division of left by right.
   * @public
   */
  static $divide(leftLong, rightLong) { return leftLong.div(rightLong); }

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
   * @param {!Long} valueLong The value to increment.
   * @return {!Long} The Long value resulting from adding one to the
   *         given value.
   * @public
   */
  static $increment(valueLong) { return valueLong.add(Long.getOne()); }

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
  static $remainder(leftLong, rightLong) { return leftLong.modulo(rightLong); }

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
};


/**
 * Exported class.
 */
exports.Longs = Longs;


/**
 * Exported class.
 */
exports.Long = Long;
