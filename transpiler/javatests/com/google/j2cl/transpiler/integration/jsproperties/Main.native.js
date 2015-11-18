/**
 * @return {number}
 * @nocollapse
 * */
Main.getFooA = function() {
  Main.$clinit();
  return Foo.a;
};

/**
 * @return {number}
 * @nocollapse
 * */
Main.getFooB = function() {
  Main.$clinit();
  return Foo.abc;
};

/**
 * @param {*} bar
 * @return {number}
 * @nocollapse
 * */
Main.getBarA = function(bar) {
  Main.$clinit();
  return bar.a;
};

/**
 * @param {*} bar
 * @return {number}
 * @nocollapse
 * */
Main.getBarB = function(bar) {
  Main.$clinit();
  return bar.abc;
};

/**
 * @param {number} x
 * @nocollapse
 * */
Main.setFooA = function(x) {
  Main.$clinit();
  Foo.a = x;
};

/**
 * @param {number} x
 * @nocollapse
 * */
Main.setFooB = function(x) {
  Main.$clinit();
  Foo.abc = x;
};

/**
 * @param {*} bar
 * @param {number} x
 * @nocollapse
 * */
Main.setBarA = function(bar, x) {
  Main.$clinit();
  bar.a = x;
};

/**
 * @param {*} bar
 * @param {number} x
 * @nocollapse
 * */
Main.setBarB = function(bar, x) {
  Main.$clinit();
  bar.abc = x;
};
