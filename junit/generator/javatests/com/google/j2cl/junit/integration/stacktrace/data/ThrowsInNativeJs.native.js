/**
 * @public
 */
ThrowsInNativeJs.throwsInNative = function() {
  throw new Error('java.lang.RuntimeException: __the_message__!');
};