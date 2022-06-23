/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.junit.apt;

import java.util.Comparator;
import javax.lang.model.element.TypeElement;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

// Similar implementation to org.junit.internal.MethodSorter, but that class is based
// on Java reflection while we use the APT API.
// Also our methods can not have parameters and thus the implementation can be simpler.
class MethodSorter {

  /** Method name ascending lexicographic sort order. */
  static final Comparator<Method> NAME_ASCENDING = Comparator.comparing(Method::javaMethodName);

  /** DEFAULT sort order */
  static final Comparator<Method> DEFAULT =
      (m1, m2) -> {
        int i1 = m1.javaMethodName().hashCode();
        int i2 = m2.javaMethodName().hashCode();
        if (i1 == i2) {
          return NAME_ASCENDING.compare(m1, m2);
        }
        return i1 < i2 ? -1 : 1;
      };

  public static Comparator<Method> getTestSorter(TypeElement typeElement) {
    FixMethodOrder fixMethodOrder = typeElement.getAnnotation(FixMethodOrder.class);
    return fixMethodOrder != null && fixMethodOrder.value() == MethodSorters.NAME_ASCENDING
        ? NAME_ASCENDING
        : DEFAULT;
  }
}
