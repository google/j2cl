/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.tools.rta.staticproperties;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

public class StaticPropertiesAccessor {

  @JsMethod
  public static void entryPoint() {
    int primitive = 5;
    Object object = new Object();
    StaticPrimitiveProperties.unreadAssignedProperty = primitive;
    primitive = StaticPrimitiveProperties.readUnassignedProperty;
    StaticPrimitiveProperties.readAssignedProperty = primitive;
    primitive = StaticPrimitiveProperties.readAssignedProperty;
    StaticObjectProperties.unreadAssignedProperty = object;
    object = StaticObjectProperties.readUnassignedProperty;
    StaticObjectProperties.readAssignedProperty = object;
    object = StaticObjectProperties.readAssignedProperty;
  }
}

class StaticPrimitiveProperties {
  public static int unreadUnassignedProperty;
  @JsProperty public static int ExposedUnreadUnassignedProperty;
  public static int unreadAssignedProperty;
  public static int readUnassignedProperty;
  public static int readAssignedProperty;
}

class StaticObjectProperties {
  public static Object unreadUnassignedProperty;
  @JsProperty public static Object ExposedUnreadUnassignedProperty;
  public static Object unreadAssignedProperty;
  public static Object readUnassignedProperty;
  public static Object readAssignedProperty;
}
