/**
 * Header hand rolled.
 */
goog.module('nativebootstrap.LongUtils');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Long = goog.require('nativebootstrap.Long$impl');


// Re-exports the implementation.
let LongUtils = goog.require('nativebootstrap.LongUtils$impl');
exports = LongUtils;
