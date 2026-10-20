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
package com.google.calendar.v2a.shared.async;

import com.google.common.util.concurrent.ListenableFuture;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Suspension {
  @WebAsync
  public static <T extends @Nullable Object> T await(ListenableFuture<T> future) {
    throw new AssertionError();
  }

  public static <T extends @Nullable Object> ListenableFuture<T> future(AsyncCallable<T> callable) {
    throw new AssertionError();
  }

  public static ListenableFuture<?> futureVoid(AsyncRunnable runnable) {
    throw new AssertionError();
  }

  @WebAsync
  public static void lock(@Nullable Object lock) {}

  public static void unlock(@Nullable Object lock) {}
}
