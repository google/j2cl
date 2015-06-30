package com.google.j2cl.transpiler.integration.instanceofinterface;

import java.io.Serializable;

public class Main implements ChildInterface {
  public static void main(String... args) {
    Object object = new Main();
    assert object instanceof ParentInterface;
    assert object instanceof ChildInterface;
    assert !(object instanceof Serializable);
  }
}
