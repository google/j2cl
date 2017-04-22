/**
 * @return {*}
 */
NativeJsTypeTest.createNativeJsTypeWithOverlay = function() {
  return { m: function() { return 6; } };
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeObjectWithToString = function() {
  return {toString: function() { return 'Native type'; }};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeObject = function() {
  return {};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeArray = function() {
  return [];
};

/**
 * @return {*}
 */
NativeJsTypeTest.createFunction = function() {
  return function() {};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNumber = function() {
  return 42;
};

/**
 * @return {*}
 */
NativeJsTypeTest.createBoxedNumber = function() {
  return new Number(42);
};

/**
 * @return {*}
 */
NativeJsTypeTest.createBoxedString = function() {
  return new String('hello');
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeSubclass = function() {
  return {
    add: function(e) { this[0] = e; },
    remove: function(e) {
      var ret = this[0] == e;
      this[0] = undefined;
      return ret;
    }
  };
};
