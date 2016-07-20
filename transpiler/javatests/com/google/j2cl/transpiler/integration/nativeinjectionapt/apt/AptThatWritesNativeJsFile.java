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
package com.google.j2cl.transpiler.integration.nativeinjectionapt.apt;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

/**
 * A simple APT that writes a native.js file for the class its processing.
 *
 * <p>To keep the APT simple it will only write public static methods.
 */
@AutoService(Processor.class)
public class AptThatWritesNativeJsFile extends BasicAnnotationProcessor {

  private class MyStep implements ProcessingStep {

    private static final String NEWLINE = "\n";

    @SuppressWarnings("unchecked")
    @Override
    public Set<? extends Class<? extends Annotation>> annotations() {
      return Sets.newHashSet(RunApt.class);
    }

    @Override
    public Set<Element> process(
        SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {

      for (Element value : elementsByAnnotation.get(RunApt.class)) {
        if (!MoreElements.isType(value)) {
          continue;
        }

        TypeElement typeElement = MoreElements.asType(value);
        ImmutableList<String> methodNames =
            FluentIterable.from(ElementFilter.methodsIn(typeElement.getEnclosedElements()))
                .transform(
                    new Function<ExecutableElement, String>() {
                      @Override
                      public String apply(ExecutableElement input) {
                        return input.getSimpleName().toString();
                      }
                    })
                .toList();

        String packageName = MoreElements.getPackage(typeElement).getQualifiedName().toString();
        String className = typeElement.getSimpleName().toString();
        writeNativeJsFile(packageName, className, methodNames);
      }

      return ImmutableSet.of();
    }

    private void writeNativeJsFile(
        String packageName, String className, ImmutableList<String> methodNames) {
      try (Writer writer =
          processingEnv
              .getFiler()
              .createResource(
                  StandardLocation.CLASS_OUTPUT,
                  packageName,
                  className + ".native.js",
                  new Element[0])
              .openWriter(); ) {

        for (String methodName : methodNames) {
          writer.write(String.format("%s.m_%s__ = function() {", className, methodName));
          writer.write(NEWLINE);
          writer.write(String.format("  return \"%s\";", methodName));
          writer.write(NEWLINE);
          writer.write("};");
          writer.write(NEWLINE);
          writer.write(NEWLINE);
        }
      } catch (IOException e) {
        processingEnv.getMessager().printMessage(Kind.ERROR, "Unable to write suite file.");
        return;
      }
    }
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    return Lists.newArrayList(new MyStep());
  }
}
