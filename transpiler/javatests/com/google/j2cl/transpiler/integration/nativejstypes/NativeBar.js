goog.module('com.google.j2cl.transpiler.integration.nativejstypes.Bar');

class Bar {
  constructor(a, b) {
    this.x = a;
    this.y = b;
  }

  product() { return this.x * this.y; }
};

exports = Bar;
