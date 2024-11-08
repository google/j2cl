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
import com.google.protobuf.Parser;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Protobuf {
  public void test() {
    MyMessage.Builder builder = MyMessage.newBuilder();
    builder.setTestField(42);

    builder.getTestField();

    MyMessage message = builder.build();
    message.getTestField();

    MyMessage chainedMessage = MyMessage.newBuilder().setTestField(1).setTestField(2).build();

    MyMessage defaultMessage = MyMessage.getDefaultInstance();
    defaultMessage.getTestField();

    Parser<?> parser = message.getParserForType();
    Parser<? extends GeneratedMessage> generatedMessageParser =
        ((GeneratedMessage) message).getParserForType();

    MyEnum enumOne = MyEnum.ONE;
    int enumOneNumber = MyEnum.ONE.getNumber();
  }
}
