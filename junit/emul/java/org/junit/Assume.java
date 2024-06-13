package org.junit;

// REMOVED FOR J2CL
// import static java.util.Arrays.asList;
// import static org.hamcrest.CoreMatchers.everyItem;
// import static org.hamcrest.CoreMatchers.is;
// import static org.hamcrest.CoreMatchers.notNullValue;
// import static org.hamcrest.CoreMatchers.nullValue;

// REMOVED FOR J2CL
// import org.hamcrest.Matcher;

/**
 * A set of methods useful for stating assumptions about the conditions in which a test is
 * meaningful. A failed assumption does not mean the code is broken, but that the test provides no
 * useful information. Assume basically means "don't run this test if these conditions don't apply".
 * The default JUnit runner skips tests with failing assumptions. Custom runners may behave
 * differently.
 *
 * <p>A good example of using assumptions is in <a
 * href="https://github.com/junit-team/junit4/wiki/Theories">Theories</a> where they are needed to
 * exclude certain datapoints that aren't suitable or allowed for a certain test case. Failed
 * assumptions are usually not logged, because there may be many tests that don't apply to certain
 * configurations.
 *
 * <p>These methods can be used directly: <code>Assume.assumeTrue(...)</code>, however, they read
 * better if they are referenced through static import:<br>
 *
 * <pre>
 * import static org.junit.Assume.*;
 *    ...
 *    assumeTrue(...);
 * </pre>
 *
 * @see <a href="https://github.com/junit-team/junit4/wiki/Theories">Theories</a>
 * @since 4.4
 */
public class Assume {

  /**
   * Do not instantiate.
   *
   * @deprecated since 4.13.
   */
  @Deprecated
  public Assume() {}

  /**
   * If called with an expression evaluating to {@code false}, the test will halt and be ignored.
   */
  public static void assumeTrue(boolean b) {
    assumeThat(b, true, b);
  }

  /** The inverse of {@link #assumeTrue(boolean)}. */
  public static void assumeFalse(boolean b) {
    assumeThat(!b, false, b);
  }

  /**
   * If called with an expression evaluating to {@code false}, the test will halt and be ignored.
   *
   * @param b If <code>false</code>, the method will attempt to stop the test and ignore it by
   *     throwing {@link AssumptionViolatedException}.
   * @param message A message to pass to {@link AssumptionViolatedException}.
   */
  public static void assumeTrue(String message, boolean b) {
    if (!b) {
      throw new AssumptionViolatedException(message);
    }
  }

  /** The inverse of {@link #assumeTrue(String, boolean)}. */
  public static void assumeFalse(String message, boolean b) {
    assumeTrue(message, !b);
  }

  /**
   * If called with a {@code null} array or one or more {@code null} elements in {@code objects},
   * the test will halt and be ignored.
   */
  public static void assumeNotNull(Object... objects) {
    assumeNotNull((Object) objects);
    for (Object o : objects) {
      assumeNotNull(o);
    }
  }

  private static void assumeNotNull(Object o) {
    if (o == null) {
      throw new AssumptionViolatedException("Got null, but expected non-null");
    }
  }

  /**
   * Call to assume that <code>actual</code> satisfies the condition specified by <code>matcher
   * </code>. If not, the test halts and is ignored. Example:
   *
   * <pre>:
   *   assumeThat(1, is(1)); // passes
   *   foo(); // will execute
   *   assumeThat(0, is(1)); // assumption failure! test halts
   *   int x = 1 / 0; // will never execute
   * </pre>
   *
   * @param <T> the static type accepted by the matcher (this can flag obvious compile-time problems
   *     such as {@code assumeThat(1, is("a"))}
   * @param actual the computed value being compared
   * @param matcher an expression, built of {@link Matcher}s, specifying allowed values
   * @see org.hamcrest.CoreMatchers
   * @see org.junit.matchers.JUnitMatchers
   */
  // REMOVED FOR J2CL
  // public static <T> void assumeThat(T actual, Matcher<T> matcher) {
  //     if (!matcher.matches(actual)) {
  //         throw new AssumptionViolatedException(actual, matcher);
  //     }
  // }

  /**
   * Call to assume that <code>actual</code> satisfies the condition specified by <code>matcher
   * </code>. If not, the test halts and is ignored. Example:
   *
   * <pre>:
   *   assumeThat("alwaysPasses", 1, is(1)); // passes
   *   foo(); // will execute
   *   assumeThat("alwaysFails", 0, is(1)); // assumption failure! test halts
   *   int x = 1 / 0; // will never execute
   * </pre>
   *
   * @param <T> the static type accepted by the matcher (this can flag obvious compile-time problems
   *     such as {@code assumeThat(1, is("a"))}
   * @param actual the computed value being compared
   * @param matcher an expression, built of {@link Matcher}s, specifying allowed values
   * @see org.hamcrest.CoreMatchers
   * @see org.junit.matchers.JUnitMatchers
   */
  // REMOVED FOR J2CL
  // public static <T> void assumeThat(String message, T actual, Matcher<T> matcher) {
  //     if (!matcher.matches(actual)) {
  //         throw new AssumptionViolatedException(message, actual, matcher);
  //     }
  // }

  private static void assumeThat(boolean condition, Object expected, Object actual) {
    if (!condition) {
      throw new AssumptionViolatedException(Assert.format("Assumption failed", expected, actual));
    }
  }

  /**
   * Use to assume that an operation completes normally. If {@code e} is non-null, the test will
   * halt and be ignored.
   *
   * <p>For example:
   *
   * <pre>
   * \@Test public void parseDataFile() {
   *   DataFile file;
   *   try {
   *     file = DataFile.open("sampledata.txt");
   *   } catch (IOException e) {
   *     // stop test and ignore if data can't be opened
   *     assumeNoException(e);
   *   }
   *   // ...
   * }
   * </pre>
   *
   * @param e if non-null, the offending exception
   */
  public static void assumeNoException(Throwable e) {
    assumeNoException("", e);
  }

  /**
   * Attempts to halt the test and ignore it if Throwable <code>e</code> is not <code>null</code>.
   * Similar to {@link #assumeNoException(Throwable)}, but provides an additional message that can
   * explain the details concerning the assumption.
   *
   * @param e if non-null, the offending exception
   * @param message Additional message to pass to {@link AssumptionViolatedException}.
   * @see #assumeNoException(Throwable)
   */
  public static void assumeNoException(String message, Throwable e) {
    if (e != null) {
      throw new AssumptionViolatedException(
          Assert.buildPrefix(message)
              + "Expected no exception to be thrown but "
              + e.getClass().getName()
              + " was.",
          e);
    }
  }
}
