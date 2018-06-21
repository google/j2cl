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
import com.google.common.io.MoreFiles;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.FrontendUtils.FileInfo;
import com.google.j2cl.frontend.GwtIncompatibleNodeCollector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * Preprocesses Java files before compilation. This is currently used to remove declarations marked
 * with {@code GwtIncompatible}.
 */
public class JavaPreprocessor {
  /**
   * Preprocess every file and returns a map between the preprocessed file paths and the old file
   * paths. All the preprocessed files will be put in a temporary directory. If a file doesn't need
   * preprocessing, the key of the map will be the original location of the file.
   */
  public static void preprocessFiles(
      List<FileInfo> fileInfos, FileSystem outputFileSystem, Problems problems) {
    for (FileInfo fileInfo : fileInfos) {
      String processedFileContent;
      try {
        String fileContent =
            MoreFiles.asCharSource(Paths.get(fileInfo.sourcePath()), StandardCharsets.UTF_8).read();
        processedFileContent = processFile(fileContent);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.toString());
        return;
      }

      // Write the processed file to output
      J2clUtils.writeToFile(
          outputFileSystem.getPath(fileInfo.targetPath()), processedFileContent, problems);
    }
  }

  @VisibleForTesting
  static String processFile(String fileContent) {
    // Avoid parsing if there are no textual references to GwtIncompatible.
    if (!fileContent.contains("GwtIncompatible")) {
      return fileContent;
    }

    Map<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_9);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_9);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_9);

    // Parse the file.
    ASTParser parser = ASTParser.newParser(AST.JLS9);
    parser.setCompilerOptions(compilerOptions);
    parser.setResolveBindings(false);
    parser.setSource(fileContent.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

    // Find all the declarations with @GwtIncompatible.
    GwtIncompatibleNodeCollector gwtIncompatibleVisitor = new GwtIncompatibleNodeCollector();
    compilationUnit.accept(gwtIncompatibleVisitor);
    List<ASTNode> gwtIncompatibleNodes = gwtIncompatibleVisitor.getNodes();

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

      StringBuilder strippedCodeBuilder = new StringBuilder();
      for (char c : fileContent.substring(startPosition, endPosition).toCharArray()) {
        strippedCodeBuilder.append(Character.isWhitespace(c) ? c : ' ');
      }
      newFileContent.append(strippedCodeBuilder);
      currentPosition = endPosition;
    }
    newFileContent.append(fileContent.substring(currentPosition));

    return newFileContent.toString();
  }
}
