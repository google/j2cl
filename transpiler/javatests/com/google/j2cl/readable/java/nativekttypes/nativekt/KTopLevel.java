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
package nativekttypes.nativekt;

public class KTopLevel<O> {
  public static class KNested<N> {
    public N instanceField;
    public static Object staticField;

    public KNested(N n) {}

    public N instanceMethod(N n) {
      return n;
    }

    public static <S> S staticMethod(S s) {
      return s;
    }

    public int renamedMethod() {
      return 0;
    }
  }

  public class KInner<I> {
    public KInner(I i) {}
  }

  public O instanceField;
  public static Object staticField;
  public int renamedField;

  public int renamedMethod() {
    return 0;
  }

  public int methodAsProperty;
  public int nonGetMethodAsProperty;
  public int renamedMethodAsProperty;
  public int getRenamedMethodAsProperty;
  public boolean isRenamedField;
  public boolean isMethodAsProperty;
  public int getstartingmethodAsProperty;

  public KTopLevel(O o) {}

  public O instanceMethod(O o) {
    return o;
  }

  public static <S> S staticMethod(S s) {
    return s;
  }
}
