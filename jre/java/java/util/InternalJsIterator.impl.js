/**
 * Impl transpiled from java.util.InternalJsIterator.
 */
goog.module('gen.java.util.InternalJsIterator$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let InternalJsIteratorEntry = goog.forwardDeclare('gen.java.util.InternalJsIteratorEntry$impl');


/**
 * @template C_V
 */
class InternalJsIterator extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
    this._entries = null;
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__java_util_InternalJsIterator() {
  }

  /**
   * A particular Java constructor as a factory method.
   * @template C_V
   * @return {!InternalJsIterator<C_V>}
   * @public
   */
  static $create(entries) {
    InternalJsIterator.$clinit();
    let instance = new InternalJsIterator;
    instance._entries = entries;
    instance.$ctor__java_util_InternalJsIterator();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @public
   */
  $ctor__java_util_InternalJsIterator() {
    this.$ctor__java_lang_Object();
    this.$init__java_util_InternalJsIterator();
  }

  /**
   * @return {InternalJsIteratorEntry<C_V>}
   * @public
   */
  m_next() {
    return InternalJsIteratorEntry.$create(this._entries.next());
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) { return instance instanceof InternalJsIterator; }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, InternalJsIterator);
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    InternalJsIterator.$clinit();
    if (!InternalJsIterator.$classInternalJsIterator_) {
      InternalJsIterator.$classInternalJsIterator_ = Class.$createForClass(
          $Util.$generateId('InternalJsIterator'),
          $Util.$generateId('java.util.InternalJsIterator'),
          Object.$getClass(),
          $Util.$generateId('java.util.InternalJsIterator'));
    }
    return InternalJsIterator.$classInternalJsIterator_;
  }

  /**
   * @override
   * @return {Class}
   * @public
   */
  m_getClass() { return InternalJsIterator.$getClass(); }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    InternalJsIteratorEntry = goog.module.get('gen.java.util.InternalJsIteratorEntry$impl');
    Object.$clinit();
  }};


/**
 * The class literal field.
 * @private {Class}
 */
InternalJsIterator.$classInternalJsIterator_ = null;



/**
 * Export class.
 */
exports = InternalJsIterator;
