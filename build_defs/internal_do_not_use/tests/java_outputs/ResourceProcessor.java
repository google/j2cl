/*
 * Copyright 2026 Google LLC
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
package example;

import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("*")
public class ResourceProcessor extends AbstractProcessor {
  private boolean generated;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (generated || round.processingOver()) {
      return false;
    }
    generated = true;
    try {
      try (var writer =
          processingEnv.getFiler().createSourceFile("example.GeneratedBinding").openWriter()) {
        writer.write(
            "package example; public class GeneratedBinding {"
                + " public static String resourceName() { return \"example/style.css\"; } }");
      }
      resource(StandardLocation.CLASS_OUTPUT, "example/style.css", "body { color: red; }\n");
      resource(
          StandardLocation.CLASS_OUTPUT,
          "example/metadata.json",
          "{\"binding\":\"example.GeneratedBinding\"}");
      resource(StandardLocation.CLASS_OUTPUT, "assets/bootstrap.js", "// resource JavaScript\n");
      resource(
          StandardLocation.CLASS_OUTPUT,
          "assets/bootstrap.native.js",
          "// resource native JavaScript\n");
      resource(StandardLocation.SOURCE_OUTPUT, "example/generated.js", "// generated JavaScript\n");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private void resource(StandardLocation location, String name, String content) throws IOException {
    try (var writer = processingEnv.getFiler().createResource(location, "", name).openWriter()) {
      writer.write(content);
    }
  }
}
