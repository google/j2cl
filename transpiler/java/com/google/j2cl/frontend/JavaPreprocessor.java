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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.j2cl.errors.Errors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * Preprocesses Java files before compilation. This is currently used to remove declarations
 * marked with {@code GwtIncompatible}.
 *
 * TODO(stalcup): The preprocessing won't happen for files not included in the current files to
 * compile, but are just referenced. Those files will be pulled in when resolving bindings and
 * so won't go thorough this preprocessor.
 */
public class JavaPreprocessor {
  private Map<String, String> compilerOptions;

  public JavaPreprocessor(Map<String, String> compilerOptions) {
    this.compilerOptions = checkNotNull(compilerOptions);
  }

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

    // The map must be ordered because it will be iterated over later and if it was not ordered then
    // our output would be unstable.
    Map<String, String> tempToOriginalFileMap = new LinkedHashMap<>();

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
    String fileContent = encoding == null ? new String(bytes) : new String(bytes, encoding);
    String preprocessedFileContent = preprocessFile(fileContent);
    return fileContent.equals(preprocessedFileContent) ? null : preprocessedFileContent;
  }

  @VisibleForTesting
  String preprocessFile(String fileContent) {
    return commentGwtIncompatibleNodes(fileContent);
  }

  /**
   * Comments out all the nodes (fields, methods, classes) marked with {@code GwtIncompatible}.
   */
  String commentGwtIncompatibleNodes(String fileContent) {
    if (!fileContent.contains("GwtIncompatible")) {
      return fileContent;
    }

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
    List<ASTNode> nodesToWrap = Lists.<ASTNode>newArrayList(unusedImportsNodes);
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
