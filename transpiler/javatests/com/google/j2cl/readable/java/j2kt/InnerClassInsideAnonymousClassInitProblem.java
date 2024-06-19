/*
 * Copyright 2024 Google Inc.
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
package j2kt;

import javaemul.internal.annotations.KtProperty;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InnerClassInsideAnonymousClassInitProblem {
  static final Object obj =
      new Object() {
        class InnerClassWithSingleConstructor {
          final int i;

          InnerClassWithSingleConstructor(int i) {
            this.i = i;
          }

          @KtProperty
          int property() {
            // To overcome the problem with Kotlin Compiler which complains that "i" is not
            // initialized, J2KT renders the constructor as primary. The workaround does not work
            // when there are two or more constructors.
            // See: https://youtrack.jetbrains.com/issue/KT-65299
            return i;
          }
        }
      };
}
