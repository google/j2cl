/**
 * Super sourced Impl for javaemul.internal.JsDate.
 */
goog.module('gen.javaemul.internal.JsDate$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');
let $int = goog.forwardDeclare('vmbootstrap.primitives.$int$impl');


// TODO(dankurka): once j2cl has js interop this super sourcing needs to go away
// and JsDate.java needs to become a js type.
class JsDate extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
    /** @private {Date} The wrapped date. */
    this.__date = null;
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__javaemul_internal_JsDate() {}

  /**
   * @return {JsDate}
   * @public
   */
  static m_create() {
    let d = JsDate.$create();
    d.__date = new Date();
    return d;
  }

  /**
   * @param {number} milliseconds
   * @return {JsDate}
   * @public
   */
  static m_create__double(milliseconds) {
    let d = JsDate.$create();
    d.__date = new Date(milliseconds);
    return d;
  }

  /**
   * @param {number} year
   * @param {number} month
   * @param {number} dayOfMonth
   * @param {number} hours
   * @param {number} minutes
   * @param {number} seconds
   * @param {number} millis
   * @return {JsDate}
   * @public
   */
  static m_create__int__int__int__int__int__int__int(year, month, dayOfMonth,
                                                     hours, minutes, seconds,
                                                     millis) {
    let d = JsDate.$create();
    d.__date =
        new Date(year, month, dayOfMonth, hours, minutes, seconds, millis);
    return d;
  }

  /**
   * @param {?string} dateString
   * @return {number}
   * @public
   */
  static m_parse__java_lang_String(dateString) {
    return Date.parse(dateString);
  }

  /**
   * @param {number} year
   * @param {number} month
   * @param {number} dayOfMonth
   * @param {number} hours
   * @param {number} minutes
   * @param {number} seconds
   * @param {number} millis
   * @return {number}
   * @public
   */
  static m_UTC__int__int__int__int__int__int__int(year, month, dayOfMonth,
                                                  hours, minutes, seconds,
                                                  millis) {
    return Date.UTC(year, month, dayOfMonth, hours, minutes, seconds, millis);
  }

  /**
   * A particular Java constructor as a factory method.
   * @return {!JsDate}
   * @public
   * @nocollapse
   */
  static $create() {
    JsDate.$clinit();
    let instance = new JsDate;
    instance.$ctor__javaemul_internal_JsDate();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @public
   */
  $ctor__javaemul_internal_JsDate() {
    this.$ctor__java_lang_Object();
    this.$init__javaemul_internal_JsDate();
  }

  /**
   * @return {number}
   * @public
   */
  m_getDate() { return this.__date.getDate(); }

  /**
   * @return {number}
   * @public
   */
  m_getDay() { return this.__date.getDay(); }

  /**
   * @return {number}
   * @public
   */
  m_getFullYear() { return this.__date.getFullYear(); }

  /**
   * @return {number}
   * @public
   */
  m_getHours() { return this.__date.getHours(); }

  /**
   * @return {number}
   * @public
   */
  m_getMilliseconds() { return this.__date.getMilliseconds(); }

  /**
   * @return {number}
   * @public
   */
  m_getMinutes() { return this.__date.getMinutes(); }

  /**
   * @return {number}
   * @public
   */
  m_getMonth() { return this.__date.getMonth(); }

  /**
   * @return {number}
   * @public
   */
  m_getSeconds() { return this.__date.getSeconds(); }

  /**
   * @return {number}
   * @public
   */
  m_getTime() { return this.__date.getTime(); }

  /**
   * @return {number}
   * @public
   */
  m_getTimezoneOffset() { return this.__date.getTimezoneOffset(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCDate() { return this.__date.getUTCDate(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCFullYear() { return this.__date.getUTCFullYear(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCHours() { return this.__date.getUTCHours(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCMinutes() { return this.__date.getUTCMinutes(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCMonth() { return this.__date.getUTCMonth(); }

  /**
   * @return {number}
   * @public
   */
  m_getUTCSeconds() { return this.__date.getUTCSeconds(); }

  /**
   * @param {number} dayOfMonth
   * @return {void}
   * @public
   */
  m_setDate__int(dayOfMonth) { this.__date.setDate(dayOfMonth); }

  /**
   * @param {number} year
   * @return {void}
   * @public
   */
  m_setFullYear__int(year) { this.__date.setFullYear(year); }

  /**
   * @param {number} year
   * @param {number} month
   * @param {number} day
   * @return {void}
   * @public
   */
  m_setFullYear__int__int__int(year, month, day) {
    this.__date.setFullYear(year, month, day);
  }

  /**
   * @param {number} hours
   * @return {void}
   * @public
   */
  m_setHours__int(hours) { this.__date.setHours(hours); }

  /**
   * @param {number} hours
   * @param {number} mins
   * @param {number} secs
   * @param {number} ms
   * @return {void}
   * @public
   */
  m_setHours__int__int__int__int(hours, mins, secs, ms) {
    this.__date.setHours(hours, mins, secs, ms);
  }

  /**
   * @param {number} minutes
   * @return {void}
   * @public
   */
  m_setMinutes__int(minutes) { this.__date.setMinutes(minutes); }

  /**
   * @param {number} month
   * @return {void}
   * @public
   */
  m_setMonth__int(month) { this.__date.setMonth(month); }

  /**
   * @param {number} seconds
   * @return {void}
   * @public
   */
  m_setSeconds__int(seconds) { this.__date.setSeconds(seconds); }

  /**
   * @param {number} milliseconds
   * @return {void}
   * @public
   */
  m_setTime__double(milliseconds) { this.__date.setTime(milliseconds); }

  /**
   * @return {?string}
   * @public
   */
  m_toLocaleString() { return this.__date.toLocaleString(); }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) { return instance instanceof JsDate; }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, JsDate);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    JsDate.$clinit();
    if (!JsDate.$classJsDate_) {
      JsDate.$classJsDate_ = Class.$createForClass(
          $Util.$generateId('JsDate'),
          $Util.$generateId('javaemul.internal.JsDate'), Object.$getClass(),
          $Util.$generateId('javaemul.internal.JsDate'));
    }
    return JsDate.$classJsDate_;
  }

  /**
   * @override
   * @return {Class}
   * @public
   */
  m_getClass() { return JsDate.$getClass(); }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
    $int = goog.module.get('vmbootstrap.primitives.$int$impl');
    Object.$clinit();
  }
};


/**
 * The class literal field.
 * @private {Class}
 */
JsDate.$classJsDate_ = null;


/**
 * Export class.
 */
exports = JsDate;
