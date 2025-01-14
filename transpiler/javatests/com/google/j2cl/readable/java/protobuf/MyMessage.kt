/*
 * Copyright 2024 Google Inc.
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
package protobuf

import com.google.protobuf.GeneratedMessage
import com.google.protobuf.Parser

/**
 * Proto message, as if it was generated from:
 * ```
 * message MyMessage {
 *   int32 foo = 1;
 *   int32 foo_bar = 2;
 *   int32 x = 3;
 *   int32 x_value = 4;
 *   int32 x_y_z = 5;
 *   int32 x_y_z_values = 6;
 * }
 * ```
 */
class MyMessage : GeneratedMessage() {
  override val parserForType: Parser<MyMessage>
    get() = TODO()

  val foo: Int = TODO()
  val fooBar: Int = TODO()
  val x: Int = TODO()
  val xValue: Int = TODO()
  val xyz: Int = TODO()
  val xyzValues: Int = TODO()

  class Builder : GeneratedMessage.Builder() {
    var foo: Int = TODO()
    var fooBar: Int = TODO()
    var x: Int = TODO()
    var xValue: Int = TODO()
    var xyz: Int = TODO()
    var xyzValues: Int = TODO()

    // Needed for J2KT
    fun setFoo(foo: Int): Builder = TODO()

    fun setFooBar(fooBar: Int): Builder = TODO()

    fun setX(x: Int): Builder = TODO()

    fun setXValue(xValue: Int): Builder = TODO()

    fun setXYZ(xyz: Int): Builder = TODO()

    fun setXYZValues(xyzValues: Int): Builder = TODO()

    fun build(): MyMessage = TODO()
  }

  companion object {
    fun getDefaultInstance(): MyMessage = TODO()

    fun newBuilder(): Builder = TODO()
  }
}
