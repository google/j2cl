/**
 * @public
 */
Throwable.prototype.m_captureStackTrace___$p_java_lang_Throwable = function() {
  // Only supporting modern browsers so generating stack by traversing callees
  // is not necessary.
};

/**
 * @return {Array<StackTraceElement>}
 * @public
 */
Throwable.prototype.m_constructJavaStackTrace___$p_java_lang_Throwable =
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
 * @param {*} e
 * @return {*}
 * @public
 */
Throwable.m_fixIE__java_lang_Object_$p_java_lang_Throwable = function(e) {
  if (!('stack' in e)) {
    try { throw e; } catch (ignored) {}
  }
  return e;
};
