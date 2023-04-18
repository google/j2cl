/*
 * Copyright 2017 Google Inc.
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

package com.google.j2cl.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.j2cl.common.SourceUtils.FileInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Small fill in for io.ZipFiles since it's not open source yet. */
final class ZipFiles {

  private static final class ZipEntryByteSource extends ByteSource {

    private final ZipFile file;
    private final ZipEntry entry;

    ZipEntryByteSource(ZipFile file, ZipEntry entry) {
      this.file = checkNotNull(file);
      this.entry = checkNotNull(entry);
    }

    @Override
    public InputStream openStream() throws IOException {
      return file.getInputStream(entry);
    }

    @Override
    public String toString() {
      return "ZipFiles.asByteSource(" + file + ", " + entry + ")";
    }
  }

  public static ImmutableList<FileInfo> unzipFile(File zipFile, File targetDirectory)
      throws IOException {
    checkNotNull(zipFile);
    checkNotNull(targetDirectory);
    checkArgument(
        targetDirectory.isDirectory(),
        "%s is not a valid directory",
        targetDirectory.getAbsolutePath());
    ImmutableList.Builder<FileInfo> results = new ImmutableList.Builder<>();
    final ZipFile zipFileObj = new ZipFile(zipFile);
    try {
      for (ZipEntry entry : entries(zipFileObj)) {
        checkName(entry.getName());
        File targetFile = new File(targetDirectory, entry.getName());
        if (entry.isDirectory()) {
          if (!targetFile.isDirectory() && !targetFile.mkdirs()) {
            throw new IOException("Failed to create directory: " + targetFile.getAbsolutePath());
          }
        } else {
          File parentFile = targetFile.getParentFile();
          if (!parentFile.isDirectory()) {
            if (!parentFile.mkdirs()) {
              throw new IOException("Failed to create directory: " + parentFile.getAbsolutePath());
            }
          }
          // Write the file to the destination.
          asByteSource(zipFileObj, entry).copyTo(Files.asByteSink(targetFile));
          results.add(FileInfo.create(targetFile.toString(), entry.getName()));
        }
      }
    } finally {
      zipFileObj.close();
    }
    return results.build();
  }

  /**
   * Returns a new {@link ByteSource} for reading the contents of the given entry in the given zip
   * file.
   */
  private static ByteSource asByteSource(ZipFile file, ZipEntry entry) {
    return new ZipEntryByteSource(file, entry);
  }

  /** Returns a {@link FluentIterable} of all the entries in the given zip file. */
  // unmodifiable Iterator<? extends ZipEntry> can be safely cast
  // to Iterator<ZipEntry>
  @SuppressWarnings("unchecked")
  public static FluentIterable<ZipEntry> entries(final ZipFile file) {
    checkNotNull(file);
    return new FluentIterable<ZipEntry>() {
      @Override
      public Iterator<ZipEntry> iterator() {
        return (Iterator<ZipEntry>) Iterators.forEnumeration(file.entries());
      }
    };
  }

  /**
   * Checks that the given entry name is legal for unzipping: if it contains ".." as a name element,
   * it could cause the entry to be unzipped outside the directory we're unzipping to.
   *
   * @throws IOException if the name is illegal
   */
  private static void checkName(String name) throws IOException {
    // First just check whether the entry name string contains "..".
    // This should weed out the the vast majority of entries, which will not
    // contain "..".
    if (name.contains("..")) {
      // If the string does contain "..", break it down into its actual name
      // elements to ensure it actually contains ".." as a name, not just a
      // name like "foo..bar" or even "foo..", which should be fine.
      File file = new File(name);
      while (file != null) {
        if (file.getName().equals("..")) {
          throw new IOException(
              "Cannot unzip file containing an entry with " + "\"..\" in the name: " + name);
        }
        file = file.getParentFile();
      }
    }
  }

  private ZipFiles() {}
}
