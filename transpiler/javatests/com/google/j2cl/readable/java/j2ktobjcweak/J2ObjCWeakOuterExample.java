/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package j2ktobjcweak;

import com.google.j2objc.annotations.WeakOuter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class J2ObjCWeakOuterExample<T extends @Nullable Object> {
  public interface Supplier<T extends @Nullable Object> {
    T get();
  }

  private static final String staticValue = "foo";
  private final T value;

  public J2ObjCWeakOuterExample(T value) {
    this.value = value;
  }

  @WeakOuter
  public final Supplier<T> anonymousClassSupplierField =
      new Supplier<T>() {
        @Override
        public T get() {
          return value;
        }
      };

  public Supplier<T> getLambdaSupplierVariable() {
    @WeakOuter Supplier<T> supplier = () -> value;
    return supplier;
  }

  public Supplier<T> getAnonymousClassSupplierVariable() {
    @WeakOuter
    Supplier<T> supplier =
        new Supplier<T>() {
          @Override
          public T get() {
            return value;
          }
        };
    return supplier;
  }

  public InnerSupplier getInnerSupplier() {
    return new InnerSupplier();
  }

  @WeakOuter
  public class InnerSupplier implements Supplier<T> {
    @Override
    public T get() {
      return value;
    }

    public InnerInnerSupplier getInnerInnerSupplier() {
      return new InnerInnerSupplier();
    }

    public InnerSiblingInnerSupplier getInnerSiblingInnerSupplier() {
      return new InnerSiblingInnerSupplier();
    }

    @WeakOuter
    public class InnerInnerSupplier implements Supplier<T> {
      @Override
      public T get() {
        return value;
      }
    }

    @WeakOuter
    public class InnerSiblingInnerSupplier extends SiblingInnerSupplier {
      @Override
      public T get() {
        return value;
      }
    }
  }

  @WeakOuter
  public class SiblingInnerSupplier implements Supplier<T> {
    @Override
    public T get() {
      return value;
    }
  }

  @WeakOuter
  public static final Supplier<String> staticAnonymousClassSupplierField =
      new Supplier<String>() {
        @Override
        public String get() {
          return staticValue;
        }
      };

  public static Supplier<String> getStaticLambdaSupplierVariable() {
    @WeakOuter Supplier<String> supplier = () -> staticValue;
    return supplier;
  }

  public static Supplier<String> getStaticAnonymousClassSupplierVariable() {
    @WeakOuter
    Supplier<String> supplier =
        new Supplier<String>() {
          @Override
          public String get() {
            return staticValue;
          }
        };
    return supplier;
  }

  public StaticSupplier getStaticSupplier() {
    return new StaticSupplier();
  }

  @WeakOuter
  public static class StaticSupplier implements Supplier<String> {
    @Override
    public String get() {
      return staticValue;
    }
  }
}
