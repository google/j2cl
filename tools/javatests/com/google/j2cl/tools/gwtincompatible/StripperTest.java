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

import com.google.common.io.Files;
import com.google.j2cl.common.Problems;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StripperTest {

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Test
  public void testEmptySources() throws IOException {
    File sourceJar = new File(tmpFolder.newFolder(), "input.srcjar");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(sourceJar));
    out.close();

    File outputSourceJar = runStripper(sourceJar);
    ZipInputStream zip = new ZipInputStream(new FileInputStream(outputSourceJar));
    assertThat(zip.getNextEntry()).isNull();
  }

  @Test
  public void testSourceJar() throws IOException {
    File sourceJar = createSourceJar();

    File outputSourceJar = runStripper(sourceJar);
    ZipInputStream zip = new ZipInputStream(new FileInputStream(outputSourceJar));

    ZipEntry entry = zip.getNextEntry();
    assertThat(entry.getName()).isEqualTo("foo/");
    asserTimeStampWasReset(entry);

    entry = zip.getNextEntry();
    assertThat(entry.getName()).isEqualTo("foo/bar/");
    asserTimeStampWasReset(entry);

    entry = zip.getNextEntry();
    assertThat(entry.getName()).isEqualTo("foo/bar/Baz.java");
    asserTimeStampWasReset(entry);

    assertThat(zip.getNextEntry()).isNull();
  }

  @Test
  public void runTwiceWithSameInput() throws IOException {
    File sourceJar1 = createSourceJar();
    File sourceJar2 = createSourceJar();

    File outputSourceJar = runStripper(sourceJar1);
    File outputSourceJar2 = runStripper(sourceJar2);

    assertThat(Files.toByteArray(outputSourceJar)).isEqualTo(Files.toByteArray(outputSourceJar2));
  }

  private File createSourceJar() throws IOException {
    File sourceJar = new File(tmpFolder.newFolder(), "input.srcjar");
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(sourceJar));

    ZipEntry e = new ZipEntry("foo/bar/Baz.java");
    out.putNextEntry(e);
    byte[] data = "// Baz.java\n".getBytes("UTF-8");
    out.write(data, 0, data.length);
    out.closeEntry();
    out.close();
    return sourceJar;
  }

  private File runStripper(File inputJar) throws IOException {
    File outputJar = new File(tmpFolder.newFolder(), "output.srcjar");
    Problems problems =
        Stripper.strip(
            new String[] {"-d", outputJar.getAbsolutePath(), inputJar.getAbsolutePath()});
    assertThat(problems.hasErrors()).isFalse();
    return outputJar;
  }

  private static void asserTimeStampWasReset(ZipEntry entry) {
    assertThat(entry.getLastModifiedTime()).isEqualTo(FileTime.fromMillis(0));
    assertThat(entry.getCreationTime()).isEqualTo(FileTime.fromMillis(0));
    assertThat(entry.getLastAccessTime()).isEqualTo(FileTime.fromMillis(0));
  }
}
