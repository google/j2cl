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
 * Mirrors {@code javatests/com/google/apps/xplat/tools/j2j/plugins/webasync/testdata/Lambda.java}
 * so the J2J and J2KT outputs can be compared.
 */
public class Lambda {

  @SuppressWarnings("unused")
  public void testVoid() {
    VoidInterface instance1 = () -> {};
    VoidInterface instance2 = () -> syncVoidMethod();
    VoidInterface instance4 = () -> asyncVoidMethod();
    VoidInterface instance5 = this::asyncVoidMethod;
    VoidInterface instance6 =
        () -> {
          syncVoidMethod();
        };
    VoidInterface instance7 =
        () -> {
          asyncVoidMethod();
        };
  }

  @SuppressWarnings("unused")
  public void testInt() {
    IntegerInterface instance1 =
        i -> {
          return i;
        };
    IntegerInterface instance2 = i -> syncIntMethod(i);
    IntegerInterface instance4 = i -> asyncIntMethod(i);
    IntegerInterface instance5 = this::asyncIntMethod;
    IntegerInterface instance6 =
        i -> {
          return syncIntMethod(i);
        };
    IntegerInterface instance7 =
        i -> {
          return asyncIntMethod(i);
        };

    IntegerInterface instance8 = i -> 5;
  }

  public void syncVoidMethod() {}

  @WebAsync
  public void asyncVoidMethod() {}

  public int syncIntMethod(int i) {
    return i;
  }

  @WebAsync
  public int asyncIntMethod(int i) {
    return i;
  }

  public interface VoidInterface {
    @WebAsync
    void methodVoid();
  }

  public interface IntegerInterface {
    @WebAsync
    int methodInt(int i);
  }
}
