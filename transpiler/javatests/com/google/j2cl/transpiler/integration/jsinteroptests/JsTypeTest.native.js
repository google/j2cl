/**
 * @return {*}
 * @public
 */
__class.createNativeButton = function() {
  return document.createElement('button');
};

/**
 * @return {*}
 * @public
 */
__class.createObject = function() {
  return {};
};

/**
 * @param {*} object
 * @return {number}
 */
__class.callPublicMethod = function(object) {
  return object.publicMethod();
};

/**
 * @param {*} value
 * @return {boolean}
 */
__class.isUndefined = function(value) {
  return value == undefined;
};

/**
 * @param {*} obj
 * @param {*} value
 */
__class.setTheField = function(obj, value) {
  obj.notTypeTightenedField = value;
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
__class.callFoo = function(obj, param) {
  return obj.foo(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
__class.callBar = function(obj, param) {
  return obj.bar(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
__class.callM = function(obj, param) {
  return obj.m(param);
};

/**
 * @param {*} jstype
 */
__class.fillJsTypeField = function(jstype) {
  jstype.someField = {};
};

/**
 * @return {*}
 */
__class.nativeObjectImplementingM = function() {
  return {m: function() { return 3; }};
};

/**
 * @return {*}
 */
__class.nativeJsFunction = function() {
  return function() { return 3; };
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasFieldRun = function(obj) {
  return obj.run != undefined;
};

/**
 * @param {*} enumeration
 * @return {number}
 */
__class.callPublicMethodFromEnumeration = function(enumeration) {
  return enumeration.idxAddOne();
};

/**
 * @param {*} enumeration
 * @return {number}
 */
__class.callPublicMethodFromEnumerationSubclass = function(enumeration) {
  return enumeration.foo();
};
