/**
 * @public
 */
Throwable.prototype.m_captureStackTrace_$p_java_lang_Throwable = function() {
  // Only supporting modern browsers so generating stack by traversing callees
  // is not necessary.
};

/**
 * @return {Array<StackTraceElement>}
 * @public
 */
Throwable.prototype.m_constructJavaStackTrace_$p_java_lang_Throwable =
    function() {
  var stackTraceElements = $Arrays.$create([0], StackTraceElement);
  var e = this.backingJsObject;
  var splitStack = (e && e.stack) ? e.stack.split(/\n/) : [];
  for (var i = 0; i < splitStack.length; i++) {
    var createSte = StackTraceElement.
        $create__java_lang_String__java_lang_String__java_lang_String__int;
    stackTraceElements[i] = createSte('', splitStack[i], null, -1);
  }
  return stackTraceElements;
};

/**
 * @param {?string} msg
 * @return {Error}
 * @public
 */
Throwable.prototype.m_createError__java_lang_String_$pp_java_lang =
    function(msg) {
  return new Error(msg);
};

/**
 * @param {Error} e
 * @return {Error}
 * @public
 */
Throwable.m_fixIE__java_lang_Object_$p_java_lang_Throwable = function(e) {
  if (!('stack' in e)) {
    try { throw e; } catch (ignored) {}
  }
  return e;
};
