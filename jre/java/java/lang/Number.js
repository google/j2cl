/**
 * Header super source for java.lang.Number.
 */
goog.module('gen.java.lang.Number');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');
let _Object = goog.require('gen.java.lang.Object');
let _$Long = goog.require('nativebootstrap.Long');
let _$Util = goog.require('nativebootstrap.Util');
let _$long = goog.require('vmbootstrap.primitives.$long');
let _$Primitives = goog.require('vmbootstrap.primitives.Primitives');


// Re-exports the implementation.
let Number = goog.require('gen.java.lang.Number$impl');
exports = Number;
