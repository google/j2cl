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
 * Mirrors {@code javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/Super.java} so
 * the J2J and J2KT outputs can be compared.
 */
public final class Super {

  public static class A {
    @WebAsync
    public void m1() {}
  }

  public static class B extends A {
    @WebAsync
    @Override
    public void m1() {
      super.m1();
      super.m1();
    }

    @WebAsync
    public void m2() {
      this.m1();
      this.m1();
    }
  }

  private Super() {}
}
