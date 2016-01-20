package com.google.j2cl.generator;

import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.sourcemap.SourceInfo;
import com.google.j2cl.errors.Errors;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Generates the source maps.
 */
public class SourceMapGeneratorStage {
  public static final String SOURCE_MAP_SUFFIX = ".js.map";

  private Charset charset;
  private Errors errors;
  private FileSystem outputFileSystem;
  private String outputLocationPath;
  private boolean generateReadableSourceMaps;

  public SourceMapGeneratorStage(
      Charset charset,
      FileSystem outputFileSystem,
      String outputLocationPath,
      Errors errors,
      boolean generateReadableSourceMaps) {
    this.charset = charset;
    this.outputFileSystem = outputFileSystem;
    this.outputLocationPath = outputLocationPath;
    this.errors = errors;
    this.generateReadableSourceMaps = generateReadableSourceMaps;
  }

  public void generateSourceMaps(List<CompilationUnit> j2clCompilationUnits) {
    class SourceMapGatheringVisitor extends AbstractVisitor {
      private List<Statement> gatheredStatements = new ArrayList<>();
      private SourceMapGenerator sourceMapGenerator =
          SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);

      @Override
      public boolean enterStatement(Statement statement) {
        gatheredStatements.add(statement);
        return true;
      }

      /**
       * For each of the java types we iterate over the statements it contains in order of the
       * output location and add them to the SourceMapGenerator. Finally we render the contents of
       * the source map to a file ending in ".js.map".
       */
      @Override
      public void exitJavaType(JavaType javaType) {
        final String inputSourceName = getCurrentCompilationUnit().getFileName();
        sourceMapGenerator.reset();
        List<Statement> sortedStatements = statementsSortedByJavaSourcePosition();
        for (Statement statement : sortedStatements) {
          SourceInfo javaSourcePosition = statement.getInputSourceInfo();
          SourceInfo javaScriptSourcePosition = statement.getOutputSourceInfo();
          if (javaSourcePosition == SourceInfo.UNKNOWN_SOURCE_INFO
              || javaScriptSourcePosition == SourceInfo.UNKNOWN_SOURCE_INFO) {
            continue;
          }
          sourceMapGenerator.addMapping(
              inputSourceName,
              null,
              javaSourcePosition.getStartFilePosition(),
              javaScriptSourcePosition.getStartFilePosition(),
              javaScriptSourcePosition.getEndFilePosition());
        }

        StringBuilder builder = new StringBuilder();
        try {
          String typeName = javaType.getDescriptor().getClassName();
          sourceMapGenerator.appendTo(builder, typeName + ".impl.js");
          String output = builder.toString();
          String readableMap = ReadableSourceMapGenerator.generateForJavaType(javaType);
          Path absolutePathForSourceMap =
              GeneratorUtils.getAbsolutePath(
                  outputFileSystem,
                  outputLocationPath,
                  GeneratorUtils.getRelativePath(javaType),
                  SOURCE_MAP_SUFFIX);
          writeToFile(absolutePathForSourceMap, generateReadableSourceMaps ? readableMap : output);
        } catch (IOException e) {
          e.printStackTrace();
        }
        // Empty the gathered statements.
        gatheredStatements.clear();
      }

      private List<Statement> statementsSortedByJavaSourcePosition() {
        Collections.sort(
            gatheredStatements,
            new Comparator<Statement>() {
              @Override
              public int compare(Statement o1, Statement o2) {
                return o1.getOutputSourceInfo().compareTo(o2.getOutputSourceInfo());
              }
            });
        return gatheredStatements;
      }
    }

    SourceMapGatheringVisitor visitor = new SourceMapGatheringVisitor();
    for (CompilationUnit j2clUnit : j2clCompilationUnits) {
      j2clUnit.accept(visitor);
    }
  }

  private void writeToFile(Path outputPath, String sourceMap) {
    try {
      // Write using the provided fileSystem (which might be the regular file system or might be a
      // zip file.)
      Files.createDirectories(outputPath.getParent());
      Files.write(outputPath, sourceMap.getBytes(charset));
    } catch (IOException e) {
      errors.error(Errors.Error.ERR_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
      errors.maybeReportAndExit();
    }
  }
}
