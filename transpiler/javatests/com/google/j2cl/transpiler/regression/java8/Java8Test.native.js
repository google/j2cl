/**
 * @return {*}
 * @public
 */
Java8Test
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
Java8Test.callFromJSNI = function(f) {
  return f(2, 'a', 'b', 'c');
};


/**
 * @return {?function():?number}
 * @public
 */
Java8Test.createNative = function() {
  return function() {
    return 5;
  };
};
