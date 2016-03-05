/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.JavaScriptObject$impl');


let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Provides class literal and Object methods implementations for native
 * JS objects.
 */
class JavaScriptObject {
  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__Object(obj) { return JavaScriptObject.$getClass(); }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    JavaScriptObject.$clinit();
    if (!JavaScriptObject.$classJavaScriptObject_) {
      JavaScriptObject.$classJavaScriptObject_ = Class.$createForClass(
          $Util.$generateId('JavaScriptObject'),
          $Util.$generateId('JavaScriptObject'),
          $Util.$generateId('JavaScriptObject'));
    }
    return JavaScriptObject.$classJavaScriptObject_;
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() { Class = goog.module.get('gen.java.lang.Class$impl'); }
}


/**
 * The class literal field.
 * @private {Class}
 */
JavaScriptObject.$classJavaScriptObject_ = null;


/**
 * Exported class.
 */
exports = JavaScriptObject;
