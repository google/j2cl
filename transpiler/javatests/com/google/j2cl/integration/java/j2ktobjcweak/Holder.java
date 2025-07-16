/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package j2ktobjcweak;

import com.google.j2objc.annotations.ObjectiveCName;
import com.google.j2objc.annotations.Weak;
import com.google.j2objc.annotations.WeakOuter;
import java.lang.ref.WeakReference;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Holder for testing WeakReference and @Weak / @WeakOuter annotations. */
@NullMarked
@ObjectiveCName("JavaHolder")
public final class Holder<T extends @Nullable Object> {
  private @Nullable T object;

  public Holder(T object) {
    this.object = object;
  }

  public void set(T object) {
    this.object = object;
  }

  public T get() {
    return object;
  }

  public Supplier<T> newSupplier() {
    return () -> object;
  }

  public Supplier<T> newWeakReferenceSupplier() {
    return new WeakReferenceSupplier<>(this);
  }

  public Supplier<T> newWeakAnnotationSupplier() {
    return new WeakAnnotationSupplier<>(this);
  }

  public Supplier<T> newWeakOuterAnnotationSupplier() {
    return new WeakOuterAnnotationSupplier();
  }

  public Supplier<T> newWeakOuterAnnotationLambdaSupplier() {
    @WeakOuter Supplier<T> supplier = () -> object;
    return supplier;
  }

  public Supplier<T> newWeakOuterAnnotationAnonymousSupplier() {
    // @WeakOuter has no effect on anonymous classes.
    @WeakOuter
    Supplier<T> supplier =
        new Supplier<T>() {
          @Override
          public T get() {
            return object;
          }
        };
    return supplier;
  }

  private static final class WeakReferenceSupplier<T extends @Nullable Object>
      implements Supplier<T> {
    private final WeakReference<Holder<T>> weakHolder;

    public WeakReferenceSupplier(Holder<T> holder) {
      this.weakHolder = new WeakReference<>(holder);
    }

    @Override
    public T get() {
      return weakHolder.get().get();
    }
  }

  @SuppressWarnings("ClassCanBeRecord")
  private static final class WeakAnnotationSupplier<T extends @Nullable Object>
      implements Supplier<T> {
    @Weak private final Holder<T> holder;

    public WeakAnnotationSupplier(Holder<T> holder) {
      this.holder = holder;
    }

    @Override
    public T get() {
      return holder.object;
    }
  }

  @WeakOuter
  private final class WeakOuterAnnotationSupplier implements Supplier<T> {
    @Override
    public T get() {
      return object;
    }
  }
}
