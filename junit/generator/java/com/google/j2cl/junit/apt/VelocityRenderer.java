/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.junit.apt;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.escapevelocity.Template;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/** Renders Apache Velocity templates using templates from the JAR resources. */
final class VelocityRenderer {

  private final Class<?> clz;

  VelocityRenderer(Class<?> clz) {
    this.clz = clz;
  }

  public String renderTemplate(String templateName, Map<String, ?> context) throws IOException {
    Template template = Template.parseFrom(templateName, this::openResource);
    return template.evaluate(context);
  }

  private Reader openResource(String resourceName) throws IOException {
    URL resource = clz.getResource(resourceName);
    if (resource == null) {
      throw new FileNotFoundException("Could not find file: " + resourceName);
    }
    URLConnection connection = resource.openConnection();
    // Another annotation processor's classloader can close a cached JarFile while we read it.
    connection.setUseCaches(false);
    return new InputStreamReader(connection.getInputStream(), UTF_8);
  }
}
