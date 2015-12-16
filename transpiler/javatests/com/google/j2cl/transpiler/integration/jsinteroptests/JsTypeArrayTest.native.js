/**
 * @return {Array<*>}
 */
JsTypeArrayTest.returnJsTypeFromNative = function() {
  return [{}, {}];
};

/**
 * @return {Array<*>}
 */
JsTypeArrayTest.returnJsTypeWithIdsFromNative = function() {
  return [{id: 1}, {id: 2}];
};

/**
 * @param {*} holder
 */
JsTypeArrayTest.fillArrayField = function(holder) {
  holder.arrayField = [{}, {}];
};

JsTypeArrayTest.fillArrayParam = function(holder) {
  holder.setArrayParam([{}, {}]);
};

/**
 * @return {*}
 */
JsTypeArrayTest.returnJsType3DimFromNative = function() {
  return [[[{id: 1}, {id: 2}, {}], []]];
};

/**
 * @param {number} i
 * @return {*}
 */
JsTypeArrayTest.getSimpleJsType = function(i) {
  return {id: i};
};

/**
 * @return {*}
 */
JsTypeArrayTest.returnObjectArrayFromNative = function() {
  return ['1', '2', '3'];
};
