/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicMethod = function(obj) {
  return obj.publicMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicSubclassMethod = function(obj) {
  return obj.publicSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicStaticSubclassMethod = function(obj) {
  return obj.publicStaticSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPrivateSubclassMethod = function(obj) {
  return obj.privateSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasProtectedSubclassMethod = function(obj) {
  return obj.protectedSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPackageSubclassMethod = function(obj) {
  return obj.packageSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicSubclassField = function(obj) {
  return obj.publicSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPublicStaticSubclassField = function(obj) {
  return obj.publicStaticSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPrivateSubclassField = function(obj) {
  return obj.privateSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasProtectedSubclassField = function(obj) {
  return obj.protectedSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
__class.hasPackageSubclassField = function(obj) {
  return obj.packageSubclassField != null;
};
