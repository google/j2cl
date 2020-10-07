/*
 * Copyright 2017 Google Inc.
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
package bridgemethods;

import java.util.function.Consumer;

public class BridgeMethodShadowedSignature {

  interface I<I1> {
    String get(Consumer<? super I1> consumer);
  }

  abstract static class B<B1, B2> implements I<B1> {
    public String get(B2 consumer) {
      return "B get B2";
    }
  }

  static class C<C1> extends B<C1, Consumer<? super C1>> implements I<C1> {
    // Needs a bridge from the I.get(Consumer) signature to B.get(Object) implementation.
  }
}

