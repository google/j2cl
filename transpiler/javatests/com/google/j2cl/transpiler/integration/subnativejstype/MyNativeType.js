goog.module('test.foo.MyNativeType');

class MyNativeType {
  constructor(x, y) {
    this.executed = true;
    this.x = x;
    this.y = y;
  }

  /**
   * @param {number} a
   * @return {number}
   */
  foo(a) { return this.x + this.y + a; }
}

exports = MyNativeType;
