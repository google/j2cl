goog.module('test.foo.MyNativeType');

class MyNativeType {
  constructor() {
    this.publicField = 0;
    this.privateField = 0;
    this.packageField = 0;
    this.protectedField = 0;
  }

  /**
   * @public
   */
  publicMethod() {}

  /**
   * @public
   */
  privateMethod() {}

  /**
   * @public
   */
  packageMethod() {}

  /**
   * @public
   */
  protectedMethod() {}
}

exports = MyNativeType;
