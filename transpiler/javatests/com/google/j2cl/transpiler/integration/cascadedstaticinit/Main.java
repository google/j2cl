package com.google.j2cl.transpiler.integration.cascadedstaticinit;

/**
 * Test cascaded static initializers run only once.
 */
public class Main {
  public static void main(String... args) {
    // Bar's initializer defines it to be 5;
    assert getBar() == 5;
    // Foo's initializer defines it to be bar * 5;
    assert getFoo() == 25;

    // If you change Bar then Foo won't update because it's initializer only runs once.
    setBar(10);
    assert getBar() == 10;
    assert getFoo() == 25;
    assert getFoo() == 25;
    assert getFoo() == 25;
  }

  public static int getBar() {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
    // for the static ValueHolder.bar property will attempt to run it's own $clinit.
    return ValueHolder.bar;
  }

  public static int getFoo() {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
    // for the static ValueHolder.foo property will attempt to run it's own $clinit.
    return ValueHolder.foo;
  }

  public static void setBar(int value) {
    // Since this is a static function in Main it will attempt to run Main's $clinit and the setter
    // for the static ValueHolder.bar property will attempt to run it's own $clinit.
    ValueHolder.bar = value;
  }
}
