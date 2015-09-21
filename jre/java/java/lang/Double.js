/**
 * Header super source for java.lang.Double.
 */
goog.module('gen.java.lang.Double');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');
let _Class = goog.require('gen.java.lang.Class');
let _Number = goog.require('gen.java.lang.Number');


// Re-exports the implementation.
let Double = goog.require('gen.java.lang.Double$impl');
exports = Double;
