/**
 * @return {*}
 * @public
 */
JsPropertyTest.createMyNativeJsType = function() {
  return new $JsPropertyTest$MyNativeJsType(0);
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createJsTypeGetProperty = function() {
  var a = {};
  a['x'] = undefined;
  return a;
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createJsTypeIsProperty = function() {
  var a = {};
  a['x'] = false;
  return a;
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createMyJsInterfaceWithProtectedNames = function() {
  var a = {};
  a['nullField'] = 'nullField';
  a['import'] = 'import';
  a['var'] = function() { return 'var'; };
  return a;
};

/**
 * @param {*} value
 * @return {boolean}
 * @public
 */
JsPropertyTest.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {*} object
 * @param {*} fieldName
 * @return {boolean}
 * @public
 */
JsPropertyTest.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};

/**
 * @param {*} object
 * @param {*} name
 * @return {number}
 * @public
 */
JsPropertyTest.getProperty = function(object, name) {
  return object[name];
};

/**
 * @param {*} object
 * @param {*} name
 * @param {*} value
 * @public
 */
JsPropertyTest.setProperty = function(object, name, value) {
  object[name] = value;
};
