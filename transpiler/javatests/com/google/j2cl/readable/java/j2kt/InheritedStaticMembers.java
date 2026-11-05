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
package j2kt;

import j2kt.relaxedvisibility.PublicClass;

/**
 * Accesses a static member inherited from a package-private superclass declared in a J2KT
 * transpiled library.
 *
 * <p>Unlike j2ktnotpassing.InheritedStaticMembers, this works because the superclass is made public
 * in Kotlin by relaxed visibility. The reference must stay qualified by the declaring type since
 * Kotlin companion object members are not inherited.
 */
public class InheritedStaticMembers {
  public static int getStaticField() {
    return PublicClass.staticField;
  }
}
