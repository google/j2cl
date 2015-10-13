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
   * Narrows a number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toByte(instance) {
    return instance << 24 >> 24;
  }

  /**
   * Narrows a number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toChar(instance) {
    return instance & 0xFFFF;
  }

  /**
   * Narrows a number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toShort(instance) {
    return instance << 16 >> 16;
  }

  /**
   * Narrows a number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @private
   */
  static $toInt(instance) {
    return instance | 0;
  }

  /**
   * Widens a byte to a char.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $widenByteToChar(instance) { return Primitives.$toChar(instance); }

  /**
   * Narrows a char to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowCharToByte(instance) { return Primitives.$toByte(instance); }

  /**
   * Narrows a char to a short.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowCharToShort(instance) { return Primitives.$toShort(instance); }

  /**
   * Narrows a short to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowShortToByte(instance) { return Primitives.$toByte(instance); }

  /**
   * Narrows a short to a char.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowShortToChar(instance) { return Primitives.$toChar(instance); }

  /**
   * Narrows an int to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowIntToByte(instance) { return Primitives.$toByte(instance); }

  /**
   * Narrows an int to a byte.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowIntToChar(instance) { return Primitives.$toChar(instance); }

  /**
   * Narrows an int to a short.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowIntToShort(instance) { return Primitives.$toShort(instance); }

  /**
   * Widens a byte to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $widenByteToLong(instance) { return $LongUtils.$fromInt(instance); }

  /**
   * Widens a char to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $widenCharToLong(instance) { return $LongUtils.$fromInt(instance); }

  /**
   * Widens a short to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $widenShortToLong(instance) { return $LongUtils.$fromInt(instance); }

  /**
   * Widens an int to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $widenIntToLong(instance) { return $LongUtils.$fromInt(instance); }

  /**
   * Narrows a float number to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $narrowFloatToLong(instance) {
    return $LongUtils.$fromNumber(instance);
  }

  /**
   * Narrows a double number to a Long.
   *
   * @param {number} instance
   * @return {!$Long}
   * @public
   */
  static $narrowDoubleToLong(instance) {
    return $LongUtils.$fromNumber(instance);
  }

  /**
   * Narrows a Long to a 8-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $narrowLongToByte(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toByte(intValue);
  }

  /**
   * Narrows a Long to a 16-bit number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $narrowLongToChar(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toChar(intValue);
  }

  /**
   * Narrows a Long to a 16-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $narrowLongToShort(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toShort(intValue);
  }

  /**
   * Narrows a Long to a 32-bit signed number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $narrowLongToInt(instance) {
    let intValue = $LongUtils.$toInt(instance);
    return Primitives.$toInt(intValue);
  }

  /**
   * Widens a Long to a float number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $widenLongToFloat(instance) { return $LongUtils.$toNumber(instance); }

  /**
   * Widens a Long to a double number.
   *
   * @param {$Long} instance
   * @return {number}
   * @public
   */
  static $widenLongToDouble(instance) { return $LongUtils.$toNumber(instance); }

  /**
   * Narrows a float number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowFloatToByte(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toByte(roundInt);
  }

  /**
   * Narrows a double number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowDoubleToByte(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toByte(roundInt);
  }

  /**
   * Narrows a float number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowFloatToChar(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toChar(roundInt);
  }

  /**
   * Narrows a double number to a 16-bit number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowDoubleToChar(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toChar(roundInt);
  }

  /**
   * Narrows a float number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowFloatToShort(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toShort(roundInt);
  }

  /**
   * Narrows a double number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowDoubleToShort(instance) {
    let roundInt = Primitives.$roundToInt(instance);
    return Primitives.$toShort(roundInt);
  }

  /**
   * Narrows a float number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowFloatToInt(instance) {
    return Primitives.$roundToInt(instance);
  }

  /**
   * Narrows a double number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $narrowDoubleToInt(instance) {
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
