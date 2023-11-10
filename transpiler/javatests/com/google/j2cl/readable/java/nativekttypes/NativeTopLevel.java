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
package nativekttypes;

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtProperty;

@KtNative(name = "nativekttypes.nativekt.KTopLevel")
public class NativeTopLevel<O> {

  @KtNative(name = "nativekttypes.nativekt.KTopLevel.KNested")
  public static class Nested<N> {
    public N instanceField;
    public static Object staticField;

    public Nested(N n) {}

    public native N instanceMethod(N n);

    public static native <S> S staticMethod(S s);
  }

  @KtNative(name = "nativekttypes.nativekt.KTopLevel.KInner")
  public class Inner<I> {
    public Inner(I i) {}
  }

  public O instanceField;
  public static Object staticField;

  @KtName("renamedField")
  public int fieldToRename;

  @KtName("renamedMethod")
  public native int methodToRename();

  @KtProperty
  public native int getMethodAsProperty();

  @KtProperty
  public native int nonGetMethodAsProperty();

  @KtProperty
  @KtName("renamedMethodAsProperty")
  public native int methodToRenameAsProperty();

  @KtProperty
  @KtName("getRenamedMethodAsProperty")
  public native int getMethodToRenameAsProperty();

  @KtName("isRenamedField")
  public boolean isFieldToRename;

  @KtProperty
  public native boolean isMethodAsProperty();

  @KtProperty
  public native int getstartingmethodAsProperty();

  public NativeTopLevel(O o) {}

  public native O instanceMethod(O o);

  public static native <S> S staticMethod(S s);
}
