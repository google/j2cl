/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.j2cl.frontend.common.GwtIncompatibleNodeCollector;
import com.google.j2cl.problems.Problems;
import com.google.j2cl.problems.Problems.Message;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * Preprocesses Java files before compilation. This is currently used to remove declarations
 * marked with {@code GwtIncompatible}.
 */
public class JavaPreprocessor {
  /**
   * Preprocess every file and returns a map between the preprocessed file paths and the old file
   * paths. All the preprocessed files will be put in a temporary directory. If a file doesn't need
   * preprocessing, the key of the map will be the original location of the file.
   */
  public static void preprocessFiles(
      List<String> filePaths,
      FileSystem outputZipFileSystem,
      String encoding,
      Problems problems,
      String sourceVersion) {
    for (String filePath : filePaths) {
      try {
        String preprocessedFileContent =
            preprocessFile(Paths.get(filePath), encoding, sourceVersion);

        // Write the preprocessed file to the zip file
        Path entryOutputPath = outputZipFileSystem.getPath(filePath);

        // Write using the provided fileSystem (which might be the regular file system or might be a
        // zip file.)
        if (entryOutputPath.getParent() != null) {
          Files.createDirectories(entryOutputPath.getParent());
        }
        Files.write(entryOutputPath, preprocessedFileContent.getBytes(Charset.defaultCharset()));

        // Wipe entries modification time so that input->output mapping is stable
        // regardless of the time of day.
        Files.setLastModifiedTime(entryOutputPath, FileTime.fromMillis(0));
      } catch (IOException e) {
        problems.error(Message.ERR_CANNOT_OPEN_FILE, filePath, e.getMessage());
        return;
      }
    }
  }

  /**
   * Preprocesses the given file making any changes before compilation.
   *
   * @return the new content of the file or null if the file doesn't need any preprocessing.
   */
  public static String preprocessFile(
      Path filePath, @Nullable String encoding, String sourceVersion) throws IOException {
    byte[] bytes = Files.readAllBytes(filePath);
    String fileContent =
        encoding == null ? new String(bytes, StandardCharsets.UTF_8) : new String(bytes, encoding);
    String preprocessedFileContent = preprocessFile(fileContent, sourceVersion);
    return fileContent.equals(preprocessedFileContent) ? fileContent : preprocessedFileContent;
  }

  @VisibleForTesting
  static String preprocessFile(String fileContent, String sourceVersion) {
    if (!fileContent.contains("GwtIncompatible")) {
      return fileContent;
    }

    Map<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(JavaCore.COMPILER_SOURCE, sourceVersion);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, sourceVersion);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, sourceVersion);

    // Parse the file.
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setCompilerOptions(compilerOptions);
    parser.setResolveBindings(false);
    parser.setSource(fileContent.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

    // Find all the declarations with @GwtIncompatible.
    GwtIncompatibleNodeCollector gwtIncomaptibleVisitor = new GwtIncompatibleNodeCollector();
    compilationUnit.accept(gwtIncomaptibleVisitor);
    List<ASTNode> gwtIncompatibleNodes = gwtIncomaptibleVisitor.getNodes();

    // Delete the gwtIncompatible nodes.
    for (ASTNode gwtIncompatibleNode : gwtIncompatibleNodes) {
      gwtIncompatibleNode.delete();
    }

    // Gets all the imports that are no longer needed.
    UnusedImportsNodeCollector unusedImportsNodeCollector = new UnusedImportsNodeCollector();
    compilationUnit.accept(unusedImportsNodeCollector);
    List<ImportDeclaration> unusedImportsNodes = unusedImportsNodeCollector.getUnusedImports();

    // Wrap all the not needed nodes inside comments in the original source
    // (so we can preserve line numbers and have accurate source maps).
    List<ASTNode> nodesToWrap = Lists.newArrayList(unusedImportsNodes);
    nodesToWrap.addAll(gwtIncompatibleNodes);
    if (nodesToWrap.isEmpty()) {
      // Nothing was changed.
      return fileContent;
    }
    // Precondition: Node ranges must not overlap and they must be sorted by position.
    StringBuilder newFileContent = new StringBuilder();
    int currentPosition = 0;
    for (ASTNode nodeToWrap : nodesToWrap) {
      int startPosition = nodeToWrap.getStartPosition();
      int endPosition = startPosition + nodeToWrap.getLength();
      checkState(
          currentPosition <= startPosition,
          "Unexpected node position: %s, must be >= %s",
          startPosition,
          currentPosition);

      newFileContent.append(fileContent.substring(currentPosition, startPosition));
      newFileContent.append("/*");
      // Replace */ to ** since Java doesn't support nested comments.
      newFileContent.append(fileContent.substring(startPosition, endPosition).replace("*/", "**"));
      newFileContent.append("*/");
      currentPosition = endPosition;
    }
    newFileContent.append(fileContent.substring(currentPosition));
    return newFileContent.toString();
  }
}
