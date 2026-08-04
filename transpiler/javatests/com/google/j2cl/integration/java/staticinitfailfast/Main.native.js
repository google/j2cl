const Foo = goog.require('staticinitfailfast.Main.Foo');

/**
 * @return {void}
 * @suppress {visibility} // Tests calling hidden constructor fails at runtime
 */
Main.createFoo = function() {
  new Foo;
};


/**
 * @return {void}
 */
Main.createFooJsChild = function() {

  class FooChild extends Foo {}

  class Zoo extends FooChild {}

  new Zoo();
};
