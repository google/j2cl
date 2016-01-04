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
NativeJsTypeTest.createNativeObjectWithoutToString = function() {
  return {};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeArray = function() {
  return [];
};
