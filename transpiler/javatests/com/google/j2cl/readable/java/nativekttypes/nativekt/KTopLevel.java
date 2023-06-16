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

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtProperty;
import org.jspecify.nullness.Nullable;

public class KTopLevel<O> {
  public static class KNested<N> {
    public @Nullable N instanceField;
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

  public @Nullable O instanceField;
  public static Object staticField;
  public int renamedField;

  public int renamedMethod() {
    return 0;
  }

  @KtProperty
  public int methodAsProperty() {
    return 0;
  }

  @KtProperty
  public int nonGetMethodAsProperty() {
    return 0;
  }

  @KtProperty
  public int renamedMethodAsProperty() {
    return 0;
  }

  @KtProperty
  @KtName("getRenamedMethodAsProperty")
  public int getRenamedMethodAsProperty() {
    return 0;
  }

  public boolean isRenamedField;

  @KtProperty
  public boolean isMethodAsProperty() {
    return false;
  }

  @KtProperty
  public int getstartingmethodAsProperty() {
    return 0;
  }

  public KTopLevel(O o) {}

  public O instanceMethod(O o) {
    return o;
  }

  public static <S> S staticMethod(S s) {
    return s;
  }
}
