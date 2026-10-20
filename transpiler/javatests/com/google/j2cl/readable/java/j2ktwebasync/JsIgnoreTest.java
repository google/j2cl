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

import com.google.calendar.v2a.shared.async.WebAsync;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Mirrors {@code
 * javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/JsIgnoreTest.java} so the J2J
 * and J2KT outputs can be compared.
 */
@JsType
public final class JsIgnoreTest {

  @JsType
  public interface IgnoredMethodsInterface {

    @JsIgnore
    long ignoredInterfaceMethod();

    @JsIgnore
    @WebAsync
    long ignoredAsyncInterfaceMethod();

    @WebAsync
    long asyncInterfaceMethod();
  }

  public static final class IgnoredMethodsInterfaceImpl implements IgnoredMethodsInterface {

    @Override
    public long ignoredInterfaceMethod() {
      return 0;
    }

    @Override
    @WebAsync
    public long ignoredAsyncInterfaceMethod() {
      return 1;
    }

    @Override
    @WebAsync
    public long asyncInterfaceMethod() {
      return 2;
    }
  }

  @JsType
  public abstract static class IgnoredMethodsAbstractClass {

    @JsIgnore
    public abstract long ignoredAbstractMethod();

    @JsIgnore
    @WebAsync
    public abstract long ignoredAsyncAbstractMethod();

    @WebAsync
    public abstract long asyncAbstractMethod();
  }

  public static final class IgnoredMethodsAbstractClassImpl extends IgnoredMethodsAbstractClass {

    @Override
    public long ignoredAbstractMethod() {
      return 3;
    }

    @Override
    @WebAsync
    public long ignoredAsyncAbstractMethod() {
      return 4;
    }

    @Override
    @WebAsync
    public long asyncAbstractMethod() {
      return 5;
    }
  }

  @JsIgnore
  @WebAsync
  public long ignoredAsyncMethod() {
    return 6;
  }
}
