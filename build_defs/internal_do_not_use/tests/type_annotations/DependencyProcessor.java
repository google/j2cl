/*
 * Copyright 2026 Google LLC
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
package example;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("*")
public class DependencyProcessor extends AbstractProcessor {
  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (!round.processingOver()) {
      TypeElement dependency = processingEnv.getElementUtils().getTypeElement("example.Dependency");
      var field = ElementFilter.fieldsIn(dependency.getEnclosedElements()).get(0);
      if (field.asType().getAnnotationMirrors().isEmpty()) {
        processingEnv
            .getMessager()
            .printMessage(Diagnostic.Kind.ERROR, "Dependency type annotations were not loaded");
      }
    }
    return false;
  }
}
