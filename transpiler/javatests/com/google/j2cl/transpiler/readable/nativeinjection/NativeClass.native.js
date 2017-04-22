/**
 * Replaces the native instance method 'nativeInstanceMethod' in NativeClass.
 * @return {string}
 * @public
 */
NativeClass.prototype.m_nativeInstanceMethod__ = function() {
  return 'nativeInstanceMethod';
};

/**
 * Replaces the native static method 'nativeStaticMethod' in NativeClass.
 * @return {NativeClass}
 * @public
 */
NativeClass.m_nativeStaticMethod__ = function() {
  return null;
};
