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
package com.google.j2cl.transpiler.frontend.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.ZipFiles;
import com.google.j2objc.annotations.ObjectiveCName;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import jsinterop.annotations.JsPackage;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 */
public class PackageInfoCache {

  /** Encapsulates all that is known about a particular package in a particular class path entry. */
  @AutoValue
  public abstract static class PackageReport {
    @Nullable
    public abstract String getJsNamespace();

    @Nullable
    public abstract String getObjectiveCName();

    public abstract boolean isNullMarked();

    public static Builder newBuilder() {
      return new AutoValue_PackageInfoCache_PackageReport.Builder().setNullMarked(false);
    }

    /** A Builder for PackageReport. */
    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setJsNamespace(String jsNamespace);

      public abstract Builder setObjectiveCName(String objectiveCName);

      public abstract Builder setNullMarked(boolean isNullMarked);

      abstract PackageReport autoBuild();

      public PackageReport build() {
        return autoBuild();
      }
    }
  }

  /**
   * When nothing is known about a particular package in a particular class path entry the answers
   * to questions about package properties are taken from this instance.
   */
  public static final PackageReport DEFAULT_PACKAGE_REPORT = PackageReport.newBuilder().build();

  /** Allows for the initialization/retrieval of one shared PackageInfoCache instance per thread. */
  private static final ThreadLocal<PackageInfoCache> packageInfoCacheStorage = new ThreadLocal<>();

  public static PackageInfoCache get() {
    return checkNotNull(packageInfoCacheStorage.get());
  }

  public static void init(List<String> classPathEntries, Problems problems) {
    checkState(
        packageInfoCacheStorage.get() == null,
        "PackageInfoCache should only be initialized once per thread.");

    packageInfoCacheStorage.set(new PackageInfoCache(classPathEntries, problems));
  }

  private final Map<String, PackageReport> packageReportByTypeName = new HashMap<>();
  private final Problems problems;

  private PackageInfoCache(List<String> classPathEntries, Problems problems) {
    this.problems = problems;
    indexPackageInfo(classPathEntries);
  }

  /**
   * Returns the JsNamespace for the given type, which must be a top level type and referenced by
   * fully qualified source name.
   */
  public String getJsNamespace(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).getJsNamespace();
  }

  /**
   * Returns the ObjectiveCName for the given type which must be a top level type and referenced by
   * its fully qualified source name.
   */
  public String getObjectiveCName(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).getObjectiveCName();
  }

  public boolean isNullMarked(String topLevelTypeSourceName) {
    return getPackageReport(topLevelTypeSourceName).isNullMarked();
  }

  public void setPackageProperties(
      String packagePath, String packageJsNamespace, String objectiveCName, boolean isNullMarked) {
    packageReportByTypeName.put(
        packagePath,
        PackageReport.newBuilder()
            .setJsNamespace(packageJsNamespace)
            .setObjectiveCName(objectiveCName)
            .setNullMarked(isNullMarked)
            .build());
  }

  private PackageReport getPackageReport(String packagePath) {
    return packageReportByTypeName.getOrDefault(packagePath, DEFAULT_PACKAGE_REPORT);
  }

  private void indexPackageInfo(List<String> classPathEntries) {
    for (String classPathEntry : classPathEntries) {
      try (ZipFile zipFile = new ZipFile(classPathEntry)) {
        for (ZipEntry entry : ZipFiles.entries(zipFile)) {
          if (entry.getName().endsWith("package-info.class")) {
            recordPackageInfo(zipFile.getInputStream(entry));
          }
        }
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
      }
    }
  }

  private void recordPackageInfo(InputStream packageInfoStream) {
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

    try {
      var reader = new ClassReader(packageInfoStream);
      reader.accept(visitor, ClassReader.SKIP_CODE);
      var packageName = reader.getClassName().replace("/package-info", "").replace('/', '.');
      packageReportByTypeName.put(
          packageName,
          PackageReport.newBuilder()
              .setJsNamespace(getAnnotation(annotations, JsPackage.class))
              .setObjectiveCName(getAnnotation(annotations, ObjectiveCName.class))
              .setNullMarked(getAnnotation(annotations, NullMarked.class) != null)
              .build());
    } catch (IOException e) {
      problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
    }
  }

  @Nullable
  private static String getAnnotation(Map<String, String> annotations, Class<?> annotationClass) {
    return annotations.get(annotationClass.getName());
  }
}
