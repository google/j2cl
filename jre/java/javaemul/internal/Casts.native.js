/**
 * @param {*} instance
 * @param {*} castType
 * @return {*}
 */
Casts.$to = function(instance, castType) {
  return Casts.$toInternal(
      instance, /** @type {function(*):boolean} */ (castType.$isInstance), castType);
};
