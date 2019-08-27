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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.CompilationRule;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MethodSorterTest {
  @Rule public CompilationRule compilation = new CompilationRule();

  @Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[] {org.junit.internal.MethodSorter.DEFAULT, MethodSorter.DEFAULT},
        new Object[] {org.junit.internal.MethodSorter.NAME_ASCENDING, MethodSorter.NAME_ASCENDING});
  }

  @Parameter public Comparator<Method> jUnitComparator;

  @Parameter(1)
  public Comparator<TestMethod> aptComparator;

  public static class Simple {
    public void c() {}

    public void b() {}

    public void a() {}

    public void b1() {}
  }

  @Test
  public void testSimpleSorting() {
    assertSorting(Simple.class);
  }

  private void assertSorting(Class<?> clazz) {

    TypeElement typeElement = compilation.getElements().getTypeElement(clazz.getCanonicalName());

    ImmutableList<ExecutableElement> methodsFromApt =
        ImmutableList.copyOf(ElementFilter.methodsIn(typeElement.getEnclosedElements()));
    ImmutableList<Method> methodsFromReflection =
        ImmutableList.copyOf(Arrays.asList(clazz.getDeclaredMethods()));

    ImmutableList<String> sortedClassMethodsFromApt =
        methodsFromApt.stream()
            .map(JUnit4TestDataExtractor::toTestMethod)
            .sorted(aptComparator)
            .map(TestMethod::javaMethodName)
            .collect(ImmutableList.toImmutableList());

    ImmutableList<String> sortedClassMethodsFromReflection =
        methodsFromReflection
            .stream()
            .sorted(jUnitComparator)
            .map(m -> m.getName())
            .collect(ImmutableList.toImmutableList());

    assertThat(sortedClassMethodsFromApt)
        .containsExactlyElementsIn(sortedClassMethodsFromReflection)
        .inOrder();
  }
}
