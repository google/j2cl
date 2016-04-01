/**
 * Implementation hand rolled.
 */
goog.module('vmbootstrap.CharSequences$impl');

let CharSequence = goog.forwardDeclare('gen.java.lang.CharSequence$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');


/**
 * Provides devirtualized method implementations for CharSequence.
 */
class CharSequences {
  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @return {number}
   * @public
   */
  static m_length__java_lang_CharSequence(obj) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_length__java_lang_String(obj);
    }
    return obj.m_length();
  }

  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @param {number} index
   * @return {number}
   */
  static m_charAt__java_lang_CharSequence__int(obj, index) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_charAt__java_lang_String__int(obj, index);
    }
    return obj.m_charAt__int(index);
  }

  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @param {number} start
   * @param {number} end
   * @return {CharSequence|string}
   */
  static m_subSequence__java_lang_CharSequence__int__int(obj, start, end) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_subSequence__java_lang_String__int__int(obj, start, end);
    }
    return obj.m_subSequence__int__int(start, end);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    CharSequence = goog.module.get('gen.java.lang.CharSequence$impl');
    String = goog.module.get('gen.java.lang.String$impl');
  }
}


/**
 * Exported class.
 */
exports = CharSequences;
