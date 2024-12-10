/**
 * @param {*} instance
 * @param {!Constructor} castType
 * @return {*}
 */
$Casts.$to = function(instance, castType) {
  return $Casts.$toInternal(
      instance, /** @type {function(*):boolean} */
      (/** @type {?} */ (castType).$isInstance), castType);
};
