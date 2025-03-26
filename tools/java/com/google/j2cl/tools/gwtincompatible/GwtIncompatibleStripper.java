/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.gwtincompatible;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import com.google.j2cl.common.OutputUtils;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.frontend.jdt.AnnotatedNodeCollector;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * A helper to comment out source code elements annotated with "incompatible" annotations
 * (e.g. @GwtIncompatible) so that they are ignored by tools taking that source as input such as the
 * java compile or the j2cl transpile.
 */
public final class GwtIncompatibleStripper {

  static void strip(
      Stream<String> files, Path outputPath, Problems problems, List<String> annotationNames) {
    try (Output out = OutputUtils.initOutput(outputPath, problems)) {
      List<FileInfo> allPaths =
          SourceUtils.getAllSources(files, problems)
              .filter(f -> f.targetPath().endsWith(".java"))
              .collect(toImmutableList());
      preprocessFiles(allPaths, out, problems, annotationNames);
    }
  }

  /** Preprocess all provided files and put them to provided output path. */
  private static void preprocessFiles(
      List<FileInfo> fileInfos, Output output, Problems problems, List<String> annotationNames) {
    for (FileInfo fileInfo : fileInfos) {
      String processedFileContent;
      try {
        String fileContent = MoreFiles.asCharSource(Paths.get(fileInfo.sourcePath()), UTF_8).read();
        processedFileContent = strip(fileContent, annotationNames);
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_OPEN_FILE, e.getMessage());
        return;
      }

      // Write the processed file to output
      output.write(fileInfo.originalPath(), processedFileContent);
    }
  }

  public static String strip(String fileContent, List<String> annotationNames) {
    // Avoid parsing if there are no textual references to the annotation name(s).
    if (annotationNames.stream().noneMatch(fileContent::contains)) {
      return fileContent;
    }

    Map<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_14);
    compilerOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_14);
    compilerOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_14);

    // Parse the file.
    ASTParser parser = ASTParser.newParser(AST.JLS14);
    parser.setCompilerOptions(compilerOptions);
    parser.setResolveBindings(false);
    parser.setSource(fileContent.toCharArray());
    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

    // Find all the declarations with the annotation name
    Set<ASTNode> nodesToRemove = new HashSet<>();
    for (String annotationName : annotationNames) {
      AnnotatedNodeCollector gwtIncompatibleVisitor = new AnnotatedNodeCollector(annotationName);
      compilationUnit.accept(gwtIncompatibleVisitor);
      nodesToRemove.addAll(gwtIncompatibleVisitor.getNodes());
    }

    // Delete the gwtIncompatible nodes.
    for (ASTNode gwtIncompatibleNode : nodesToRemove) {
      gwtIncompatibleNode.delete();
    }

    // Gets all the imports that are no longer needed.
    UnusedImportsNodeCollector unusedImportsNodeCollector = new UnusedImportsNodeCollector();
    compilationUnit.accept(unusedImportsNodeCollector);
    List<ImportDeclaration> unusedImportsNodes = unusedImportsNodeCollector.getUnusedImports();

    // Wrap all the not needed nodes inside comments in the original source
    // (so we can preserve line numbers and have accurate source maps).
    List<ASTNode> nodesToWrap = Lists.newArrayList(unusedImportsNodes);
    // Add the nodes to remove in start position order.
    nodesToRemove.stream()
        .sorted(Comparator.comparingInt(ASTNode::getStartPosition))
        .forEach(nodesToWrap::add);
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

      newFileContent.append(fileContent, currentPosition, startPosition);

      StringBuilder strippedCodeBuilder = new StringBuilder();
      for (char c : fileContent.substring(startPosition, endPosition).toCharArray()) {
        strippedCodeBuilder.append(Character.isWhitespace(c) ? c : ' ');
      }
      if (nodeToWrap instanceof EnumConstantDeclaration) {
        // HACK: We assume that if there is a comma, it directly follows the enum constant and we
        // remove it. In practice this should work for most cases since if there is a comma
        // following the constant, the formatter will have it adjacent to the constant. If this is
        // the last constant then it would not be followed by a comma but rather by a semicolon; and
        // in this case it is ok to just remove the constant and leave the preceding comma.
        if (fileContent.charAt(endPosition) == ',') {
          strippedCodeBuilder.append(' ');
          endPosition++;
        }
      }
      newFileContent.append(strippedCodeBuilder);
      currentPosition = endPosition;
    }
    newFileContent.append(fileContent, currentPosition, fileContent.length());

    return newFileContent.toString();
  }

  private GwtIncompatibleStripper() {}
}
