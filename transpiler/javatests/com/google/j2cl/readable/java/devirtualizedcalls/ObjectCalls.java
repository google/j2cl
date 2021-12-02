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
package devirtualizedcalls;

public class ObjectCalls {
  @SuppressWarnings({"ArrayEquals", "ArrayHashCode", "ArrayToString"})
  public void main() {
    Object object = new Object();

    object.equals(object);
    object.hashCode();
    object.toString();
    object.getClass();

    ObjectCalls[] objectCalls = new ObjectCalls[1];
    objectCalls.equals(objectCalls);
    objectCalls.hashCode();
    objectCalls.toString();
    objectCalls.getClass();
  }

  // test object calls in the same declaring class with implicit qualifiers and this qualifier.
  public void test() {
    equals(new Object());
    hashCode();
    toString();
    getClass();

    this.equals(new Object());
    this.hashCode();
    this.toString();
    this.getClass();
  }

  public boolean testNotEquals() {
    return !equals(new Object());
  }
}
