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
package com.google.j2cl.frontend;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.errors.Errors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Preprocesses Java files before compilation. This is currently used to remove declarations
 * marked with {@code GwtIncompatible}.
 *
 * TODO(stalcup): The preprocessing won't happen for files not included in the current files to
 * compile, but are just referenced. Those files will be pulled in when resolving bindings and
 * so won't go thorugh this preprocessor.
 */
public class JavaPreprocessor {

  /**
   * Preprocess every file and returns a map between the preprocessed file paths and the old file
   * paths. All the preprocessed files will be put in a temporary directory. If a file doesn't need
   * preprocessing, the key of the map will be the original location of the file.
   */
  public Map<String, String> preprocessFiles(
      List<String> filePaths, String encoding, Errors errors) {
    // Create a temporary directory to put all the files. We can't just create temporary files,
    // because the name of the file must match the class declared in the file (JLS 7.6).
    String temporaryDirectory;
    try {
      temporaryDirectory = Files.createTempDirectory("j2cl").toString();
    } catch (IOException e) {
      errors.error(Errors.Error.ERR_CANNOT_CREATE_TEMP_DIR);
      return null;
    }

    Map<String, String> tempToOriginalFileMap = new HashMap<>();
    for (String filePath : filePaths) {
      try {
        String preprocessedFileContent = preprocessFile(Paths.get(filePath), encoding);
        if (preprocessedFileContent != null) {
          // Write the preprocessed file to a temporary file.
          Path tempFilePath =
              Paths.get(temporaryDirectory, Paths.get(filePath).getFileName().toString());
          Files.write(tempFilePath, preprocessedFileContent.getBytes());
          tempToOriginalFileMap.put(tempFilePath.toString(), filePath);
        } else {
          // The file didn't need preprocessing, just point to the original file.
          tempToOriginalFileMap.put(filePath, filePath);
        }
      } catch (IOException e) {
        errors.error(Errors.Error.ERR_CANNOT_OPEN_FILE, filePath);
        return null;
      }
    }

    return tempToOriginalFileMap;
  }

  /**
   * Preprocesses the given file making any changes before compilation.
   * @return the new content of the file or null if the file doesn't need any preprocessing.
   */
  public String preprocessFile(Path filePath, @Nullable String encoding) throws IOException {
    byte[] bytes = Files.readAllBytes(filePath);
    return preprocessFile(encoding == null ? new String(bytes) : new String(bytes, encoding));
  }

  @VisibleForTesting
  String preprocessFile(String fileContent) {
    if (!fileContent.contains("GwtIncompatible")) {
      // If the file doesn't contain "GwtIncompatible", stop here to avoid useless parsing.
      return null;
    }
    // Parse the file.
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setResolveBindings(false);
    parser.setSource(fileContent.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

    // Find all the declarations with @GwtIncompatible.
    GwtIncompatibleNodeCollector gwtIncomaptibleVisitor = new GwtIncompatibleNodeCollector();
    compilationUnit.accept(gwtIncomaptibleVisitor);
    List<ASTNode> gwtIncompatibleNodes = gwtIncomaptibleVisitor.getNodes();
    if (gwtIncompatibleNodes.isEmpty()) {
      return null;
    }

    // Delete the gwtIncompatible nodes and gets all the imports that are no longer needed.
    for (ASTNode gwtIncompatibleNode : gwtIncompatibleNodes) {
      gwtIncompatibleNode.delete();
    }
    UnusedImportsNodeCollector unusedImportsNodeCollector = new UnusedImportsNodeCollector();
    compilationUnit.accept(unusedImportsNodeCollector);
    List<ImportDeclaration> unusedImportsNodes = unusedImportsNodeCollector.getUnusedImports();

    // Wrap all the not needed nodes inside comments in the original source
    // (so we can preserve line numbers and have accurate source maps).
    // Precondition: Node ranges must not overlap and they must be sorted by position.
    StringBuilder newFileContent = new StringBuilder();
    List<ASTNode> nodesToWrap = Lists.<ASTNode>newArrayList(unusedImportsNodes);
    nodesToWrap.addAll(gwtIncompatibleNodes);
    int currentPosition = 0;
    for (ASTNode nodeToWrap : nodesToWrap) {
      int startPosition = nodeToWrap.getStartPosition();
      int endPosition = startPosition + nodeToWrap.getLength();
      Preconditions.checkState(
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
