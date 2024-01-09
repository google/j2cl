/*
 * Copyright 2022 Google Inc.
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
package systemgetproperty;

public class Main {

  private static final String property = System.getProperty("jre.classMetadata");
  private static final String propertyWithDefault =
      System.getProperty("jre.classMetadata", "default");

  public static void main(String[] args) {
    boolean x;
    x = property == "SIMPLE";
    x = property.equals("SIMPLE");
    x = property.equalsIgnoreCase("SIMPLE");
    x = property == "NOTSIMPLE";

    x = propertyWithDefault.equals("SIMPLE");
    x = System.getProperty("jre.bar", "bar").equals("bar");
    x = System.getProperty("jre.classMetadata", mightHaveSideEffects()).equals("SIMPLE");
    x = System.getProperty("unset.property", mightHaveSideEffects()).equals("foo");
  }

  private static String mightHaveSideEffects() {
    return "Foo";
  }
}
