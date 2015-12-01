/**
 * @return {number}
 */
Main.getFooA = function() {
  Main.$clinit();
  return Foo.a;
};

/**
 * @return {number}
 */
Main.getFooB = function() {
  Main.$clinit();
  return Foo.abc;
};

/**
 * @param {*} bar
 * @return {number}
 */
Main.getBarA = function(bar) {
  Main.$clinit();
  return bar.a;
};

/**
 * @param {*} bar
 * @return {number}
 */
Main.getBarB = function(bar) {
  Main.$clinit();
  return bar.abc;
};

/**
 * @param {number} x
 */
Main.setFooA = function(x) {
  Main.$clinit();
  Foo.a = x;
};

/**
 * @param {number} x
 */
Main.setFooB = function(x) {
  Main.$clinit();
  Foo.abc = x;
};

/**
 * @param {*} bar
 * @param {number} x
 */
Main.setBarA = function(bar, x) {
  Main.$clinit();
  bar.a = x;
};

/**
 * @param {*} bar
 * @param {number} x
 */
Main.setBarB = function(bar, x) {
  Main.$clinit();
  bar.abc = x;
};
