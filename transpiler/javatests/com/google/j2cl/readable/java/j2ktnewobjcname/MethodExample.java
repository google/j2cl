/*
 * Copyright 2022 Google Inc.
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
package j2ktnewobjcname;

import com.google.j2objc.annotations.ObjectiveCName;

public class MethodExample {

  public static class Foo {}

  public static class Default {
    public void instanceMethod(int i) {}

    public void instanceMethod(int i, long[] l) {}

    public void instanceMethod(int i, long[][] l) {}

    public void instanceMethod(int i, Object id) {}

    public void instanceMethod(Foo foo) {}

    public void instanceMethod(Foo[] foo) {}

    public void instanceMethod(Foo[][] foo) {}

    public <T> void instanceMethod(T t) {}

    public <S extends String> void instanceMethod(S s) {}

    public static void staticMethod(int i) {}

    public static void staticMethod(int i, long[] l) {}

    public static void staticMethod(int i, long[][] l) {}

    public static void staticMethod(int i, Object id) {}

    public static void staticMethod(Foo foo) {}

    public static void staticMethod(Foo[] foo) {}

    public static void staticMethod(Foo[][] foo) {}

    public static <T> void staticMethod(T t) {}

    public static <S extends String> void staticMethod(S s) {}
  }

  public static class Custom {
    @ObjectiveCName("newFoo")
    public void foo() {}

    @ObjectiveCName("newProtectedFoo")
    protected void protectedFoo() {}

    @ObjectiveCName("implicitParams")
    public void implicitParams() {}

    @ObjectiveCName("implicitParams")
    public void implicitParams(int i) {}

    @ObjectiveCName("implicitParams")
    public void implicitParams(int i, String s) {}

    @ObjectiveCName("explicitParams")
    public void explicitParams() {}

    @ObjectiveCName("explicitParamsWithIndex:")
    public void explicitParams(int i) {}

    @ObjectiveCName("explicitParamsWithIndex:name:")
    public void explicitParams(int i, String s) {}
  }

  public static final class Special {
    public static final class WithBoolean {
      public void get(boolean x) {}
    }

    public static final class WithChar {
      public void get(char x) {}
    }

    public static final class WithByte {
      public void get(byte x) {}
    }

    public static final class WithShort {
      public void get(short x) {}
    }

    public static final class WithInt {
      public void get(int x) {}
    }

    public static final class WithLong {
      public void get(long x) {}
    }

    public static final class WithFloat {
      public void get(float x) {}
    }

    public static final class WithDouble {
      public void get(double x) {}
    }

    public static final class WithObject {
      public void get(Object x) {}
    }

    public static final class WithString {
      public void get(String x) {}
    }

    public static final class WithFoo {
      public void get(Foo x) {}
    }
  }

  public static void main(String... args) {}
}
