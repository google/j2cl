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
