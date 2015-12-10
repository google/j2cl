/**
 * Impl transpiled from java.util.InternalJsMapFactory.
 */
goog.module('gen.java.util.InternalJsMapFactory$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let InternalJsMap = goog.forwardDeclare('gen.java.util.InternalJsMap$impl');


class InternalJsMapFactory extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__java_util_InternalJsMapFactory() {
  }

  /**
   * A particular Java constructor as a factory method.
   * @return {!InternalJsMapFactory}
   * @public
   */
  static $create() {
    InternalJsMapFactory.$clinit();
    let instance = new InternalJsMapFactory;
    instance.$ctor__java_util_InternalJsMapFactory();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @public
   */
  $ctor__java_util_InternalJsMapFactory() {
    this.$ctor__java_lang_Object();
    this.$init__java_util_InternalJsMapFactory();
  }

  /**
   * @template M_V
   * @return {InternalJsMap<M_V>}
   * @public
   */
  static m_newJsMap() {
    InternalJsMapFactory.$clinit();
    return InternalJsMap.$create();
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return instance instanceof InternalJsMapFactory; }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, InternalJsMapFactory);
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    InternalJsMapFactory.$clinit();
    if (!InternalJsMapFactory.$classInternalJsMapFactory_) {
      InternalJsMapFactory.$classInternalJsMapFactory_ = Class.$createForClass(
          $Util.$generateId('InternalJsMapFactory'),
          $Util.$generateId('java.util.InternalJsMapFactory'),
          $Util.$generateId('java.util.InternalJsMapFactory'));
    }
    return InternalJsMapFactory.$classInternalJsMapFactory_;
  }

  /**
   * @override
   * @return {Class}
   * @public
   */
  m_getClass() { return InternalJsMapFactory.$getClass(); }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    InternalJsMap = goog.module.get('gen.java.util.InternalJsMap$impl');
    Object.$clinit();
  }};


/**
 * The class literal field.
 * @private {Class}
 */
InternalJsMapFactory.$classInternalJsMapFactory_ = null;



/**
 * Export class.
 */
exports = InternalJsMapFactory;
