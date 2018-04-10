/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.tools.gwtincompatible;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StripperTest {

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  private File outputSourceJar;
  private PrintStream printStream;
  private ByteArrayOutputStream byteArrayOutputStream;

  @Before
  public void before() throws UnsupportedEncodingException {
    outputSourceJar = new File(tmpFolder.getRoot(), "output.srcjar");

    byteArrayOutputStream = new ByteArrayOutputStream();
    printStream = new PrintStream(byteArrayOutputStream, true, "UTF-8");
  }

  @Test
  public void testEmptySources() throws UnsupportedEncodingException {
    new Stripper().run(new String[] {"-d", outputSourceJar.getAbsolutePath()}, printStream);
    assertThat(new String(byteArrayOutputStream.toByteArray(), "UTF-8")).isEmpty();
  }

  @Test
  public void testSourceJar() throws IOException {
    File sourceJar = tmpFolder.newFile("input.srcjar");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(sourceJar));

    ZipEntry e = new ZipEntry("foo/bar/Baz.java");
    out.putNextEntry(e);
    byte[] data = "// Baz.java\n".getBytes("UTF-8");
    out.write(data, 0, data.length);
    out.closeEntry();
    out.close();

    new Stripper()
        .run(
            new String[] {"-d", outputSourceJar.getAbsolutePath(), sourceJar.getAbsolutePath()},
            printStream);
    assertThat(new String(byteArrayOutputStream.toByteArray(), "UTF-8")).isEmpty();

    FileInputStream input = new FileInputStream(outputSourceJar);
    ZipInputStream zip = new ZipInputStream(input);

    ZipEntry entry = zip.getNextEntry();
    assertThat(entry).isNotNull();
    assertThat(entry.getName()).isEqualTo("foo/");
    assertThat(entry.getLastModifiedTime()).isEqualTo(FileTime.fromMillis(0));

    entry = zip.getNextEntry();
    assertThat(entry).isNotNull();
    assertThat(entry.getName()).isEqualTo("foo/bar/");
    assertThat(entry.getLastModifiedTime()).isEqualTo(FileTime.fromMillis(0));

    entry = zip.getNextEntry();
    assertThat(entry).isNotNull();
    assertThat(entry.getName()).isEqualTo("foo/bar/Baz.java");
    assertThat(entry.getLastModifiedTime()).isEqualTo(FileTime.fromMillis(0));

    assertThat(zip.getNextEntry()).isNull();
  }
}
