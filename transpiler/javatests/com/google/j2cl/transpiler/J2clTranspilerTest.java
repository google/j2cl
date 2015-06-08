package com.google.j2cl.transpiler;

import com.google.common.io.Files;
import com.google.j2cl.errors.Errors;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * Tests for {@link com.google.j2cl.transpiler.J2clTranspiler#run}
 */
public class J2clTranspilerTest extends TestCase {

  private File tempDirectory;
  private ByteArrayOutputStream outputStream;
  private Errors errors;

  @Override
  public void setUp() {
    tempDirectory = createTempDirectory();
    outputStream = new ByteArrayOutputStream();
    errors = new Errors(new PrintStream(outputStream));
  }

  @Override
  public void tearDown() {
    errors.reset();
    deleteTempDirectory(tempDirectory);
  }

  public void testInvalidFlags() {
    String[] args = new String[] {"-unknown", "abc"};
    J2clTranspiler.run(errors, args);
    assertEquals(
        outputStream.toString(),
        "invalid flag: \"-unknown\" is not a valid option\n"
            + "Valid options: \n"
            + " -bootclasspath <path> -classpath (-cp) <path> -d <directory> -encoding <encoding> "
            + "-h (-help) -source <release> -sourcepath <file>\n"
            + "use -help for a list of possible options in more details\n"
            + "1 error(s).\n");
  }

  public void testHelpFlag() {
    String[] args = new String[] {"-help"};
    J2clTranspiler.run(errors, args);
    assertEquals(
        outputStream.toString(),
        " <source files>          : source files\n"
            + " -bootclasspath <path>   : Override location of bootstrap class files\n"
            + " -classpath (-cp) <path> : Specify where to find user class files and\n"
            + "                           annotation processors\n"
            + " -d <directory>          : Specify where to place generated files\n"
            + " -encoding <encoding>    : Specify character encoding used by source files\n"
            + " -h (-help)              : print this message\n"
            + " -source <release>       : Provide source compatibility with specified release\n"
            + " -sourcepath <file>      : Specify where to find input source files\n");
  }

  public void testInvalidOptions() {
    String[] args = new String[] {"-source", "2.0", "-encoding", "abc"};
    J2clTranspiler.run(errors, args);
    assertEquals(
        outputStream.toString(),
        "invalid source version: 2.0\n" + "unsupported encoding: abc\n" + "2 error(s).\n");
  }

  public void testBuggyCode() {
    String exampleJavaPath = "com/google/j2cl/transpiler/Buggy.java";
    String absoluteJavaPath = tempDirectory.getAbsolutePath() + "/" + exampleJavaPath;
    String outputDirectory = tempDirectory.getAbsolutePath() + "/output";
    addSourceFile("package com.google.j2cl.transpiler; public class Buggy {abc;}", exampleJavaPath);
    String[] args = new String[] {"-d", outputDirectory, absoluteJavaPath};
    J2clTranspiler.run(errors, args);
    assertTrue(outputStream.toString().contains("Buggy.java:1: Syntax error,"));
  }

  public void testValidOptions() {
    String exampleJavaPath = "com/google/j2cl/transpiler/Example.java";
    String absoluteJavaPath = tempDirectory.getAbsolutePath() + "/" + exampleJavaPath;
    String outputDirectory = tempDirectory.getAbsolutePath() + "/output";
    addSourceFile("package com.google.j2cl.transpiler; public class Example {}", exampleJavaPath);
    String[] args =
        new String[] {
          "-d", outputDirectory, "-source", "1.7", "-encoding", "UTF-8", absoluteJavaPath
        };
    J2clTranspiler.run(errors, args);
    assertEquals(errors.errorCount(), 0);
  }

  /**
   * Creates a temporary directory.
   */
  private File createTempDirectory() {
    File tempDirectory = null;
    try {
      tempDirectory = File.createTempFile("test", ".tmp");
      tempDirectory.delete();
      if (!tempDirectory.mkdir()) {
        throw new IOException("Could not create tmp directory: " + tempDirectory.getPath());
      }
      tempDirectory.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return tempDirectory;
  }

  /**
   * Recursively delete specified directory.
   */
  private void deleteTempDirectory(File directory) {
    if (directory != null && directory.exists()) {
      for (File file : directory.listFiles()) {
        if (file.isDirectory()) {
          deleteTempDirectory(file);
        } else {
          file.delete();
        }
      }
      directory.delete();
    }
  }

  /**
   * Creates a source file in the temporary directory.
   */
  private void addSourceFile(String source, String fileName) {
    File file = new File(tempDirectory, fileName);
    file.getParentFile().mkdirs();
    try {
      Files.write(source, file, Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
