/**
 * @param {*} o
 * @return {number}
 * @public
 */
JsExportTest.callFoo = function(o) {
  return o.foo();
};

/**
 * @param {*} o
 * @return {number}
 * @public
 */
JsExportTest.accessField = function(o) {
  return o.field;
};
