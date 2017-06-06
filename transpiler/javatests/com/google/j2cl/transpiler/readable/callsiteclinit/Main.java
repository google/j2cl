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
package com.google.j2cl.transpiler.readable.callsiteclinit;

// We should be inserting a Yo.clinit() in the constructor for Main, the clinit of Main and in
// Main.asdf.
public class Main {
  public static class Yo {
    public static String a = "asdf";
  }

  public static class Yar {
    public static String b = "qwerty";
  }

  public static class Yas {
    public static String c = "string";
  }

  private static String staticField = Yo.a;

  private static String staticWithBlock;

  static {
    String temp = Yas.c;
    {
      // Test inner block.
      staticWithBlock = Yar.b;
    }
  }

  private String instanceField = Yo.a;
  private String instanceWithBlock;

  {
    instanceWithBlock = Yar.b;
  }

  public void asdf() {
    String insideMethod = Yo.a;
  }
}
