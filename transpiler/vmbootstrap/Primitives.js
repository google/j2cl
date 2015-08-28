goog.module('vmbootstrap.PrimitivesModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var LongUtils = goog.require('nativebootstrap.LongUtilsModule').LongUtils;
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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $byte;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$byte.$classByte) {
      $byte.$classByte = Class.$createForPrimitive('byte');
    }
    return $byte.$classByte;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$byte.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $short;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$short.$classShort) {
      $short.$classShort = Class.$createForPrimitive('short');
    }
    return $short.$classShort;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$short.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $int;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$int.$classInt) {
      $int.$classInt = Class.$createForPrimitive('int');
    }
    return $int.$classInt;
  }
};


/**
 * @public {number}
 * @nocollapse
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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $long;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$long.$classLong) {
      $long.$classLong = Class.$createForPrimitive('long');
    }
    return $long.$classLong;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$long.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $float;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$float.$classFloat) {
      $float.$classFloat = Class.$createForPrimitive('float');
    }
    return $float.$classFloat;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$float.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $double;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$double.$classDouble) {
      $double.$classDouble = Class.$createForPrimitive('double');
    }
    return $double.$classDouble;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$double.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $char;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$char.$classChar) {
      $char.$classChar = Class.$createForPrimitive('char');
    }
    return $char.$classChar;
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$char.$initialArrayValue = 0;


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
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'boolean'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $boolean;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$boolean.$classBoolean) {
      $boolean.$classBoolean = Class.$createForPrimitive('boolean');
    }
    return $boolean.$classBoolean;
  }
};


/**
 * @public {boolean}
 * @nocollapse
 */
$boolean.$initialArrayValue = false;


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
   * @nocollapse
   */
  static $isInstance(instance) { return false; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $void;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!$void.$classVoid) {
      $void.$classVoid = Class.$createForPrimitive('void');
    }
    return $void.$classVoid;
  }
};


/**
 * @public {boolean}
 * @nocollapse
 */
$void.$initialArrayValue = false;


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
    return LongUtils.$fromInt(instance);
  }

  /**
   * Casts a char to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castCharToLong(instance) {
    return LongUtils.$fromInt(instance);
  }

  /**
   * Casts a short to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castShortToLong(instance) {
    return LongUtils.$fromInt(instance);
  }

  /**
   * Casts an int to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castIntToLong(instance) {
    return LongUtils.$fromInt(instance);
  }

  /**
   * Casts a float number to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castFloatToLong(instance) {
    return LongUtils.$fromNumber(instance);
  }

  /**
   * Casts a double number to a Long.
   *
   * @param {number} instance
   * @return {!Long}
   * @public
   */
  static $castDoubleToLong(instance) {
    return LongUtils.$fromNumber(instance);
  }

  /**
   * Casts a Long to a 8-bit signed number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToByte(instance) {
    var intValue = LongUtils.$toInt(instance);
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
    var intValue = LongUtils.$toInt(instance);
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
    var intValue = LongUtils.$toInt(instance);
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
    var intValue = LongUtils.$toInt(instance);
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
    return LongUtils.$toNumber(instance);
  }

  /**
   * Casts a Long to a double number.
   *
   * @param {Long} instance
   * @return {number}
   * @public
   */
  static $castLongToDouble(instance) {
    return LongUtils.$toNumber(instance);
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
