goog.module('vmbootstrap.PrimitivesModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongsModule').Long;
var Longs = goog.require('nativebootstrap.LongsModule').Longs;
var Util = goog.require('nativebootstrap.UtilModule').Util;


/**
 * Placeholder class definition for the primitive class byte.
 *
 * Non-instantiable.
 */
class $byte {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $byte;
  }
};


/**
 * @public {number}
 */
$byte.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$byte.$class = Class.$createForPrimitive('byte');


/**
 * Exported class.
 */
exports.$byte = $byte;


/**
 * Placeholder class definition for the primitive class short.
 *
 * Non-instantiable.
 */
class $short {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $short;
  }
};


/**
 * @public {number}
 */
$short.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$short.$class = Class.$createForPrimitive('short');


/**
 * Exported class.
 */
exports.$short = $short;


/**
 * Placeholder class definition for the primitive class int.
 *
 * Non-instantiable.
 */
class $int {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $int;
  }
};


/**
 * @public {number}
 */
$int.$initialArrayValue = 0;


/**
 * @public {number}
 */
$int.MAX_VALUE = 0x7fffffff;


/**
 * @public {number}
 */
$int.MIN_VALUE = -0x80000000;


/**
 * @public {Class}
 */
$int.$class = Class.$createForPrimitive('int');


/**
 * Exported class.
 */
exports.$int = $int;


/**
 * Placeholder class definition for the primitive class long.
 *
 * Non-instantiable.
 */
class $long {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $long;
  }
};


/**
 * @public {number}
 */
$long.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$long.$class = Class.$createForPrimitive('long');


/**
 * Exported class.
 */
exports.$long = $long;


/**
 * Placeholder class definition for the primitive class float.
 *
 * Non-instantiable.
 */
class $float {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $float;
  }
};


/**
 * @public {number}
 */
$float.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$float.$class = Class.$createForPrimitive('float');


/**
 * Exported class.
 */
exports.$float = $float;


/**
 * Placeholder class definition for the primitive class double.
 *
 * Non-instantiable.
 */
class $double {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $double;
  }
};


/**
 * @public {number}
 */
$double.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$double.$class = Class.$createForPrimitive('double');


/**
 * Exported class.
 */
exports.$double = $double;


/**
 * Placeholder class definition for the primitive class char.
 *
 * Non-instantiable.
 */
class $char {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $char;
  }
};


/**
 * @public {number}
 */
$char.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$char.$class = Class.$createForPrimitive('char');


/**
 * Exported class.
 */
exports.$char = $char;


/**
 * Placeholder class definition for the primitive class boolean.
 *
 * Non-instantiable.
 */
class $boolean {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return typeof instance === 'boolean'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $boolean;
  }
};


/**
 * @public {boolean}
 */
$boolean.$initialArrayValue = false;


/**
 * @public {Class}
 */
$boolean.$class = Class.$createForPrimitive('boolean');


/**
 * Exported class.
 */
exports.$boolean = $boolean;

/**
 * Placeholder class definition for the primitive class void.
 *
 * Non-instantiable.
 */
class $void {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return false; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $void;
  }
};


/**
 * @public {boolean}
 */
$void.$initialArrayValue = false;


/**
 * @public {Class}
 */
$void.$class = Class.$createForPrimitive('void');


/**
 * Exported class.
 */
exports.$void = $void;


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
   * @return {!Long}
   * @public
   */
  static $castByteToLong(instance) {
    return Longs.$fromInt(instance);
  }

  /**
   * Casts a char to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castCharToLong(instance) {
    return Longs.$fromInt(instance);
  }

  /**
   * Casts a short to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castShortToLong(instance) {
    return Longs.$fromInt(instance);
  }

  /**
   * Casts an int to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castIntToLong(instance) {
    return Longs.$fromInt(instance);
  }

  /**
   * Casts a float number to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castFloatToLong(instance) {
    return Longs.$fromNumber(instance);
  }

  /**
   * Casts a double number to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castDoubleToLong(instance) {
    return Longs.$fromNumber(instance);
  }

  /**
   * Casts a Long to a 8-bit signed number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToByte(instance) {
    var intValue = Longs.$toInt(instance);
    return Primitives.$toByte(intValue);
  }

  /**
   * Casts a Long to a 16-bit number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToChar(instance) {
    var intValue = Longs.$toInt(instance);
    return Primitives.$toChar(intValue);
  }

  /**
   * Casts a Long to a 16-bit signed number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToShort(instance) {
    var intValue = Longs.$toInt(instance);
    return Primitives.$toShort(intValue);
  }

  /**
   * Casts a Long to a 32-bit signed number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToInt(instance) {
    var intValue = Longs.$toInt(instance);
    return Primitives.$toInt(intValue);
  }

  /**
   * Casts a Long to a float number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToFloat(instance) {
    return Longs.$toNumber(instance);
  }

  /**
   * Casts a Long to a double number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToDouble(instance) {
    return Longs.$toNumber(instance);
  }

  /**
   * Casts a float number to a 8-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  static $castFloatToByte(instance) {
    var roundInt = Primitives.$roundToInt(instance);
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
    var roundInt = Primitives.$roundToInt(instance);
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
    var roundInt = Primitives.$roundToInt(instance);
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
    var roundInt = Primitives.$roundToInt(instance);
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
    var roundInt = Primitives.$roundToInt(instance);
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
    var roundInt = Primitives.$roundToInt(instance);
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
    return Math.max(Math.min(value, $int.MAX_VALUE), $int.MIN_VALUE) | 0;
  }
};


/**
 * Exported class.
 */
exports.Primitives = Primitives;
