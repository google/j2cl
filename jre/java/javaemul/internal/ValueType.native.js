/** @const {!Array<string>|undefined} */
ValueType.prototype.$excluded_fields;

/**
 * @param {!ValueType} type
 * @return {!Array<string>}
 */
ValueType.filteredkeys = function(type) {
  const keys = Object.keys(type);
  const excludedFields = type.$excluded_fields;
  return excludedFields ? keys.filter(x => !excludedFields.includes(x)) : keys;
};

/**
 * @param {function(new:?,...):undefined} ctor
 * @param {number} flags
 * @param {...string} excluded_fields
 */
ValueType.mixin = function(ctor, flags, ...excluded_fields) {
  if (flags & 1) ctor.prototype.equals = ValueType.prototype.equals;
  if (flags & 2) ctor.prototype.hashCode = ValueType.prototype.hashCode;
  if (flags & 4) ctor.prototype.toString = ValueType.prototype.toString;
  if (excluded_fields) {
    ctor.prototype.$excluded_fields = excluded_fields;
  }
};
