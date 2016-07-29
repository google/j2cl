/**
 * Replaces the native instance method 'nativeInstanceMethod' in NativeClass.
 * @return {string}
 * @public
 */
__class.prototype.m_nativeInstanceMethod__ = function() {
  return 'nativeInstanceMethod';
};

/**
 * Replaces the native static method 'nativeStaticMethod' in NativeClass.
 * @return {__class}
 * @public
 */
__class.m_nativeStaticMethod__ = function() {
  return null;
};
