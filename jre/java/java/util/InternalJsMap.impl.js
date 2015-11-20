/**
 * Impl transpiled from java.util.InternalJsMap.
 */
goog.module('gen.java.util.InternalJsMap$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let InternalJsIterator = goog.forwardDeclare('gen.java.util.InternalJsIterator$impl');
let $int = goog.forwardDeclare('vmbootstrap.primitives.$int$impl');


/**
 * @template C_V
 */
class InternalJsMap extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
    this._map = new Map();
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__java_util_InternalJsMap() {
  }

  /**
   * A particular Java constructor as a factory method.
   * @template C_V
   * @return {!InternalJsMap<C_V>}
   * @public
   * @nocollapse
   */
  static $create() {
    InternalJsMap.$clinit();
    let instance = new InternalJsMap;
    instance.$ctor__java_util_InternalJsMap();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @public
   */
  $ctor__java_util_InternalJsMap() {
    this.$ctor__java_lang_Object();
    this.$init__java_util_InternalJsMap();
  }

  /**
   * @param {number} key
   * @return {C_V}
   * @public
   */
  m_get__int(key) {
    return this._map.get(key);
  }

  /**
   * @param {?string} key
   * @return {C_V}
   * @public
   */
  m_get__java_lang_String(key) {
    return this._map.get(key);
  }

  /**
   * @param {number} key
   * @param {C_V} value
   * @return {void}
   * @public
   */
  m_set__int__java_lang_Object(key, value) {
    this._map.set(key, value);
  }

  /**
   * @param {?string} key
   * @param {C_V} value
   * @return {void}
   * @public
   */
  m_set__java_lang_String__java_lang_Object(key, value) {
    this._map.set(key, value);
  }

  /**
   * @param {number} key
   * @return {void}
   * @public
   */
  m_delete__int(key) {
    this._map['delete'](key);
  }

  /**
   * @param {?string} key
   * @return {void}
   * @public
   */
  m_delete__java_lang_String(key) {
    this._map['delete'](key);
  }

  /**
   * @return {InternalJsIterator<C_V>}
   * @public
   */
  m_entries() {
    return InternalJsIterator.$create(this._map.entries());
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) { return instance instanceof InternalJsMap; }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, InternalJsMap);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    InternalJsMap.$clinit();
    if (!InternalJsMap.$classInternalJsMap_) {
      InternalJsMap.$classInternalJsMap_ = Class.$createForClass(
          $Util.$generateId('InternalJsMap'),
          $Util.$generateId('java.util.InternalJsMap'),
          Object.$getClass(),
          $Util.$generateId('java.util.InternalJsMap'));
    }
    return InternalJsMap.$classInternalJsMap_;
  }

  /**
   * @override
   * @return {Class}
   * @public
   */
  m_getClass() { return InternalJsMap.$getClass(); }

  /**
   * Runs inline static field initializers.
   * @public
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    InternalJsIterator = goog.module.get('gen.java.util.InternalJsIterator$impl');
    $int = goog.module.get('vmbootstrap.primitives.$int$impl');
    Object.$clinit();
  }};


/**
 * The class literal field.
 * @private {Class}
 */
InternalJsMap.$classInternalJsMap_ = null;



/**
 * Export class.
 */
exports = InternalJsMap;
