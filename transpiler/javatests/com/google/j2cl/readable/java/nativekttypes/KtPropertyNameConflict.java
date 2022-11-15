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
import javaemul.internal.annotations.KtProperty;

public abstract class KtPropertyNameConflict {
  public int fieldWithConflictingGetter;
  public int fieldWithConflictingNonGetter;
  public int fieldWithConflictingRenamedMethod;

  @KtProperty
  public int getFieldWithConflictingGetter() {
    return fieldWithConflictingGetter;
  }

  @KtProperty
  public int fieldWithConflictingNonGetter() {
    return fieldWithConflictingNonGetter;
  }

  @KtProperty
  @KtName("fieldWithConflictingRenamedMethod")
  public int renamedMethod() {
    return fieldWithConflictingRenamedMethod;
  }

  @KtProperty
  public int getFieldWithConflictingSuperMethod() {
    return 0;
  }

  public interface Interface {
    @KtProperty
    int fieldWithConflictingInterfaceMethod();
  }

  public abstract class Subclass extends KtPropertyNameConflict implements Interface {
    int fieldWithConflictingSuperMethod;
    int fieldWithConflictingInterfaceMethod;

    @Override
    public int fieldWithConflictingInterfaceMethod() {
      return fieldWithConflictingInterfaceMethod;
    }
  }

  public static void test(KtPropertyNameConflict o, Interface i) {
    int fieldWithConflictingGetter = o.fieldWithConflictingGetter;
    int conflictingGetter = o.getFieldWithConflictingGetter();
    int fieldWithConflictingNonGetter = o.fieldWithConflictingNonGetter;
    int conflictingNonGetter = o.fieldWithConflictingNonGetter();
    int fieldWithConflictingRenamedMethod = o.fieldWithConflictingRenamedMethod;
    int conflictingRenamedMethod = o.renamedMethod();
    int conflictingSuperMethod = o.getFieldWithConflictingSuperMethod();
    int conflictingInterfaceMethod = i.fieldWithConflictingInterfaceMethod();
  }

  public static void test(Subclass o) {
    int fieldWithConflictingGetter = o.fieldWithConflictingGetter;
    int conflictingGetter = o.getFieldWithConflictingGetter();
    int fieldWithConflictingNonGetter = o.fieldWithConflictingNonGetter;
    int conflictingNonGetter = o.fieldWithConflictingNonGetter();
    int fieldWithConflictingRenamedMethod = o.fieldWithConflictingRenamedMethod;
    int conflictingRenamedMethod = o.renamedMethod();
    int fieldWithConflictingSuperMethod = o.fieldWithConflictingSuperMethod;
    int conflictingSuperMethod = o.getFieldWithConflictingSuperMethod();
    int fieldWithConflictingInterfaceMethod = o.fieldWithConflictingInterfaceMethod;
    int conflictingInterfaceMethod = o.fieldWithConflictingInterfaceMethod();
  }
}
