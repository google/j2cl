/**
 * @return {*}
 */
__class.createNativeJsTypeWithOverlay = function() {
  return { m: function() { return 6; } };
};

/**
 * @return {*}
 */
__class.createNativeObjectWithToString = function() {
  return {toString: function() { return 'Native type'; }};
};

/**
 * @return {*}
 */
__class.createNativeObject = function() {
  return {};
};

/**
 * @return {*}
 */
__class.createNativeArray = function() {
  return [];
};

/**
 * @return {*}
 */
__class.createFunction = function() {
  return function() {};
};

/**
 * @return {*}
 */
__class.createNumber = function() {
  return 42;
};

/**
 * @return {*}
 */
__class.createBoxedNumber = function() {
  return new Number(42);
};

/**
 * @return {*}
 */
__class.createBoxedString = function() {
  return new String('hello');
};

/**
 * @return {*}
 */
__class.createNativeSubclass = function() {
  return {
    add: function(e) { this[0] = e; },
    remove: function(e) {
      var ret = this[0] == e;
      this[0] = undefined;
      return ret;
    }
  };
};
