/*
 * Copyright 2017 Google Inc.
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
package com.google.j2cl.ast.processors;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Utility methods for using Apache Velocity.
 */
class VelocityUtil {
  private static final String CLASSPATH_RESOURCE_LOADER_CLASS = "classpath.resource.loader.class";

  /**
   * Creates and returns a VelocityEngine that will find templates on the
   * classpath.
   */
  public static VelocityEngine createEngine() {
    VelocityEngine velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty(
        CLASSPATH_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
    velocityEngine.init();
    return velocityEngine;
  }
}
