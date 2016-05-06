/**
 * Header hand rolled.
 */
goog.module('nativebootstrap.Long');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Long = goog.require('goog.math.Long');


// Re-exports the implementation.
let Long = goog.require('nativebootstrap.Long$impl');
exports = Long;
