/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicMethod = function(obj) {
  return obj.publicMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicStaticMethod = function(obj) {
  return obj.publicStaticMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPrivateMethod = function(obj) {
  return obj.privateMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasProtectedMethod = function(obj) {
  return obj.protectedMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPackageMethod = function(obj) {
  return obj.packageMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicField = function(obj) {
  return obj.publicField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicStaticField = function(obj) {
  return obj.publicStaticField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPrivateField = function(obj) {
  return obj.privateField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasProtectedField = function(obj) {
  return obj.protectedField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPackageField = function(obj) {
  return obj.packageField != undefined;
};
