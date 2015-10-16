/**
 * Header super source for java.lang.String.
 */
goog.module('gen.java.lang.String');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');
let _$Util = goog.require('nativebootstrap.Util');
let _Object = goog.require('gen.java.lang.Object');
let _$Objects = goog.require('vmbootstrap.Objects');


// Re-exports the implementation.
let String = goog.require('gen.java.lang.String$impl');
exports = String;
