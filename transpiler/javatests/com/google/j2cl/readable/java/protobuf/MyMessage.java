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

@ProtoNonnullApi
public class MyMessage extends GeneratedMessage {

  public final int testField;

  public int getTestField() {
    return testField;
  }

  public static MyMessage getDefaultInstance() {
    return new MyMessage(0);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  private MyMessage(int testField) {
    this.testField = testField;
  }

  @ProtoNonnullApi
  public static class Builder extends GeneratedMessage.Builder {
    public int testField;

    public int getTestField() {
      return testField;
    }

    public Builder setTestField(int testField) {
      this.testField = testField;
      return this;
    }

    public MyMessage build() {
      return new MyMessage(testField);
    }

    private Builder() {}
  }
}
