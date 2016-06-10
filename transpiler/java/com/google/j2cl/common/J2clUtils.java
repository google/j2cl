package com.google.j2cl.common;

import java.io.PrintStream;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2cl with J2cl.
 */
public class J2clUtils {

  /**
   * J2cl's implementation of String.format(format, args).
   * Returns a formatted string using the specified format string and arguments.
   * @param format
   * @param args
   * @return The formatted string.
   */
  public static String format(String format, Object... args) {
    return String.format(format, args);
  }

  /**
   * J2cl's implementation of PrintStream.printf(format, args).
   *   (Note that the method signature differs from PrintStream.printf).
   * A convenience method to write a formatted string to this output stream using the specified
   *   format string and arguments.
   * @param stream
   * @param format
   * @param args
   * @return The stream that was printed to.
   */
  public static PrintStream printf(PrintStream stream, String format, Object... args) {
    return stream.printf(format, args);
  }

  /**
   * J2cl's implementation of System.exit(status).
   * Terminates the currently running Java Virtual Machine. The argument serves as a status code;
   *   by convention, a nonzero status code indicates abnormal termination.
   * @param status
   */
  public static void exit(int status) {
    System.exit(status);
  }
}
