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

import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.ZipFiles;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 */
public class PackageInfoCache {

  private final Map<String, PackageInfo> packageReportByTypeName = new HashMap<>();
  private final Map<String, Manifest> manifestByPath = new HashMap<>();
  private final Problems problems;

  public PackageInfoCache(List<String> classPathEntries, Problems problems) {
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

  public Manifest getManifest(String path) {
    return manifestByPath.get(path);
  }

  public void setPackageProperties(
      String packageName, String packageJsNamespace, String objectiveCName, boolean isNullMarked) {
    packageReportByTypeName.put(
        packageName,
        PackageInfo.newBuilder()
            .setPackageName(packageName)
            .setJsNamespace(packageJsNamespace)
            .setObjectiveCName(objectiveCName)
            .setNullMarked(isNullMarked)
            .build());
  }

  private PackageInfo getPackageReport(String packagePath) {
    return packageReportByTypeName.getOrDefault(packagePath, PackageInfo.DEFAULT);
  }

  private void indexPackageInfo(List<String> classPathEntries) {
    for (String classPathEntry : classPathEntries) {
      try (ZipFile zipFile = new ZipFile(classPathEntry)) {
        for (ZipEntry entry : ZipFiles.entries(zipFile)) {
          if (entry.getName().endsWith("package-info.class")) {
            var packageInfo = PackageInfo.read(zipFile.getInputStream(entry));
            packageReportByTypeName.put(packageInfo.getPackageName(), packageInfo);
          }
          if (entry.getName().equals(JarFile.MANIFEST_NAME)) {
            manifestByPath.put(classPathEntry, new Manifest(zipFile.getInputStream(entry)));
          }
        }
        problems.abortIfCancelled();
      } catch (ZipException e) {
        problems.fatal(FatalError.CANNOT_EXTRACT_ZIP, classPathEntry, e.getMessage());
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
      }
    }
  }
}
