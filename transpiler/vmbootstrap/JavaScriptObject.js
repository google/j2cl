/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.JavaScriptObject');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');
let _Class = goog.require('gen.java.lang.Class');


// Re-exports the implementation.
let JavaScriptObject = goog.require('vmbootstrap.JavaScriptObject$impl');
exports = JavaScriptObject;
