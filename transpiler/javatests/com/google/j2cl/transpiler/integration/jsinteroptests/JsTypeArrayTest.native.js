/**
 * @return {Array<*>}
 */
__class.returnJsTypeFromNative = function() {
  return [{}, {}];
};

/**
 * @return {Array<*>}
 */
__class.returnJsTypeWithIdsFromNative = function() {
  return [{id: 1}, {id: 2}];
};

/**
 * @param {*} holder
 */
__class.fillArrayField = function(holder) {
  holder.arrayField = [{}, {}];
};

__class.fillArrayParam = function(holder) {
  holder.setArrayParam([{}, {}]);
};

/**
 * @return {*}
 */
__class.returnJsType3DimFromNative = function() {
  return [[[{id: 1}, {id: 2}, {}], []]];
};

/**
 * @param {number} i
 * @return {*}
 */
__class.getSimpleJsType = function(i) {
  return {id: i};
};

/**
 * @return {*}
 */
__class.returnObjectArrayFromNative = function() {
  return ['1', '2', '3'];
};

/**
 * @return {*}
 */
__class.returnSomeFunction = function() {
  return function(a) { return a + 2; };
};
