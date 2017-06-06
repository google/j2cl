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
package com.google.j2cl.transpiler.readable.casttoboxedtype;

public class CastToBoxedType {
  @SuppressWarnings("unused")
  public void test() {
    Object o = new Integer(1);
    Byte b = (Byte) o;
    Double d = (Double) o;
    Float f = (Float) o;
    Integer i = (Integer) o;
    Long l = (Long) o;
    Short s = (Short) o;
    Number n = (Number) o;
    Character c = (Character) o;
    Boolean bool = (Boolean) o;
  }
}
