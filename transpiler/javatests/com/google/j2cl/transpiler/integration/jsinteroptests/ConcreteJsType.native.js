/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicMethod = function(obj) {
  return obj.publicMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicStaticMethod = function(obj) {
  return obj.publicStaticMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPrivateMethod = function(obj) {
  return obj.privateMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasProtectedMethod = function(obj) {
  return obj.protectedMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPackageMethod = function(obj) {
  return obj.packageMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicField = function(obj) {
  return obj.publicField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicStaticField = function(obj) {
  return obj.publicStaticField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPrivateField = function(obj) {
  return obj.privateField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasProtectedField = function(obj) {
  return obj.protectedField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPackageField = function(obj) {
  return obj.packageField != undefined;
};
