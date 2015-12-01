/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import static com.google.j2cl.tools.jsni.JsniConverter.log;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Write the corresponding javascript code of {@link JsniMethod}s into several files and zip them.
 * <p/>
 * Each javascript file will be written into a directory inside the zip file corresponding to the
 * package name of the java class.
 * Ex: The corresponding javascript file of the class com.foo.Bar will written into
 * <code>com/foo</code> directory.
 */
public class NativeJsFilesWriter {
  private static final String STATIC_JS_METHOD =
      "{0}.{1} = function({2}) '{'\n    {0}.$clinit();{3}\n'}'\n\n";
  private static final String JS_METHOD = "{0}.prototype.{1} = function({2}) '{'{3}\n'}'\n\n";
  private static final String ZIP_PATH_SEPARATOR = "/";

  private final String zipFilefilePath;

  public NativeJsFilesWriter(String zipFilefilePath) {
    this.zipFilefilePath = zipFilefilePath;
  }

  public void write(Multimap<String, JsniMethod> jsniMethodsByTypeName) {
    try (ZipOutputStream zipOutputStream = createZipOutputStream(zipFilefilePath)) {

      for (String type : jsniMethodsByTypeName.keySet()) {
        int lastDot = type.lastIndexOf('.');
        String typeName = type.substring(lastDot + 1);
        String packageName = type.substring(0, lastDot);

        String content = buildContent(typeName, jsniMethodsByTypeName.get(type));

        Preconditions.checkState(
            !Strings.isNullOrEmpty(content),
            String.format("The content of the javascript file for the type %s is empty", type));

        // build js file name including directory.
        String directory = packageName.replaceAll("\\.", ZIP_PATH_SEPARATOR) + ZIP_PATH_SEPARATOR;
        String javascriptFilePath = directory + typeName + ".native.js";
        JsniConverter.log("content of " + javascriptFilePath + ":\n" + content);
        addTozipFile(zipOutputStream, javascriptFilePath, content);
      }

    } catch (IOException e) {
      throw new RuntimeException("Impossible to write into the zip file", e);
    }
  }

  private ZipOutputStream createZipOutputStream(String zipFilefilePath) {
    log("Creating %s", zipFilefilePath);

    try {
      return new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFilefilePath)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(
          String.format("Impossible to create the file %s.", zipFilefilePath), e);
    }
  }

  private String buildContent(String typeName, Collection<JsniMethod> jsniMethod) {
    StringBuilder contentBuilder = new StringBuilder();

    for (JsniMethod method : jsniMethod) {
      String template = method.isStatic() ? STATIC_JS_METHOD : JS_METHOD;
      String params = Joiner.on(", ").join(method.getParameterNames());

      contentBuilder.append(
          MessageFormat.format(
              template, typeName, method.getName(), params, stripEnd(method.getBody())));
    }

    return contentBuilder.toString();
  }

  private String stripEnd(String s) {
    int i = s.length() - 1;
    while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
      i--;
    }
    return s.substring(0, i + 1);
  }

  private void addTozipFile(ZipOutputStream zipOutputStream, String filePath, String content)
      throws IOException {
    log("Zipping %s...", filePath);

    ZipEntry sourceEntry = new ZipEntry(filePath);

    zipOutputStream.putNextEntry(sourceEntry);
    zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }
}
