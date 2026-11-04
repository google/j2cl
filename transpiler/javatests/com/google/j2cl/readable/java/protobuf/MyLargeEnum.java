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
package protobuf;

import com.google.protobuf.Internal;
import com.google.protobuf.Internal.ProtoNonnullApi;

interface MyLargeEnum0 {
  MyLargeEnum ZERO = new MyLargeEnum();
  int ZERO_VALUE = 0;
  MyLargeEnum ONE = new MyLargeEnum();
  int ONE_VALUE = 1;
}

@ProtoNonnullApi
@SuppressWarnings("ShouldNotSubclass")
public final class MyLargeEnum implements Internal.EnumLite, MyLargeEnum0 {
  @Override
  public final int getNumber() {
    throw new RuntimeException();
  }
}
