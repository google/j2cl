/*
 * Copyright 2016 Google Inc.
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
package java.lang.invoke;

//TODO(dankurka): Only needed until g3 is on Java8, afterwards delete
public class LambdaMetafactory {
  public static CallSite metafactory(
      Object caller,
      String invokedName,
      Object invokedType,
      Object samMethodType,
      Object implMethod,
      Object instantiatedMethodType) {
    return null;
  }

  public static CallSite altMetafactory(
      Object caller, String invokedName, Object invokedType, Object... args) {
    return null;
  }
}
