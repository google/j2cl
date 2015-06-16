/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.ast.apt;

import static com.google.auto.common.MoreElements.isAnnotationPresent;

import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep;
import com.google.auto.common.MoreElements;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.Visitable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;

/**
 * J2clAstProcessingStep emits a visitor class.
 */
public class J2clAstProcessingStep implements ProcessingStep {

  private final ProcessingEnvironment processingEnv;

  public J2clAstProcessingStep(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return Sets.newHashSet(Visitable.class);
  }

  @Override
  public void process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    try {
      doProcess(elementsByAnnotation);
    } catch (Exception e) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      e.printStackTrace(printWriter);
      processingEnv
          .getMessager()
          .printMessage(
              Kind.ERROR, "Can not write java file: " + e.toString() + " " + stringWriter);
    }
  }

  private void doProcess(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation)
      throws IOException {

    for (Element value : elementsByAnnotation.values()) {
      // The @Test annotation is on methods of a class
      if (value.getKind() != ElementKind.METHOD) {
        processingEnv.getMessager().printMessage(Kind.WARNING, "Ignoring element", value);
        continue;
      }
      Element possibleClazz = value.getEnclosingElement();
      // We are not looking at junit tests that are not top level.
      if (!MoreElements.isType(possibleClazz)) {
        processingEnv.getMessager().printMessage(Kind.WARNING, "Ignoring element", possibleClazz);
        continue;
      }

      // Create visitors here.

    }
  }

  @VisibleForTesting
  static Predicate<ExecutableElement> hasAnnotation(final Class<? extends Annotation> annotation) {
    return new Predicate<ExecutableElement>() {

      @Override
      public boolean apply(@Nullable ExecutableElement input) {
        return isAnnotationPresent(input, annotation);
      }
    };
  }
}
