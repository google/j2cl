/**
 * Impl transpiled from java.util.InternalJsIteratorEntry.
 */
goog.module('gen.java.util.InternalJsIteratorEntry$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');


/**
 * @template C_V
 */
class InternalJsIteratorEntry extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
    this._entry = null;
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__java_util_InternalJsIteratorEntry() {
  }

  /**
   * A particular Java constructor as a factory method.
   * @template C_V
   * @return {!InternalJsIteratorEntry<C_V>}
   * @public
   */
  static $create(entry) {
    InternalJsIteratorEntry.$clinit();
    let instance = new InternalJsIteratorEntry;
    instance._entry = entry;
    instance.$ctor__java_util_InternalJsIteratorEntry();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @public
   */
  $ctor__java_util_InternalJsIteratorEntry() {
    this.$ctor__java_lang_Object();
    this.$init__java_util_InternalJsIteratorEntry();
  }

  /**
   * @return {boolean}
   * @public
   */
  m_done() {
    return this._entry.done;
  }

  /**
   * @return {?string}
   * @public
   */
  m_getKey() {
    return this._entry.value[0];
  }

  /**
   * @return {C_V}
   * @public
   */
  m_getValue() {
    return this._entry.value[1];
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return instance instanceof InternalJsIteratorEntry; }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, InternalJsIteratorEntry);
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    InternalJsIteratorEntry.$clinit();
    if (!InternalJsIteratorEntry.$classInternalJsIteratorEntry_) {
      InternalJsIteratorEntry.$classInternalJsIteratorEntry_ = Class.$createForClass(
          $Util.$generateId('InternalJsIteratorEntry'),
          $Util.$generateId('java.util.InternalJsIteratorEntry'),
          Object.$getClass(),
          $Util.$generateId('java.util.InternalJsIteratorEntry'));
    }
    return InternalJsIteratorEntry.$classInternalJsIteratorEntry_;
  }

  /**
   * @override
   * @return {Class}
   * @public
   */
  m_getClass() { return InternalJsIteratorEntry.$getClass(); }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    Object.$clinit();
  }};


/**
 * The class literal field.
 * @private {Class}
 */
InternalJsIteratorEntry.$classInternalJsIteratorEntry_ = null;



/**
 * Export class.
 */
exports = InternalJsIteratorEntry;
