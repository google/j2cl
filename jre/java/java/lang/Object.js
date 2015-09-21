/**
 * Header super source for java.lang.Object.
 */
goog.module('gen.java.lang.Object');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');
let _$Hashing = goog.require('nativebootstrap.Hashing');


// Re-exports the implementation.
let Object = goog.require('gen.java.lang.Object$impl');
exports = Object;
