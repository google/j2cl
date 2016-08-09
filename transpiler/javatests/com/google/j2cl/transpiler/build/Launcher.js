goog.module("build.Launcher");

const Foo = goog.require("build.Foo");
const Bar = goog.require("build.Bar");

console.log(Foo.$create__());
console.log(Bar.$create__());

console.log(Foo.createBar());
console.log(Bar.createFoo());
