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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;
import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.common.SourceUtils;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.frontend.javac.AnnotatedNodeCollector;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

/**
 * A helper to comment out source code elements annotated with "incompatible" annotations
 * (e.g. @GwtIncompatible) so that they are ignored by tools taking that source as input such as the
 * java compile or the j2cl transpile.
 */
public final class GwtIncompatibleStripper {

  static void strip(
      Stream<Path> files, Output output, Problems problems, List<String> annotationNames) {
    try (output) {
      List<FileInfo> allPaths =
          SourceUtils.getAllSources(files, output.createTempDirectory("_source_jars"), problems)
              .filter(f -> f.targetPath().endsWith(".java"))
              .collect(toImmutableList());
      preprocessFiles(allPaths, output, problems, annotationNames);
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

    Context context = new Context();
    JavacFileManager fileManager = new JavacFileManager(context, true, UTF_8);
    try {
      // Set an empty bootclasspath to save time on javac initialization.
      // By default it will read classes from the host JDK's bootclasspath.
      fileManager.setLocationFromPaths(StandardLocation.PLATFORM_CLASS_PATH, ImmutableList.of());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    SimpleJavaFileObject fileObject =
        new SimpleJavaFileObject(URI.create("string:///temp.java"), JavaFileObject.Kind.SOURCE) {
          @Override
          public String getCharContent(boolean ignoreEncodingErrors) {
            return fileContent;
          }
        };

    Log.instance(context).useSource(fileObject);

    JCCompilationUnit compilationUnit =
        ParserFactory.instance(context)
            .newParser(
                fileContent,
                /* keepDocComments= */ true,
                /* keepEndPos= */ true,
                /* keepLineMap= */ true)
            .parseCompilationUnit();

    // Find all the declarations with the annotation name
    AnnotatedNodeCollector gwtIncompatibleVisitor =
        new AnnotatedNodeCollector(
            ImmutableList.copyOf(annotationNames),
            // Stop traversing on the first matching scope. Since we're deleting that entire scope
            // there's no need to traverse within it.
            /* stopTraversalOnMatch= */ true);
    gwtIncompatibleVisitor.scan(compilationUnit, null);
    ImmutableSet<Tree> incompatibleNodes = gwtIncompatibleVisitor.getNodes();

    // Gets all the imports that are no longer needed.
    UnusedImportsNodeCollector unusedImportsNodeCollector =
        new UnusedImportsNodeCollector(incompatibleNodes);
    unusedImportsNodeCollector.scan(compilationUnit, null);
    List<ImportTree> unusedImportsNodes = unusedImportsNodeCollector.getUnusedImports();

    // Replace all the not needed nodes with whitespace in the original source
    // (so we can preserve line numbers and have accurate source maps).
    List<Tree> nodesToRemove = Lists.newArrayList(unusedImportsNodes);
    nodesToRemove.addAll(incompatibleNodes);
    if (nodesToRemove.isEmpty()) {
      // Nothing was changed.
      return fileContent;
    }

    JavacTrees docTrees = JavacTrees.instance(context);
    DocSourcePositions sourcePositions = docTrees.getSourcePositions();

    // The nodes to remove are sorted by position. Overlapping node ranges (multivariable) are
    // handled by virtue of skipping ranges that have already been processed.
    StringBuilder newFileContent = new StringBuilder();
    int currentPosition = 0;
    for (Tree node : nodesToRemove) {
      int startPosition =
          getStartPosition(node, docTrees, sourcePositions, compilationUnit, fileContent);
      int endPosition = getEndPosition(node, sourcePositions, compilationUnit);

      // If a node is overlapping with a previously stripped node, its startPosition will be
      // adjusted to the currentPosition.
      // Multivariable declarations are only current example of this where both share start
      // position and first one end before the second one.
      startPosition = Math.max(startPosition, currentPosition);
      checkState(startPosition < endPosition);

      newFileContent.append(fileContent, currentPosition, startPosition);

      StringBuilder strippedCodeBuilder = new StringBuilder();
      for (char c : fileContent.substring(startPosition, endPosition).toCharArray()) {
        strippedCodeBuilder.append(Character.isWhitespace(c) ? c : ' ');
      }
      if (node instanceof VariableTree) {
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

  private static int getStartPosition(
      Tree node,
      JavacTrees trees,
      DocSourcePositions sourcePositions,
      CompilationUnitTree compilationUnit,
      String fileContent) {
    int start = (int) sourcePositions.getStartPosition(compilationUnit, node);
    DocCommentTree javaDoc = trees.getDocCommentTree(trees.getPath(compilationUnit, node));
    if (javaDoc != null) {
      int javadocStart = (int) sourcePositions.getStartPosition(compilationUnit, javaDoc, javaDoc);
      checkState(javadocStart < start);
      // DocTrees.getSourcePositions() returns the position slightly after the initial "/**".
      // To ensure we strip the entire Javadoc block, we reverse search for the exact "/**"
      // prefix starting from the returned docStart position.
      start = fileContent.lastIndexOf("/**", javadocStart);
    }
    return start;
  }

  private static int getEndPosition(
      Tree node, DocSourcePositions sourcePositions, CompilationUnitTree compilationUnit) {
    return (int) sourcePositions.getEndPosition(compilationUnit, node);
  }

  private GwtIncompatibleStripper() {}
}
