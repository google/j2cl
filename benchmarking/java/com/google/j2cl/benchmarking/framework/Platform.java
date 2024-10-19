/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.benchmarking.framework;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Helper class defining platform specific operations. */
final class Platform {

  private static final long FORCE_GC_TIMEOUT_SECS = 2;

  /**
   * Runs the garbage collector.
   *
   * <p>Adapted from caliper project: com/google/caliper/util/Util.java
   */
  @GwtIncompatible // Relies on runFinalization and threading.
  static void forceGc() {
    System.gc();
    System.runFinalization();
    final CountDownLatch latch = new CountDownLatch(1);

    // Create, then throw away, an object that will trigger the latch when finalized away.
    Object unused =
        new Object() {
          @Override
          protected void finalize() {
            latch.countDown();
          }
        };
    unused = null;

    System.gc();
    System.runFinalization();
    try {
      latch.await(FORCE_GC_TIMEOUT_SECS, SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // Hack to use on non-JVM platform compiling after above forceGc stripped.
  static void forceGc(Object... args) {
    nativeGc();
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "gc")
  private static native void nativeGc();

  @Target(ElementType.METHOD)
  public @interface GwtIncompatible {}

  private Platform() {}
}
