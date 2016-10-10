/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Enums$impl');

let IllegalArgumentException = goog.forwardDeclare('java.lang.IllegalArgumentException$impl');
let Exceptions = goog.forwardDeclare('vmbootstrap.Exceptions$impl');
let Enum = goog.forwardDeclare('java.lang.Enum$impl');

class Enums {
  /**
   * @param {Array<T>} values An array containing all instances of a particular
   *     enum type.
   * @return {Object<string, T>} A map from enum name to enum instance.
   * @template T
   * @public
   */
  static createMapFromValues(values) {
    let map = {};
    for (var i = 0; i < values.length; i++) {
      let name = /** @type {Enum} */ (values[i]).name();
      map[name] = values[i];
    }
    return map;
  }

  /**
   * We need to throw an exception if the name does not exist in the map.
   * @param {?string} name
   * @param {Object<string, T>} map
   * @return {T}
   * @template T
   * @public
   */
  static getValueFromNameAndMap(name, map) {
    Enums.$clinit();
    if (name == null) {
      throw Exceptions.toJs(IllegalArgumentException.$create__());
    }
    let enumValue = map[name];
    if (enumValue == undefined) {
      throw Exceptions.toJs(IllegalArgumentException.$create__());
    }
    return enumValue;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    IllegalArgumentException =
        goog.module.get('java.lang.IllegalArgumentException$impl');
    Exceptions = goog.module.get('vmbootstrap.Exceptions$impl');
    Enum = goog.module.get('java.lang.Enum$impl');
  }
};

exports = Enums;
