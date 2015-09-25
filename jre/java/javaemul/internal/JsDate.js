/**
 * Header for super sourced javaemul.internal.JsDate.
 */
goog.module('gen.javaemul.internal.JsDate');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Object = goog.require('gen.java.lang.Object');
let _$Util = goog.require('nativebootstrap.Util');
let _Class = goog.require('gen.java.lang.Class');
let _String = goog.require('gen.java.lang.String');
let _$double = goog.require('vmbootstrap.primitives.$double');
let _$int = goog.require('vmbootstrap.primitives.$int');


// Re-exports the implementation.
var JsDate = goog.require('gen.javaemul.internal.JsDate$impl');
exports = JsDate;
