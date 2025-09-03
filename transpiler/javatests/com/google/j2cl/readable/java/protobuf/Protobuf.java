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

import com.google.j2objc.annotations.ObjectiveCName;
import com.google.protobuf.ExtensionLite;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Protobuf {
  public void test() {
    MyMessage.Builder builder = MyMessage.newBuilder();

    builder.getFoo();
    builder.setFoo(1);

    builder.getFooBar();
    builder.setFooBar(2);

    builder.getX();
    builder.setX(3);

    builder.getXValue();
    builder.setXValue(4);

    builder.getXYZ();
    builder.setXYZ(5);

    builder.getXYZValues();
    builder.setXYZValues(6);

    MyMessage message = builder.build();
    message.getFoo();
    message.getFooBar();
    message.getX();
    message.getXValue();
    message.getXYZ();
    message.getXYZValues();

    MyMessage chainedMessage =
        MyMessage.newBuilder()
            .setFoo(1)
            .setFooBar(2)
            .setX(3)
            .setXValue(4)
            .setXYZ(5)
            .setXYZValues(6)
            .build();

    MyMessage defaultMessage = MyMessage.getDefaultInstance();
    defaultMessage.getFoo();

    Parser<?> parser = message.getParserForType();
    Parser<? extends GeneratedMessage> generatedMessageParser =
        ((GeneratedMessage) message).getParserForType();

    MyEnum enumOne = MyEnum.ONE;
    int enumOneNumber = MyEnum.ONE.getNumber();
  }

  public <T> void testExtension(ExtensionLite<T> extension) {
    int i = extension.getNumber();
  }

  public static class Test {
    public Test(MyMessage myMessage) {}

    public void accept(MyMessage myMessage) {}

    public static void acceptStatic(MyMessage myMessage) {}

    @ObjectiveCName("acceptStaticWithMyMessage:")
    public static void acceptStaticWithObjectiveCName(MyMessage myMessage) {}

    public MyMessage get() {
      throw new RuntimeException();
    }

    public static MyMessage getStatic(MyMessage myMessage) {
      throw new RuntimeException();
    }

    @ObjectiveCName("getStaticWithMyMessage:")
    public static MyMessage getStaticWithObjectiveCName(MyMessage myMessage) {
      throw new RuntimeException();
    }
  }
}
