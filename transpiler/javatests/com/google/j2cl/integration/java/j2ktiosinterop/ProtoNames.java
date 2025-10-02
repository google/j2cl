/*
 * Copyright 2025 Google Inc.
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
package j2ktiosinterop;

import com.google.j2objc.annotations.ObjectiveCName;

public final class ProtoNames {
  private ProtoNames() {}

  public static void consume(DefaultProtoMessage message) {}

  public static void consume(DefaultProtoEnum enumValue) {}

  public static void consume(CustomPrefixProtoMessage message) {}

  public static void consume(CustomPrefixProtoEnum enumValue) {}

  public static DefaultProtoMessage produceDefaultMessage() {
    return DefaultProtoMessage.newBuilder()
        .setMyString("hello")
        .setMyEnum(DefaultProtoEnum.DEFAULT_ENUM_VALUE1)
        .build();
  }

  public static DefaultProtoEnum produceDefaultEnum() {
    return DefaultProtoEnum.DEFAULT_ENUM_VALUE2;
  }

  public static CustomPrefixProtoMessage produceCustomMessage() {
    return CustomPrefixProtoMessage.newBuilder()
        .setMyString("hello")
        .setMyEnum(CustomPrefixProtoEnum.CUSTOM_PREFIX_ENUM_VALUE1)
        .build();
  }

  public static CustomPrefixProtoEnum produceCustomEnum() {
    return CustomPrefixProtoEnum.CUSTOM_PREFIX_ENUM_VALUE2;
  }

  @ObjectiveCName("customConsumeDefaultMessage")
  public static void customConsumeWithoutColon(DefaultProtoMessage message) {}

  @ObjectiveCName("customConsumeDefaultEnum")
  public static void customConsumeWithoutColon(DefaultProtoEnum enumValue) {}

  @ObjectiveCName("customConsumeCustomMessage")
  public static void customConsumeWithoutColon(CustomPrefixProtoMessage message) {}

  @ObjectiveCName("customConsumeCustomEnum")
  public static void customConsumeWithoutColon(CustomPrefixProtoEnum enumValue) {}

  @ObjectiveCName("customConsumeDefaultMessage:")
  public static void customConsumeWithColon(DefaultProtoMessage message) {}

  @ObjectiveCName("customConsumeDefaultEnum:")
  public static void customConsumeWithColon(DefaultProtoEnum enumValue) {}

  @ObjectiveCName("customConsumeCustomMessage:")
  public static void customConsumeWithColon(CustomPrefixProtoMessage message) {}

  @ObjectiveCName("customConsumeCustomEnum:")
  public static void customConsumeWithColon(CustomPrefixProtoEnum enumValue) {}
}
