let JsException = goog.require("java.lang.JsException");

/**
 * @param {*} wrapped
 * @return {Exception}
 * @public
 */
JsExceptionViolator.m_createJsException__java_lang_Object = function(wrapped) {
  JsExceptionViolator.$clinit();
  return JsException.$create__java_lang_Object(wrapped);
};

/**
 * @param {Throwable} throwable
 * @return {*}
 * @public
 */
JsExceptionViolator.m_getBackingJsObject__java_lang_Throwable = function(throwable) {
  JsExceptionViolator.$clinit();
  return throwable.backingJsObject;
};
