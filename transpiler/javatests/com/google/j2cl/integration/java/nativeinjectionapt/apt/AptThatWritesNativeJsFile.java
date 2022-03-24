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
package nativeinjectionapt.apt;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

/** A simple APT that writes a native JS files for the class its processing. */
@AutoService(Processor.class)
public class AptThatWritesNativeJsFile extends BasicAnnotationProcessor {

  private class MyStep implements ProcessingStep {

    @Override
    public Set<? extends Class<? extends Annotation>> annotations() {
      return Sets.newHashSet(RunApt.class);
    }

    @Override
    public Set<Element> process(
        SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
      for (Element value : elementsByAnnotation.get(RunApt.class)) {
        TypeElement typeElement = MoreElements.asType(value);
        String packageName = MoreElements.getPackage(typeElement).getQualifiedName().toString();
        writeNativeJsFile(packageName, typeElement.getSimpleName().toString());
      }
      writeJsFile("nativeinjectionapt", "NativeClass");
      return ImmutableSet.of();
    }

    private void writeNativeJsFile(String packageName, String className) {
      try (Writer writer = createResource(packageName, className + ".native.js")) {
        writeln(writer, "%s.nativeStaticMethod = function() {", className);
        writeln(writer, "  return '%s';", className);
        writeln(writer, "};");
      } catch (IOException e) {
        processingEnv
            .getMessager()
            .printMessage(Kind.ERROR, "Unable to write file: " + e.getMessage());
        return;
      }
    }

    private void writeJsFile(String packageName, String className) {
      try (Writer writer = createResource(packageName, className + ".js")) {
        writeln(writer, "goog.module('%s.%s');", packageName, className);
        writeln(writer, "class %s {", className);
        writeln(writer, "  static nativeStaticMethod() { return '%s'; }", className);
        writeln(writer, "};");
        writeln(writer, "exports=%s;", className);
      } catch (IOException e) {
        processingEnv
            .getMessager()
            .printMessage(Kind.ERROR, "Unable to write file: " + e.getMessage());
        return;
      }
    }

    private Writer createResource(String packageName, String resourceName) throws IOException {
      return processingEnv
          .getFiler()
          .createResource(StandardLocation.SOURCE_OUTPUT, packageName, resourceName)
          .openWriter();
    }

    private void writeln(Writer writer, String format, Object... args) throws IOException {
      writer.write(String.format(format, args) + "\n");
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
