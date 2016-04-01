/**
 * Impl super source for java.lang.Class.
 */
goog.module('gen.java.lang.Class$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');
let Reflect = goog.require('goog.reflect');


/**
 * A model object that describes a Class or Interface.
 * <p>
 * Commonly referred to as a class literal.
 * <p>
 * Answers questions like "is this an enum?", "what is the name of this class?",
 * "is it an array class?", etc.
 */
class Class extends Object {
  /**
   * Defines instance fields.
   *
   * @param {*} ctor
   * @param {number} dimensionCount
   */
  constructor(ctor, dimensionCount) {
    super();

    /**
     * @private {*}
     */
    this.ctor_ = ctor;

    /**
     * @private {number}
     */
    this.dimensionCount_ = dimensionCount;
  }

  /**
   * @return {boolean}
   * @public
   */
  m_desiredAssertionStatus() { return false; }

  /**
   * @return {?string}
   * @public
   */
  m_getCanonicalName() {
    let simpleNameSuffix = '';
    for (let i = 0; i < this.dimensionCount_; i++) {
      simpleNameSuffix = simpleNameSuffix + '[]';
    }
    return $Util.$extractClassName(this.ctor_) + simpleNameSuffix;
  }

  /**
   * If the current class literal is for an array then the class literal of its
   * element type is returned otherwise null.
   *
   * @return {Class}
   * @public
   */
  m_getComponentType() {
    if (this.m_isArray()) {
      return Class.$get(this.ctor_, this.dimensionCount_ - 1);
    }
    return null;
  }

  /**
   * @return {Array<Object>}
   * @public
   */
  m_getEnumConstants() { return null; }

  /**
   * @return {?string}
   * @public
   */
  m_getName() {
    let className = $Util.$extractClassName(this.ctor_);
    if (this.m_isArray()) {
      let namePrefix = '';
      for (let i = 0; i < this.dimensionCount_; i++) {
        namePrefix = namePrefix + '[';
      }
      let isPrimitive = this.m_isPrimitive();
      let typePrefix = isPrimitive ? '' : 'L';
      let typeSuffix = isPrimitive ? '' : ';';
      return namePrefix + typePrefix + className + typeSuffix;
    }
    return className;
  }

  /**
   * @return {?string}
   * @public
   */
  m_getSimpleName() {
    let className = $Util.$extractClassName(this.ctor_);
    let simpleClassName = className.substring(className.lastIndexOf('.') + 1);
    let simpleNameSuffix = '';
    for (let i = 0; i < this.dimensionCount_; i++) {
      simpleNameSuffix = simpleNameSuffix + '[]';
    }
    return simpleClassName + simpleNameSuffix;
  }

  /**
   * @return {boolean}
   * @public
   */
  m_isArray() { return this.dimensionCount_ != 0; }

  /**
   * @param {$Util.ClassType} type
   * @return {boolean}
   * @private
   */
  $isOfType_(type) {
    return !this.m_isArray() && $Util.$extractClassType(this.ctor_) == type;
  }

  /**
   * @return {boolean}
   * @public
   */
  m_isEnum() { return this.$isOfType_($Util.ClassType.ENUM); }

  /**
   * @return {boolean}
   * @public
   */
  m_isInterface() { return this.$isOfType_($Util.ClassType.INTERFACE); }

  /**
   * @return {boolean}
   * @public
   */
  m_isPrimitive() { return this.$isOfType_($Util.ClassType.PRIMITIVE); }

  /**
   * @return {string}
   * @public
   */
  $javaToString() {
    return (this.m_isInterface() ? 'interface ' :
            (this.m_isPrimitive() ? '' : 'class ')) +
        this.m_getName();
  }

  /**
   * Returns the Class instance that corresponds to provided constructor and
   * optional dimension count in the case of an array.
   *
   * @param {*} classConstructor
   * @param {number=} opt_dimensionCount
   * @return {Class}
   * @public
   */
  static $get(classConstructor, opt_dimensionCount) {
    let dimensionCount = opt_dimensionCount || 0;
    return Reflect.cache(
        classConstructor.prototype, '$$class/' + dimensionCount,
        function() { return new Class(classConstructor, dimensionCount); });
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @param {Object} instance
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) {
    return instance instanceof Class;
  }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, Class);
  }

  /**
   * Runs inline static field initializers.
   *
   * @protected
   */
  static $clinit() { Object.$clinit(); }
};


$Util.$setClassMetadata(Class, 'java.lang.Class');


/**
 * Exported class.
 */
exports = Class;
