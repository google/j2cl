/**
 * @return {*}
 * @public
 */
JsTypeTest.createNativeButton = function() {
  return document.createElement('button');
};

/**
 * @return {*}
 * @public
 */
JsTypeTest.createObject = function() {
  return {};
};

/**
 * @param {*} object
 * @return {number}
 */
JsTypeTest.callPublicMethod = function(object) {
  return object.publicMethod();
};

/**
 * @param {*} value
 * @return {boolean}
 */
JsTypeTest.isUndefined = function(value) {
  return value == undefined;
};

/**
 * @param {*} obj
 * @param {*} value
 */
JsTypeTest.setTheField = function(obj, value) {
  obj.notTypeTightenedField = value;
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callFoo = function(obj, param) {
  return obj.foo(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callBar = function(obj, param) {
  return obj.bar(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callM = function(obj, param) {
  return obj.m(param);
};

/**
 * @param {*} jstype
 */
JsTypeTest.fillJsTypeField = function(jstype) {
  jstype.someField = {};
};

/**
 * @return {*}
 */
JsTypeTest.nativeObjectImplementingM = function() {
  return {m: function() { return 3; }};
};

/**
 * @return {*}
 */
JsTypeTest.nativeJsFunction = function() {
  return function() { return 3; };
};

/**
 * @param {*} obj
 * @return {boolean}
 */
JsTypeTest.hasFieldRun = function(obj) {
  return obj.run != undefined;
};
