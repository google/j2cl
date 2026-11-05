/*
 * Copyright 2026 Google Inc.
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
package j2ktnotpassing;

import j2ktnotpassing.inheritedstaticmembersdep.PublicClassFromJavaLibrary;

// TODO(b/581990423): Move to j2kt once J2KT qualifies the reference with
// PublicClassFromJavaLibrary instead of its package-private superclass, which is not accessible
// from Kotlin.
public class InheritedStaticMembers {
  public static int getStaticField() {
    return PublicClassFromJavaLibrary.staticField;
  }
}
