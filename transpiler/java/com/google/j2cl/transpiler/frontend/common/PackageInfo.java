/*
 * Copyright 2025 Google Inc.
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
package com.google.j2cl.transpiler.frontend.common;

import com.google.auto.value.AutoValue;
import com.google.j2objc.annotations.ObjectiveCName;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.annotation.Nullable;
import jsinterop.annotations.JsPackage;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

/** Encapsulates all that is known about a particular package via its package-info. */
@AutoValue
public abstract class PackageInfo {

  /**
   * When nothing is known about a particular package in a particular class path entry the answers
   * to questions about package properties are taken from this instance.
   */
  public static final PackageInfo DEFAULT = PackageInfo.newBuilder().setPackageName("").build();

  public static PackageInfo read(InputStream packageInfoStream) throws IOException {
    var annotations = new HashMap<String, String>();
    // Prefill with known annotations so we can use it to avoid traversing unrelated annotations.
    annotations.put(JsPackage.class.getName(), null);
    annotations.put(ObjectiveCName.class.getName(), null);
    annotations.put(NullMarked.class.getName(), null);

    final int opcode = org.objectweb.asm.Opcodes.ASM9;
    var visitor =
        new ClassVisitor(opcode) {
          @Override
          @Nullable
          public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            String annotationName = Type.getType(descriptor).getClassName();
            if (!annotations.containsKey(annotationName)) {
              return null; // Unknown annotation, stop traversal.
            }

            // Annotation is present: initialize with empty value (might be overridden in visitor).
            annotations.put(annotationName, "");
            return new AnnotationVisitor(opcode) {
              @Override
              public void visit(String name, Object value) {
                annotations.put(annotationName, value.toString());
              }
            };
          }
        };

    var reader = new ClassReader(packageInfoStream);
    reader.accept(visitor, ClassReader.SKIP_CODE);
    var packageName = reader.getClassName().replace("/package-info", "").replace('/', '.');
    return PackageInfo.newBuilder()
        .setPackageName(packageName)
        .setJsNamespace(annotations.get(JsPackage.class.getName()))
        .setObjectiveCName(annotations.get(ObjectiveCName.class.getName()))
        .setNullMarked(annotations.get(NullMarked.class.getName()) != null)
        .build();
  }

  public abstract String getPackageName();

  @Nullable
  public abstract String getJsNamespace();

  @Nullable
  public abstract String getObjectiveCName();

  public abstract boolean isNullMarked();

  public static Builder newBuilder() {
    return new AutoValue_PackageInfo.Builder().setNullMarked(false);
  }

  /** A Builder for PackageReport. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPackageName(String packageName);

    public abstract Builder setJsNamespace(String jsNamespace);

    public abstract Builder setObjectiveCName(String objectiveCName);

    public abstract Builder setNullMarked(boolean isNullMarked);

    abstract PackageInfo build();
  }
}
