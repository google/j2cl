/**
 * // TODO(b/79389970): change castType to Function.
 * @param {*} instance
 * @param {Object} castType
 * @return {*}
 */
$Casts.$to = function(instance, castType) {
  return $Casts.$toInternal(
      instance, /** @type {function(*):boolean} */ (castType.$isInstance),
      /** @type {Constructor} */ (castType));
};
