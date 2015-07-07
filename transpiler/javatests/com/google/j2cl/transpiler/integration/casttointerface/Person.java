package com.google.j2cl.transpiler.integration.casttointerface;

public class Person implements HasName {
  private String name;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }
}
