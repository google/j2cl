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
package j2ktiosinterop;

public final class DefaultNames {
  public DefaultNames() {}

  public DefaultNames(int i) {}

  public DefaultNames(int i, String s) {}

  public void method() {}

  public void booleanMethod(boolean c) {}

  public void charMethod(char c) {}

  public void byteMethod(byte b) {}

  public void shortMethod(short s) {}

  public void intMethod(int i) {}

  public void longMethod(long l) {}

  public void floatMethod(float f) {}

  public void doubleMethod(double d) {}

  public void objectMethod(Object obj) {}

  public void stringMethod(String obj) {}

  @SuppressWarnings("AvoidObjectArrays")
  public void stringArrayMethod(String[] sa) {}

  @SuppressWarnings("AvoidObjectArrays")
  public void stringArrayArrayMethod(String[][] saa) {}

  public void cloneableMethod(Cloneable c) {}

  public void numberMethod(Number c) {}

  public void classMethod(Class<String> c) {}

  public void stringIterableMethod(Iterable<String> i) {}

  public void intStringMethod(int i, String s) {}

  public <T> void genericMethod(T t) {}

  public <T extends String> void genericStringMethod(T t) {}

  public void get(int i) {}

  public int intField;

  @SuppressWarnings("NonFinalStaticField")
  public static int staticIntField;

  public static void staticMethod() {}

  public static void staticIntMethod(int i) {}

  public static void staticIntStringMethod(int i, String s) {}
}
