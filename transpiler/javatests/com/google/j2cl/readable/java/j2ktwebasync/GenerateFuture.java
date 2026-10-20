/*
 * Copyright 2026 Google Inc.
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
package j2ktwebasync;

import static com.google.calendar.v2a.shared.async.AsyncAwait.await;

import com.google.calendar.v2a.shared.async.WebAsync;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.j2objc.annotations.ObjectiveCKmpMethod;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * Tests J2KT-specific {@code @WebAsync(generateFuture = true)} bridge synthesis, {@code AsyncAwait}
 * call rewriting, and annotation handling ({@code @JsType}, {@code @JsMethod}, {@code @JsFunction},
 * and {@code @ObjectiveCKmpMethod}).
 */
public class GenerateFuture {

  @WebAsync(generateFuture = true)
  public String explicitFuture(String id) {
    return id;
  }

  @WebAsync(generateFuture = true)
  public void voidFuture() {}

  @WebAsync
  public String awaitFuture(ListenableFuture<String> future) {
    return await(future);
  }

  public interface AsyncInterface {
    @WebAsync(generateFuture = true)
    int compute(int x);
  }

  public static class AsyncInterfaceImpl implements AsyncInterface {
    @Override
    @WebAsync(generateFuture = true)
    public int compute(int x) {
      return x;
    }
  }

  @JsType
  public interface JsTypeService {
    @JsMethod(name = "customName")
    @WebAsync(generateFuture = true)
    String getData(String id);
  }

  @JsFunction
  public interface AsyncJsFunction {
    @WebAsync(generateFuture = true)
    void run(String input);
  }

  public static class KmpAdapter {}

  public static class KmpService {
    @WebAsync(generateFuture = true)
    @ObjectiveCKmpMethod(selector = "customSelector:", adapter = KmpAdapter.class)
    public String getData(String id) {
      return id;
    }
  }
}
