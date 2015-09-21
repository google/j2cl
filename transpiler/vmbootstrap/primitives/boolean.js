/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.primitives.$boolean');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');


// Re-exports the implementation.
let $boolean = goog.require('vmbootstrap.primitives.$boolean$impl');
exports = $boolean;
