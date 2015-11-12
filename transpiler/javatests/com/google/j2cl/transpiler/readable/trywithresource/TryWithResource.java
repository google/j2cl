package com.google.j2cl.transpiler.readable.trywithresource;

public class TryWithResource {
  static class ClosableThing implements AutoCloseable {
    @Override
    public void close() {}
  }

  public static void tryWithResource() {
    try (ClosableThing thing = new ClosableThing()) {
      int i = 0;
    }
  }

  public static void tryWithResourceMultipleResources(String[] args) {
    try (ClosableThing thing = new ClosableThing(); ClosableThing thing2 = new ClosableThing()) {
      int i = 0;
      throw new Exception();
    } catch (Exception e) {
      int b = 10;
    }
  }
}
