/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.primitives.$int');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');


// Re-exports the implementation.
let $int = goog.require('vmbootstrap.primitives.$int$impl');
exports = $int;
