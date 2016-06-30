goog.module("build.Launcher");

const Foo = goog.require("build.Foo");
const Bar = goog.require("build.Bar");

console.log(Foo.$create());
console.log(Bar.$create());

console.log(Foo.createBar());
console.log(Bar.createFoo());
