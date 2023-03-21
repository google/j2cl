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
    // copybara:begin_strip(not available in oss)
    // This error occurs when `equals` is called on an Apps JSPB proto message
    // but the monkey-patch which provides `equals` was not loaded. This
    // typically means that you were passing a `jspb.Message` into J2CL as
    // an `Object` and should instead use an ImmutableJS message.
    //
    // TODO(b/264934765): update this once j2cl proto types are heterogeneous.
    // copybara:end_strip
    throw new Error('equals and hashcode expected but not defined.');
  }
};
