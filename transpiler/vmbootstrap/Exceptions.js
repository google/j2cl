/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Exceptions');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Throwable = goog.require('gen.java.lang.Throwable');
let _AutoClosable = goog.require('gen.java.lang.AutoCloseable');

// Re-exports the implementation.
let Exceptions = goog.require('vmbootstrap.Exceptions$impl');
exports = Exceptions;
