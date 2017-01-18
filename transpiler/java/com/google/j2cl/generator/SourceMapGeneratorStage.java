package com.google.j2cl.generator;

import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.j2cl.ast.Type;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.problems.Problems;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generates the source maps.
 */
public class SourceMapGeneratorStage {
  public static final String SOURCE_MAP_SUFFIX = ".js.map";

  private final Charset charset;
  private final Problems problems;
  private final FileSystem outputFileSystem;
  private final String outputLocationPath;
  private final String javaSourceFile;
  private final String javaScriptImplementationFileContents;
  private final boolean generateReadableSourceMaps;
  private final String compilationUnitSourceFileName;

  public SourceMapGeneratorStage(
      Charset charset,
      FileSystem outputFileSystem,
      String compilationUnitSourceFileName,
      String outputLocationPath,
      String javaSourceFile,
      String javaScriptImplementationFileContents,
      Problems problems,
      boolean generateReadableSourceMaps) {
    this.charset = charset;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.javaSourceFile = javaSourceFile;
    this.problems = problems;
    this.generateReadableSourceMaps = generateReadableSourceMaps;
    this.compilationUnitSourceFileName = compilationUnitSourceFileName;
    this.javaScriptImplementationFileContents = javaScriptImplementationFileContents;
  }

  public void generateSourceMaps(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition) {
    try {
      String output =
          generateReadableSourceMaps
              ? ReadableSourceMapGenerator.generate(
                  javaSourcePositionByOutputSourcePosition,
                  Paths.get(javaSourceFile),
                  javaScriptImplementationFileContents)
              : renderSourceMapToString(type, javaSourcePositionByOutputSourcePosition);

      if (output.isEmpty() && generateReadableSourceMaps) {
        return;
      }
      Path absolutePathForSourceMap =
          GeneratorUtils.getAbsolutePath(
              outputFileSystem,
              outputLocationPath,
              GeneratorUtils.getRelativePath(type),
              SOURCE_MAP_SUFFIX);
      GeneratorUtils.writeToFile(absolutePathForSourceMap, output, charset, problems);
    } catch (IOException e) {
      problems.error("Could not generate source maps for %s: %s", javaSourceFile, e.getMessage());
    }
  }

  private String renderSourceMapToString(
      Type type, Map<SourcePosition, SourcePosition> javaSourcePositionByOutputSourcePosition)
      throws IOException {
    SourceMapGenerator sourceMapGenerator =
        SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
    for (Entry<SourcePosition, SourcePosition> entry :
        javaSourcePositionByOutputSourcePosition.entrySet()) {
      SourcePosition javaSourcePosition = entry.getValue();
      SourcePosition javaScriptSourcePosition = entry.getKey();
      if (javaSourcePosition == SourcePosition.UNKNOWN
          || javaScriptSourcePosition == SourcePosition.UNKNOWN) {
        continue;
      }
      sourceMapGenerator.addMapping(
          compilationUnitSourceFileName,
          null,
          toFilePosition(javaSourcePosition.getStartFilePosition()),
          toFilePosition(javaScriptSourcePosition.getStartFilePosition()),
          toFilePosition(javaScriptSourcePosition.getEndFilePosition()));
    }
    StringBuilder sb = new StringBuilder();
    String typeName = type.getDescriptor().getSimpleBinaryName();
    sourceMapGenerator.appendTo(sb, typeName + JavaScriptImplGenerator.FILE_SUFFIX);
    return sb.toString();
  }

  /**
   * Converts a j2cl File Position to a JsCompiler sourcemap File Position.
   *
   * @param j2clFilePosition
   * @return JsCompiler sourcemap File Position
   */
  private FilePosition toFilePosition(com.google.j2cl.common.FilePosition j2clFilePosition) {
    return new FilePosition(j2clFilePosition.getLine(), j2clFilePosition.getColumn());
  }
}
