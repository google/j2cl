/** @return {!Class} */
$Objects.throwTypeError = function() {
  throw new TypeError('null.getClass()');
};

/**
 * Fails in goog.DEBUG if we saw `equalsAndHashCodeShouldBeAvailable` but did
 * not find either `equals` or `hashCode`.
 * @param {*} instance
 */
$Objects.assertEqualsAndHashCodePresentIfExpected = function(instance) {
  if (goog.DEBUG && instance.equalsAndHashCodeShouldBeAvailable) {
    throw new Error('equals and hashcode expected but not defined.');
  }
};
