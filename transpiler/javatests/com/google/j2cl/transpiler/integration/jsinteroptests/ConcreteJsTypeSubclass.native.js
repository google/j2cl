/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicMethod = function(obj) {
  return obj.publicMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicSubclassMethod = function(obj) {
  return obj.publicSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicStaticSubclassMethod = function(obj) {
  return obj.publicStaticSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPrivateSubclassMethod = function(obj) {
  return obj.privateSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasProtectedSubclassMethod = function(obj) {
  return obj.protectedSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPackageSubclassMethod = function(obj) {
  return obj.packageSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicSubclassField = function(obj) {
  return obj.publicSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicStaticSubclassField = function(obj) {
  return obj.publicStaticSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPrivateSubclassField = function(obj) {
  return obj.privateSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasProtectedSubclassField = function(obj) {
  return obj.protectedSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPackageSubclassField = function(obj) {
  return obj.packageSubclassField != null;
};
