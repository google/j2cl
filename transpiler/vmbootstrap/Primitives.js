goog.module('vmbootstrap.PrimitivesModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Object = goog.require('gen.java.lang.CoreModule').Object;
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
 * @public {?}
 */
$byte.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$byte.$class =
    Class.$createForClass(Util.$generateId('byte'), Util.$generateId('byte'),
                          Object.$class, Util.$generateId('byte'));


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
 * @public {?}
 */
$short.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$short.$class =
    Class.$createForClass(Util.$generateId('short'), Util.$generateId('short'),
                          Object.$class, Util.$generateId('short'));


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
 * @public {?}
 */
$int.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$int.$class =
    Class.$createForClass(Util.$generateId('int'), Util.$generateId('int'),
                          Object.$class, Util.$generateId('int'));


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
 * @public {?}
 */
$long.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$long.$class =
    Class.$createForClass(Util.$generateId('long'), Util.$generateId('long'),
                          Object.$class, Util.$generateId('long'));


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
 * @public {?}
 */
$float.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$float.$class =
    Class.$createForClass(Util.$generateId('float'), Util.$generateId('float'),
                          Object.$class, Util.$generateId('float'));


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
 * @public {?}
 */
$double.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$double.$class = Class.$createForClass(
    Util.$generateId('double'), Util.$generateId('double'), Object.$class,
    Util.$generateId('double'));


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
 * @public {?}
 */
$char.$initialArrayValue = 0;


/**
 * @public {Class}
 */
$char.$class =
    Class.$createForClass(Util.$generateId('char'), Util.$generateId('char'),
                          Object.$class, Util.$generateId('char'));


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
 * @public {?}
 */
$boolean.$initialArrayValue = false;


/**
 * @public {Class}
 */
$boolean.$class = Class.$createForClass(
    Util.$generateId('boolean'), Util.$generateId('boolean'), Object.$class,
    Util.$generateId('boolean'));


/**
 * Exported class.
 */
exports.$boolean = $boolean;
