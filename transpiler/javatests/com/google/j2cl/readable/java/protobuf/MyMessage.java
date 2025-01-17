/*
 * Copyright 2022 Google Inc.
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

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Internal.ProtoNonnullApi;
import com.google.protobuf.Parser;
import org.jspecify.annotations.NullMarked;

/**
 * Proto message, as if it was generated from:
 *
 * <pre>{@code
 * message MyMessage {
 *   int32 foo = 1;
 *   int32 foo_bar = 2;
 *   int32 x = 3;
 *   int32 x_value = 4;
 *   int32 x_y_z = 5;
 *   int32 x_y_z_values = 6;
 * }
 * }</pre>
 */
@ProtoNonnullApi
@NullMarked
public class MyMessage extends GeneratedMessage {
  public Parser<MyMessage> getParserForType() {
    throw new RuntimeException();
  }

  public int getFoo() {
    throw new RuntimeException();
  }

  public int getFooBar() {
    throw new RuntimeException();
  }

  public int getX() {
    throw new RuntimeException();
  }

  public int getXValue() {
    throw new RuntimeException();
  }

  public int getXYZ() {
    throw new RuntimeException();
  }

  public int getXYZValues() {
    throw new RuntimeException();
  }

  public static MyMessage getDefaultInstance() {
    throw new RuntimeException();
  }

  public static Builder newBuilder() {
    throw new RuntimeException();
  }

  private MyMessage(int testField) {
    throw new RuntimeException();
  }

  @ProtoNonnullApi
  public static class Builder extends GeneratedMessage.Builder {
    public int getFoo() {
      throw new RuntimeException();
    }

    public int getFooBar() {
      throw new RuntimeException();
    }

    public int getX() {
      throw new RuntimeException();
    }

    public int getXValue() {
      throw new RuntimeException();
    }

    public int getXYZ() {
      throw new RuntimeException();
    }

    public int getXYZValues() {
      throw new RuntimeException();
    }

    public Builder setFoo(int foo) {
      throw new RuntimeException();
    }

    public Builder setFooBar(int foo) {
      throw new RuntimeException();
    }

    public Builder setX(int x) {
      throw new RuntimeException();
    }

    public Builder setXValue(int xValue) {
      throw new RuntimeException();
    }

    public Builder setXYZ(int xyz) {
      throw new RuntimeException();
    }

    public Builder setXYZValues(int xyzValues) {
      throw new RuntimeException();
    }

    public MyMessage build() {
      throw new RuntimeException();
    }

    private Builder() {}
  }
}
