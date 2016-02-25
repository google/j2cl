/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

import jsinterop.annotations.JsMethod;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Object.html">the
 * official Java API doc</a> for details.
 */
public class Object {

  public boolean equals(Object that) {
    // Super-source replaced.
    return false;
  }

  //TODO(dankurka): this method should actually be final, but because of GWT's String class it can not
  public Class<?> getClass() {
    // Super-source replaced.
    return null;
  }

  public int hashCode() {
    // Super-source replaced.
    return 0;
  }

  @JsMethod(name = "$javaToString")
  public String toString() {
    // Super-source replaced.
    return null;
  }


  @JsMethod(name = "toString")
  private String toStringBridge() {
    // Super-source replaced.
    return null;
  }
}
