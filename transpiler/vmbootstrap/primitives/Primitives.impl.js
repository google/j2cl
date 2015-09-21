/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.Primitives$impl');


let $Long = goog.require('nativebootstrap.Long$impl');
let $LongUtils = goog.require('nativebootstrap.LongUtils$impl');
let $int = goog.forwardDeclare('vmbootstrap.primitives.$int$impl');


/**
 * Static Primitive helper.
 */
class Primitives {
  /**
   * Casts a number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toByte(instance) {
    return instance << 24 >> 24;
  }

  /**
   * Casts a number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toChar(instance) {
    return instance & 0xFFFF;
  }

  /**
   * Casts a number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toShort(instance) {
    return instance << 16 >> 16;
  }

  /**
   * Casts a number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toInt(instance) {
    return instance | 0;
  }

  /**
   * Casts a byte to a char.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castByteToChar(instance) {
    return Primitives.$toChar(instance);
  }

  /**
   * Casts a char to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castCharToByte(instance) {
    return Primitives.$toByte(instance);
  }

  /**
   * Casts a char to a short.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castCharToShort(instance) {
    return Primitives.$toShort(instance);
  }

  /**
   * Casts a short to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castShortToByte(instance) {
    return Primitives.$toByte(instance);
  }

  /**
   * Casts a short to a char.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castShortToChar(instance) {
    return Primitives.$toChar(instance);
  }

  /**
   * Casts an int to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castIntToByte(instance) {
    return Primitives.$toByte(instance);
  }

  /**
   * Casts an int to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castIntToChar(instance) {
    return Primitives.$toChar(instance);
  }

  /**
   * Casts an int to a short.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castIntToShort(instance) {
    return Primitives.$toShort(instance);
  }

  /**
   * Casts a byte to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castByteToLong(instance) {
    return $LongUtils.$fromInt(instance);
  }

  /**
   * Casts a char to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castCharToLong(instance) {
    return $LongUtils.$fromInt(instance);
  }

  /**
   * Casts a short to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castShortToLong(instance) {
    return $LongUtils.$fromInt(instance);
  }

  /**
   * Casts an int to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castIntToLong(instance) {
    return $LongUtils.$fromInt(instance);
  }

  /**
   * Casts a float number to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castFloatToLong(instance) {
    return $LongUtils.$fromNumber(instance);
  }

  /**
   * Casts a double number to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $castDoubleToLong(instance) {
    return $LongUtils.$fromNumber(instance);
  }

  /**
   * Casts a Long to a 8-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToByte(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toByte(intValue);
  }

  /**
   * Casts a Long to a 16-bit number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToChar(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toChar(intValue);
  }

  /**
   * Casts a Long to a 16-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToShort(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toShort(intValue);
  }

  /**
   * Casts a Long to a 32-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToInt(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toInt(intValue);
  }

  /**
   * Casts a Long to a float number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToFloat(instance) {
    return $LongUtils.$toNumber(instance);
  }

  /**
   * Casts a Long to a double number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $castLongToDouble(instance) {
    return $LongUtils.$toNumber(instance);
  }

  /**
   * Casts a float number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castFloatToByte(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toByte(roundInt);
  }

  /**
   * Casts a double number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castDoubleToByte(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toByte(roundInt);
  }

  /**
   * Casts a float number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castFloatToChar(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toChar(roundInt);
  }

  /**
   * Casts a double number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castDoubleToChar(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toChar(roundInt);
  }

  /**
   * Casts a float number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castFloatToShort(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toShort(roundInt);
  }

  /**
   * Casts a double number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castDoubleToShort(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toShort(roundInt);
  }

  /**
   * Casts a float number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castFloatToInt(instance) {
    return Primitives.$roundToInt(instance);
  }

  /**
   * Casts a double number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castDoubleToInt(instance) {
    return Primitives.$roundToInt(instance);
  }

  /**
   * Rounds to an integral value.
   * @param {number} value
   * @return {number}
   * @private
   */
  static $roundToInt(value) {
    Primitives.$clinit();
    return Math.max(Math.min(value, $int.MAX_VALUE), $int.MIN_VALUE) | 0;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    $int = goog.module.get('vmbootstrap.primitives.$int$impl');
  }
};


/**
 * Exported class.
 */
exports = Primitives;
