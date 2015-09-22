package com.google.j2cl.transpiler.integration.variablecollisions;

class Asserts {}

public class Main {
  public Main() {}

  public Main(boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assert Main;
    assert Asserts instanceof Asserts;
    assert $Asserts instanceof Main;
    assert l_Asserts == 1;
  }

  public int testJSKeywords(int in) {
    int let = 100;
    return let + in;
  }

  public void testClassLocalVarCollision() {
    boolean Main = true;
    Asserts a = new Asserts();
    int Asserts = 1;
    int $Asserts = 2;
    int l_Asserts = 3;
    assert Main && !(new Object() instanceof Main);
    assert a instanceof Asserts;
    assert Asserts == 1;
    assert $Asserts == 2;
    assert l_Asserts == 3;
  }

  public void testClassParameterCollision(
      boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assert Main;
    assert Asserts instanceof Asserts;
    assert $Asserts instanceof Main;
    assert l_Asserts == 1;
  }

  public void testClassParameterCollisionInConstructor() {
    Main m = new Main(true, new Asserts(), new Main(), 1);
  }

  public static void main(String... args) {
    Main m = new Main();
    assert m.testJSKeywords(10) == 110;
    m.testClassLocalVarCollision();
    m.testClassParameterCollision(true, new Asserts(), m, 1);
    m.testClassParameterCollisionInConstructor();
  }
}
