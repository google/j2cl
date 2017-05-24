package c;

import b.B;
import dagger.Component;

@Component
public interface C {

  B b();
}
