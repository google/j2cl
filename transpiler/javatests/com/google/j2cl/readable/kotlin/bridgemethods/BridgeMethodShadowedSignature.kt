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
package bridgemethods

import java.util.function.Consumer

class BridgeMethodShadowedSignature {
  internal interface I<I1> {
    fun get(consumer: Consumer<I1>): String?
  }

  abstract class B<B1, B2> : I<B1> {
    fun get(consumer: B2): String {
      return "B get B2"
    }
  }

  internal class C<C1> :
    B<C1, Consumer<C1>>(),
    I<C1> { // Needs a bridge from the I.get(Consumer) signature to B.get(Object) implementation.
  }
}
