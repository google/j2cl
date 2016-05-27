package com.google.j2cl.generator;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.j2cl.errors.Errors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * NativeJavaScriptFile contains information about native javascript files that is used to output
 * native code during the javascript generation stage.
 */
public class NativeJavaScriptFile {
  private String path;
  private String content;
  private boolean used = false;
  private String zipPath;

  public static final String NATIVE_EXTENSION = ".native.js";

  public NativeJavaScriptFile(String path, String content, String zipPath) {
    this.path = path;
    this.content = content;
    this.zipPath = zipPath;
  }

  public String getPathWithoutExtension() {
    return path.substring(0, path.lastIndexOf(NATIVE_EXTENSION));
  }

  public String getContent() {
    return content;
  }

  public String getZipPath() {
    return zipPath;
  }

  /**
   * Can only set to used.
   */
  public void setUsed() {
    used = true;
  }

  public boolean wasUsed() {
    return used;
  }

  /**
   * Given a list of zip file paths, this method will extract files with the extension
   * @NATIVE_EXTENSION and return the a map of file paths to NativeJavaScriptFile objects of the
   * form:
   *
   * <p>/com/google/example/nativejsfile1 => NativeJavaScriptFile
   *
   * <p>/com/google/example/nativejsfile2 => NativeJavaScriptFile
   */
  public static Map<String, NativeJavaScriptFile> getFilesByPathFromZip(
      List<String> zipPaths, String charSet, Errors errors) {
    Map<String, NativeJavaScriptFile> loadedFilesByPath = new LinkedHashMap<>();
    for (String zipPath : zipPaths) {
      try (ZipFile zipFile = new ZipFile(zipPath)) {
        List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
        for (ZipEntry entry : entries) {
          if (!entry.getName().endsWith(NATIVE_EXTENSION)) {
            continue; // If the path isn't of type NATIVE_EXTENSION, don't add it.
          }
          InputStream stream = zipFile.getInputStream(entry);
          String content = CharStreams.toString(new InputStreamReader(stream, charSet));
          Closeables.closeQuietly(stream);
          NativeJavaScriptFile file = new NativeJavaScriptFile(entry.getName(), content, zipPath);
          loadedFilesByPath.put(file.getPathWithoutExtension(), file);
        }
      } catch (IOException e) {
        errors.error(Errors.Error.ERR_CANNOT_OPEN_ZIP, zipPath);
      }
    }
    return loadedFilesByPath;
  }
}
