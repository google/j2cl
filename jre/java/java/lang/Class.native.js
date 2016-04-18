let Reflect = goog.require('goog.reflect');

/**
 * @param {*} classConstructor
 * @param {number=} opt_dimensionCount
 * @return {Class}
 * @public
 */
Class.$get = function(classConstructor, opt_dimensionCount) {
  let dimensionCount = opt_dimensionCount || 0;
  return Reflect.cache(
      classConstructor.prototype, '$$class/' + dimensionCount,
      function() { return new Class(classConstructor, dimensionCount); });
};

/**
 * @param {*} classConstructor
 * @return {*}
 * @private
 */
Class.getSuperCtor = function(classConstructor) {
  var parentCtor =
      window.Object.getPrototypeOf(classConstructor.prototype).constructor;
  return parentCtor == window.Object ? null : parentCtor;
};
