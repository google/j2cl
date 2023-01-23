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
package j2ktobjcname;

import com.google.j2objc.annotations.ObjectiveCName;

public class MethodExample {

  public class Foo {}

  public class ObjCName {

    public void instanceMethod(int i) {}

    public void instanceMethod(int i, long[] l) {}

    public void instanceMethod(int i, long[][] l) {}

    public void instanceMethod(Foo foo) {}

    @ObjectiveCName("newFoo")
    public void foo() {}

    @ObjectiveCName("newProtectedFoo")
    protected void protectedFoo() {}
  }

  public static void main(String... args) {}
}
