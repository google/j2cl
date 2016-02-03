/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Enums$impl');

let IllegalArgumentException =
    goog.forwardDeclare('gen.java.lang.IllegalArgumentException$impl');
let Exceptions = goog.forwardDeclare('vmbootstrap.Exceptions$impl');

class Enums {
  /**
   * @param {Array<*>} values
   * @return {Object<string, *>}
   * @public
   */
  static createMapFromValues(values) {
    /** @type {Object<string, *>} */
    let map = {};
    for (var i = 0; i < values.length; i++) {
      /** @type {string} */
      let name = values[i].m_name();
      map[name] = values[i];
    }
    return map;
  }

  /**
   * We need to throw an exception if the name does not exist in the map.
   * @public
   */
  static getValueFromNameAndMap(name, map) {
    Enums.$clinit();
    if (name == null) {
      throw Exceptions.toJs(IllegalArgumentException.$create());
    }
    let enumValue = map[name];
    if (enumValue == undefined) {
      throw Exceptions.toJs(IllegalArgumentException.$create());
    }
    return enumValue;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    IllegalArgumentException =
        goog.module.get('gen.java.lang.IllegalArgumentException$impl');
    Exceptions = goog.module.get('vmbootstrap.Exceptions$impl');
  }
};

exports = Enums;
