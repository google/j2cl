package com.google.j2cl.common;

import java.beans.Introspector;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2cl with J2cl.
 */
public class J2clUtils {

  public static final String FILEPATH_SEPARATOR = File.separator;
  public static final char FILEPATH_SEPARATOR_CHAR = File.separatorChar;

  /**
   * J2cl's implementation of String.format(format, args).
   * Returns a formatted string using the specified format string and arguments.
   */
  public static String format(String format, Object... args) {
    return String.format(format, args);
  }

  /**
   * J2cl's implementation of PrintStream.printf(format, args).
   *   (Note that the method signature differs from PrintStream.printf).
   * A convenience method to write a formatted string to this output stream using the specified
   *   format string and arguments.
   */
  public static PrintStream printf(PrintStream stream, String format, Object... args) {
    return stream.printf(format, args);
  }
  
  /** Escapes a string into a representation suitable for literals. */
  public static String escapeJavaString(String string) {
    // NOTE: StringEscapeUtils.escapeJava does not escape unprintable character 127 (delete).
    return StringEscapeUtils.escapeJava(string).replace("\u007f", "\\u007F");
  }

  /** Convert a string to normal Java variable name capitalization. */
  public static String decapitalize(String substring) {
    return Introspector.decapitalize(substring);
  }

  /** Adapts a method that outputs to a stream to directly return the output as a String. */
  public static String streamToString(Consumer<OutputStream> streamOutputer) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    streamOutputer.accept(new PrintStream(outputStream));
    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
  }
}
