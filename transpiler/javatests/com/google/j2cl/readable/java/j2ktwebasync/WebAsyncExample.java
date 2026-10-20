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

/**
 * Mirrors {@code
 * javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/WebAsyncExample.java} so the
 * J2J and J2KT outputs can be compared.
 */
public class WebAsyncExample {

  @WebAsync
  @SuppressWarnings("unused")
  public A method1() {
    int i = getFive();
    return method2(A.<A>getNull());
  }

  @WebAsync
  public A method2(A a) {
    return a;
  }

  @WebAsync
  public int getFive() {
    return 5;
  }

  @WebAsync
  @SuppressWarnings("unused")
  public void varargs(int a, int... more) {}

  public static final class A {
    @WebAsync
    public static <T> T getNull() {
      return null;
    }

    private A() {}
  }
}
