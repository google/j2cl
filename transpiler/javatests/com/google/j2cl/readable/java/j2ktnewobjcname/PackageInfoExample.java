/*
 * Copyright 2023 Google Inc.
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
package j2ktnewobjcname;

import j2ktnewobjcname.depspackage.FooFromDeps;
import j2ktnewobjcname.srcspackage.FooFromSrcs;

public final class PackageInfoExample {
  // Expects @ObjCName("withFromSrcsPrefixFooFromDeps") annotation for "foo" parameter
  public FooFromDeps apply(FooFromDeps foo) {
    return foo;
  }

  // Expects @ObjCName("withFromSrcsPrefixFooFromSrcs") annotation for "foo" parameter
  public FooFromSrcs apply(FooFromSrcs foo) {
    return foo;
  }
}
