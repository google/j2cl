package b;

import a.A;
import javax.inject.Inject;

public class B {
  private final A a;

  @Inject
  public B(final A a) {
    this.a = a;
  }
}
