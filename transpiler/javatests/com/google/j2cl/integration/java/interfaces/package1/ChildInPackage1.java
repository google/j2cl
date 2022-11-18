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

package interfaces.package1;

public class ChildInPackage1 extends ClassInPackage1WithPackagePrivateMethod
    implements InterfaceInPackage1 {

  // m needs to be implemented here since Java does not allow an interface method which is public
  // to expose the package private method.
  @Override
  public String m() {
    return "explicit-override-in-child";
  }
}
