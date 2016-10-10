/**
 * Impl hand rolled.
 */
goog.module('nativebootstrap.Hashing$impl');


/**
 * Utility functions for setting and retrieving system level hashcodes.
 */
class Hashing {
  /**
   * Gets a hash code on the passed-in object.
   *
   * @param {*} obj
   * @return {number}
   * @public
   */
  static $getHashCode(obj) {
    let o = /** @type {Object} */ (obj);
    return o.$systemHashCode || (window.Object.defineProperties(o, {
             $systemHashCode:
                 {value: Hashing.$getNextHashId(), enumerable: false}
           }),
                                 o.$systemHashCode);
  }

  /**
   * Gets the next hash code.
   *
   * @return {number}
   * @private
   */
  static $getNextHashId() {
    return ++Hashing.$nextHashId_;
  }
};


/**
 * @private {number}
 */
Hashing.$nextHashId_ = 0;


/**
 * Exported class.
 */
exports = Hashing;
