goog.module('gen.java.lang.CoreModule');


var Hashing = goog.require('nativebootstrap.HashingModule').Hashing;
var Util = goog.require('nativebootstrap.UtilModule').Util;


/**
 * Transpiled from java/lang/Object.java.
 */
class Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {}

  /**
   * Runs inline instance field initializers.
   * @private
   */
  $init__java_lang_Object() {}

  /**
   * A particular Java constructor as a factory method.
   * @return {!Object}
   * @public
   * @nocollapse
   */
  static $create() {
    Object.$clinit();
    var instance = new Object;
    instance.$ctor__java_lang_Object();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @protected
   */
  $ctor__java_lang_Object() { this.$init__java_lang_Object(); }

  /**
   * @return {Class}
   * @public
   */

  m_getClass() { return this.constructor.$getClass(); }

  /**
   * @return {number}
   * @public
   */
  m_hashCode() { return Hashing.$getHashCode(this); }

  /**
   * @param {*} other
   * @return {boolean}   q
   * @public
   */
  m_equals__java_lang_Object(other) { return this === other; }

  /**
   * @return {?string}
   * @public
   */
  m_toString() {
    // TODO: fix this implementation. The hash code should be returned in hex
    // but can't currently depend on Integer to get access to that static
    // function because Closure doesn't yet support module circular references.
    return this.constructor.$getClass().m_getName() + '@' + this.m_hashCode();
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @param {*} instance
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    // Accepts all Java and JS Objects, including native arrays.
    // TODO: decide how to handle Strings.
    return typeof(instance) == 'object';
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    // Special case for Array.
    if (classConstructor == Array) {
      return true;
    }
    return Util.$canCastClass(classConstructor, Object);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!Object.$classObject) {
      Object.$classObject = Class.$createForClass(
        Util.$generateId('Object'),
        Util.$generateId('java.lang.Object'),
        null,
        Util.$generateId('java.lang.Object'));
    }
    return Object.$classObject;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {}
};



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
   * @param {?string} simpleName
   * @param {?string} name
   * @param {Class.Type_} type
   * @param {Class} superClassLiteral
   * @param {?string} canonicalName
   * @param {Array<Object>} enumConstants
   * @param {Class} arrayElementClassLiteral
   * @param {number} dimensionCount
   * @private
   */
  constructor(simpleName, name, type, superClassLiteral, canonicalName,
      enumConstants, arrayElementClassLiteral, dimensionCount) {
    super();

    /**
     * @private {?string}
     */
    this.$name_ = name;

    /**
     * @private {?string}
     */
    this.$simpleName_ = simpleName;

    /**
     * @private {Class.Type_}
     */
    this.$type_ = type;

    /**
     * @private {Class}
     */
    this.$superClassLiteral_ = superClassLiteral;

    /**
     * @private {?string}
     */
    this.$canonicalName_ = canonicalName || name;

    /**
     * @private {Array<Object>}
     */
    this.$enumConstants_ = enumConstants;

    /**
     * Only used for Array class literals.
     * @private {Class}
     */
    this.$arrayElementClassLiteral_ = arrayElementClassLiteral;

    /**
     * @private {number}
     */
    this.$dimensionCount_ = dimensionCount;

    /**
     * An array of lazily created class literals that describe Arrays of the
     * same type as the type being described by this class literal. For example
     * if this class literal is for class "Bar" then when array classes Bar[]
     * and Bar[][] are created class literals to describe them will be created
     * and stored here.
     * @private {Array<Class>}
     */
    this.$arrayClassLiteralsByDimensions_ = [];
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
    if (this.m_isArray() && this.$canonicalName_ == null) {
      var canonicalNameSuffix = '';
      for (var i = 0; i < this.$dimensionCount_; i++) {
        canonicalNameSuffix = canonicalNameSuffix + '[]';
      }
      this.$canonicalName_ =
          this.$arrayElementClassLiteral_.m_getCanonicalName() +
          canonicalNameSuffix;
    }
    return this.$canonicalName_;
  }

  /**
   * If the current class literal is for an array then the class literal of its
   * element type is returned otherwise null.
   * @return {Class}
   * @public
   */
  m_getComponentType() {
    if (this.m_isArray()) {
      return this.$dimensionCount_ == 1 ?
          this.$arrayElementClassLiteral_ :
          this.$arrayElementClassLiteral_.$forArray(this.$dimensionCount_ - 1);
    }
    return null;
  }

  /**
   * @return {Array<Object>}
   * @public
   */
  m_getEnumConstants() { return this.$enumConstants_; }

  /**
   * @return {?string}
   * @public
   */
  m_getName() {
    if (this.m_isArray() && this.$name_ == null) {
      var namePrefix = '';
      for (var i = 0; i < this.$dimensionCount_; i++) {
        namePrefix = namePrefix + '[';
      }
      var isPrimitive =
          this.$arrayElementClassLiteral_.$type_ == Class.Type_.PRIMITIVE;
      var typePrefix = isPrimitive ? '' : 'L';
      var typeSuffix = isPrimitive ? '' : ';';
      this.$name_ = namePrefix + typePrefix +
          this.$arrayElementClassLiteral_.m_getName() + typeSuffix;
    }
    return this.$name_;
  }

  /**
   * @return {?string}
   * @public
   */
  m_getSimpleName() {
    if (this.m_isArray() && this.$simpleName_ == null) {
      var simpleNameSuffix = '';
      for (var i = 0; i < this.$dimensionCount_; i++) {
        simpleNameSuffix = simpleNameSuffix + '[]';
      }
      this.$simpleName_ =
          this.$arrayElementClassLiteral_.m_getSimpleName() + simpleNameSuffix;
    }
    return this.$simpleName_;
  }

  /**
   * @return {Class}
   * @public
   */
  m_getSuperclass() { return this.$superClassLiteral_; }

  /**
   * @return {boolean}
   * @public
   */
  m_isArray() { return this.$type_ == Class.Type_.ARRAY; }

  /**
   * @return {boolean}
   * @public
   */
  m_isEnum() { return this.$type_ == Class.Type_.ENUM; }

  /**
   * @return {boolean}
   * @public
   */
  m_isInterface() { return this.$type_ == Class.Type_.INTERFACE; }

  /**
   * @return {boolean}
   * @public
   */
  m_isPrimitive() { return this.$type_ == Class.Type_.PRIMITIVE; }

  /**
   * @return {string}
   * @public
   */
  m_toString() {
    return (this.m_isInterface() ? 'interface ' :
            (this.m_isPrimitive() ? '' : 'class ')) +
        this.m_getName();
  }

  /**
   * @param {number} dimensionCount
   * @return {Class}
   * @public
   */
  $forArray(dimensionCount) {
    var arrayClassLiteral =
        this.$arrayClassLiteralsByDimensions_[dimensionCount];
    if (arrayClassLiteral != null) {
      return arrayClassLiteral;
    }

    arrayClassLiteral = new Class(
        null, null, Class.Type_.ARRAY, Object.$getClass(),
        null, null, this, dimensionCount);
    this.$arrayClassLiteralsByDimensions_[dimensionCount] = arrayClassLiteral;
    return arrayClassLiteral;
  }

  /**
   * Should only ever be called once as part of a Class declaration.
   * @param {string} simpleName
   * @param {string} name
   * @param {Class} superClassLiteral
   * @param {string=} opt_canonicalName
   * @return {Class}
   * @public
   */
  static $createForClass(simpleName, name, superClassLiteral,
      opt_canonicalName) {
    Class.$clinit();
    return new Class(simpleName, name, Class.Type_.PLAIN, superClassLiteral,
        opt_canonicalName || null, null, null, 0);
  }

  /**
   * Should only ever be called once as part of an Enum class declaration.
   * @param {string} simpleName
   * @param {string} name
   * @param {Class} superClassLiteral
   * @param {string} canonicalName
   * @param {Array<Object>} enumConstants
   * @return {Class}
   * @public
   */
  static $createForEnum(simpleName, name, superClassLiteral, canonicalName,
      enumConstants) {
    Class.$clinit();
    return new Class(simpleName, name, Class.Type_.ENUM, superClassLiteral,
        canonicalName, enumConstants, null, 0);
  }

  /**
   * Should only ever be called once as part of an Interface declaration.
   * @param {string} simpleName
   * @param {string} name
   * @param {Class} superClassLiteral
   * @return {Class}
   * @public
   */
  static $createForInterface(simpleName, name, superClassLiteral,
      opt_canonicalName) {
    Class.$clinit();
    return new Class(simpleName, name, Class.Type_.INTERFACE, superClassLiteral,
        opt_canonicalName || null, null, null, 0);
  }

  /**
   * @param {string} simpleName
   * @return {Class}
   * @public
   */
  static $createForPrimitive(simpleName) {
    Class.$clinit();
    return new Class(simpleName, simpleName, Class.Type_.PRIMITIVE, null,
        simpleName, null, null, 0);
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @param {Object} instance
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    return instance instanceof Class;
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return Util.$canCastClass(classConstructor, Class);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!Class.$classClass) {
      Class.$classClass = Class.$createForClass(
          Util.$generateId('Class'),
          Util.$generateId('java.lang.Class'),
          Object.$getClass(),
          Util.$generateId('java.lang.Class'));
    }
    return Class.$classClass;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() { Object.$clinit(); }
};


/**
 * @enum {number}
 * @private
 */
Class.Type_ = {
  PLAIN: 0,
  ARRAY: 1,
  ENUM: 2,
  INTERFACE: 3,
  PRIMITIVE: 4
};


/**
 * @define {boolean} Whether or not to keep getName() and getCanonicalName()
 * accurate.
 * @private
 */
goog.define('CLASS_NAMES_ENABLED_', true);


/**
 * Exported class.
 */
exports.Class = Class;


/**
 * Exported class.
 */
exports.Object = Object;
