goog.module('extern.MyClassWithEmptyNamespace');


var MyClassWithEmptyNamespace = goog.require('MyClassWithEmptyNamespace');


// Suppress bogus 'goog.require'd but not used' lint error.
new MyClassWithEmptyNamespace;


exports = MyClassWithEmptyNamespace;
