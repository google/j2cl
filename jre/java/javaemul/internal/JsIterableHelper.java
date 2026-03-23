/*
 * Copyright 2026 Google Inc.
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
package javaemul.internal;

import java.util.Iterator;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** A helper class to create JavaScript iterable objects. */
public final class JsIterableHelper {

  /** Abstraction for JavaScript Iterable. */
  @JsType(isNative = true, name = "Iterable", namespace = JsPackage.GLOBAL)
  public interface JsIterable<T> {
    @JsMethod(name = "[Symbol.iterator]")
    JsIterator<T> _private_jsIterator__();
  }

  /** Abstraction for JavaScript Iterator. */
  @JsType(isNative = true, name = "Iterator", namespace = JsPackage.GLOBAL)
  public interface JsIterator<T> {
    @JsMethod(name = "next")
    IIterableResult<T> _private_jsNext__();
  }

  /** Abstraction for JavaScript IIterableResult. */
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public interface IIterableResult<T> {}

  @JsMethod
  public static native <T> IIterableResult<T> makeResult(T value, boolean done);

  public static <T> JsIterable<T> asJsIterable(Iterable<T> iterable) {
    return new JsIterableAdapter<T>(iterable);
  }

  private static final class JsIterableAdapter<T> implements JsIterable<T> {
    private final Iterable<T> delegate;

    private JsIterableAdapter(Iterable<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public final JsIterableIteratorAdapter<T> _private_jsIterator__() {
      return new JsIterableIteratorAdapter<>(delegate.iterator());
    }
  }

  /**
   * Adapts a Java {@link Iterator} to also implement the {@link JsIterable} API. This ultimately
   * makes this type an {@code IteratorIterable} for JS.
   *
   * <p>In general JS expects all Iterators to themselves be iterable, see:
   * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Iteration_protocols#the_iterator_protocol:~:text=Such%20object%20is,not%20iterators.)
   */
  private static final class JsIterableIteratorAdapter<T> implements JsIterator<T>, JsIterable<T> {
    private final JsIterator<T> delegate;

    private JsIterableIteratorAdapter(JsIterator<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public final IIterableResult<T> _private_jsNext__() {
      return delegate._private_jsNext__();
    }

    @Override
    public final JsIterator<T> _private_jsIterator__() {
      return this;
    }
  }

  private JsIterableHelper() {}
}
