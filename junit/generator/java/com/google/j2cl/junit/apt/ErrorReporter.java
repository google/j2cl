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
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

class ErrorReporter {

  private final Messager messager;

  public ErrorReporter(Messager messager) {
    this.messager = messager;
  }

  public void report(ErrorMessage errorMessage, Object... args) {
    messager.printMessage(errorMessage.kind(), errorMessage.format(args));
  }

  public void report(ErrorMessage errorMessage, TypeElement type) {
    report(errorMessage, type.getQualifiedName());
  }

  public void report(ErrorMessage errorMessage, Element method) {
    report(errorMessage, getQualifiedName(method));
  }

  private String getQualifiedName(Element element) {
    Name clazzName = MoreElements.asType(element.getEnclosingElement()).getQualifiedName();
    return clazzName + "." + element.getSimpleName();
  }
}
