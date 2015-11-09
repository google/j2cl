/**
 * Replaces the native instance method 'nativeInstanceMethod' in NativeClass.
 * @return {string}
 * @public
 */
NativeClass.prototype.m_nativeInstanceMethod = function() {
  return 'nativeInstanceMethod';
};

/**
 * Replaces the native static method 'nativeStaticMethod' in NativeClass.
 * @return {string}
 * @public
 */
NativeClass.m_nativeStaticMethod = function() {
  return 'nativeStaticMethod';
};
