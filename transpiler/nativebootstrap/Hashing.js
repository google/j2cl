goog.module('nativebootstrap.HashingModule');


/**
 * Utility functions for setting and retrieving system level hashcodes.
 */
class Hashing {
  /**
   * Gets a hash code on the passed-in object.
   *
   * @param {Object} obj
   * @return {number}
   * @public
   */
  static $getHashCode(obj) {
    return obj.$systemHashCode ||
           (window.Object.defineProperties(
                obj,
                {
                  $systemHashCode:
                      {value: Hashing.$getNextHashId(), enumerable: false}
                }),
            obj.$systemHashCode);
  }

  /**
   * Gets the next hash code.
   *
   * @return {number}
   * @private
   */
  static $getNextHashId() { return ++Hashing.$nextHashId_; }
};


/**
 * @private {number}
 */
Hashing.$nextHashId_ = 0;


/**
 * Exported class.
 */
exports.Hashing = Hashing;
