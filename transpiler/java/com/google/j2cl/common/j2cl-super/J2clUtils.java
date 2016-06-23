package com.google.j2cl.common;

import java.io.PrintStream;

/**
 * Utility methods to replace calls to Java methods that J2cl does not support, so they can be
 * supersourced when compiling J2cl with J2cl.
 */
public class J2clUtils {

  /**
   * J2cl's implementation of String.format(format, args). Returns only the format string.
   *
   * @param format
   * @param args
   * @return The formatted string.
   */
  public static String format(String format, Object... args) {
    return format;
  }

  /**
   * J2cl's implementation of PrintStream.printf(format, args). (Note that the method signature
   * differs from PrintStream.printf). Prints only the format string to the given PrintStream.
   *
   * @param stream
   * @param format
   * @param args
   * @return The stream that was printed to.
   */
  public static PrintStream printf(PrintStream stream, String format, Object... args) {
    stream.print(format);
    return stream;
  }

  /**
   * J2cl's implementation of System.exit(status). Throws a J2clExit Error, which should not be
   * caught.
   *
   * @param status
   */
  public static void exit(int status) {
    throw new J2clExit(status);
  }

  private static class J2clExit extends Error {
    public int status;

    public J2clExit(int status) {
      this.status = status;
    }
  }
}
