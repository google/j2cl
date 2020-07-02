/**
 * @param {function(new:?,...):undefined} ctor
 * @param {number} flags
 */
ValueType.mixin = function(ctor, flags) {
  if (flags & 1) ctor.prototype.equals = ValueType.prototype.equals;
  if (flags & 2) ctor.prototype.hashCode = ValueType.prototype.hashCode;
  if (flags & 4) ctor.prototype.toString = ValueType.prototype.toString;
};
