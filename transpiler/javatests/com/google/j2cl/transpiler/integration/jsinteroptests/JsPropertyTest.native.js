/**
 * @return {*}
 * @public
 */
__class.createMyNativeJsType = function() {
  return new MyNativeJsType(0);
};

/**
 * @return {*}
 * @public
 */
__class.createJsTypeGetProperty = function() {
  var a = {};
  a['x'] = undefined;
  return a;
};

/**
 * @return {*}
 * @public
 */
__class.createJsTypeIsProperty = function() {
  var a = {};
  a['x'] = false;
  return a;
};

/**
 * @return {*}
 * @public
 */
__class.createMyJsInterfaceWithProtectedNames = function() {
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
__class.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {*} object
 * @param {*} fieldName
 * @return {boolean}
 * @public
 */
__class.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};

/**
 * @param {*} object
 * @param {*} name
 * @return {number}
 * @public
 */
__class.getProperty = function(object, name) {
  return object[name];
};

/**
 * @param {*} object
 * @param {*} name
 * @param {*} value
 * @public
 */
__class.setProperty = function(object, name, value) {
  object[name] = value;
};
