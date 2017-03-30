/**
 * @return {*}
 * @public
 */
__class
    .createNativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod =
    function() {
  return {
    getA: function() {
      return 1;
    }
  };
};


/**
 * @param {?function(number, ...?string):?string} f
 * @return {?string}
 * @public
 */
__class.callFromJSNI = function(f) {
  return f(2, 'a', 'b', 'c');
};


/**
 * @return {?function():?number}
 * @public
 */
__class.createNative = function() {
  return function() {
    return 5;
  };
};