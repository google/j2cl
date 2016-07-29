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
__class.createEmptyNativeObject = function() {
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
__class.createNativeObjectWithEquals = function(x) {
  return {field: x, equals: function(other) { return other.field == 10; }};
};

/**
 * @return {*}
 */
__class.createNativeObjectWithHashCode = function() {
  return {hashCode: function() { return 100; }};
};
