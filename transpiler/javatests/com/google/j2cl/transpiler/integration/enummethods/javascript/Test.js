/**
 * @fileoverview Test that Enum's ordinal() and name() are exposed to js.
 */
goog.module('foo.test.Test');

class Test {
  /**
   * @param {*} anyEnum
   * @return {?string}
   */
  static getName(anyEnum) {
    return anyEnum.name();
  }

  /**
   * @param {*} anyEnum
   * @return {number}
   */
  static getOrdinal(anyEnum) {
    return anyEnum.ordinal();
  }

  /**
   * @param {*} enumA
   * @param {*} enumB
   * @return {number}
   */
  static compare(enumA, enumB) {
    return enumA.compareTo(enumB);
  }
}

exports = Test;
