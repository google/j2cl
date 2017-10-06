/**
 * @param {*} e
 * @return {void}
 */
com_google_gwt_emultest_java_lang_ThrowableTestBase
    .m_throwNative__java_lang_Object = function(e) {
  com_google_gwt_emultest_java_lang_ThrowableTestBase.$clinit();
  throw e;
};

/**
 * @param {function()} thrower
 * @return {*}
 */
com_google_gwt_emultest_java_lang_ThrowableTestBase
    .m_catchNative__com_google_gwt_emultest_java_lang_ThrowableTestBase_Thrower =
    function(thrower) {
  com_google_gwt_emultest_java_lang_ThrowableTestBase.$clinit();
  try {
    thrower();
  } catch (e) {
    return e;
  }
};
