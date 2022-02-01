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
 * @param {!Constructor} target Target.
 * @param {!Constructor} source Source.
 * @param {number} flags
 * @param {...string} excluded_fields
 */
ValueType.mixin = function(target, source, flags, ...excluded_fields) {
  if (flags & 1) target.prototype.equals = source.prototype.equals;
  if (flags & 2) target.prototype.hashCode = source.prototype.hashCode;
  if (flags & 4) target.prototype.toString = source.prototype.toString;
  if (excluded_fields) {
    target.prototype.$excluded_fields = excluded_fields;
  }
};
