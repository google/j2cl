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

import com.google.auto.common.MoreElements;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

class BaseValidator {
  protected final ErrorReporter errorReporter;

  public BaseValidator(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  protected final boolean validateNotInnerClass(TypeElement typeElement) {
    if (typeElement.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
      errorReporter.report(ErrorMessage.NON_TOP_LEVEL_TYPE, typeElement);
      return false;
    }
    return true;
  }

  protected final boolean validateMemberIsPublic(Element element) {
    if (!MoreElements.hasModifiers(Modifier.PUBLIC).apply(element)) {
      errorReporter.report(ErrorMessage.NON_PUBLIC, element);
      return false;
    }
    return true;
  }

  protected final boolean validateMemberIsNonFinal(Element element) {
    if (MoreElements.hasModifiers(Modifier.FINAL).apply(element)) {
      errorReporter.report(ErrorMessage.IS_FINAL, element);
      return false;
    }
    return true;
  }

  protected final boolean validateMethodNoArguments(ExecutableElement executableElement) {
    if (!TestingPredicates.ZERO_PARAMETER_PREDICATE.apply(executableElement)) {
      errorReporter.report(ErrorMessage.HAS_ARGS, executableElement);
      return false;
    }
    return true;
  }
}
