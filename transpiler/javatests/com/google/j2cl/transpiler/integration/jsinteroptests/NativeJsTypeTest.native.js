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
NativeJsTypeTest.createEmptyNativeObject = function() {
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
NativeJsTypeTest.createNativeObjectWithEquals = function(x) {
  return {field: x, equals: function(other) { return other.field == 10; }};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeObjectWithHashCode = function() {
  return {hashCode: function() { return 100; }};
};
