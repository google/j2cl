package org.junit;

// REMOVED FOR J2CL
// import org.hamcrest.Matcher;

import com.google.j2cl.junit.runtime.InternalAssumptionViolatedException;

/**
 * An exception class used to implement <i>assumptions</i> (state in which a given test is
 * meaningful and should or should not be executed). A test for which an assumption fails should not
 * generate a test case failure.
 *
 * @see org.junit.Assume
 * @since 4.12
 */
@SuppressWarnings("deprecation")
// MODIFIED FOR J2CL
// Extend from our own internal exception type.
// END OF MODIFICATIONS
public class AssumptionViolatedException extends InternalAssumptionViolatedException {
  private static final long serialVersionUID = 1L;

  /**
   * An assumption exception with the given <i>actual</i> value and a <i>matcher</i> describing the
   * expectation that failed.
   */
  // REMOVED FOR J2CL
  // public <T> AssumptionViolatedException(T actual, Matcher<T> matcher) {
  //     super(actual, matcher);
  // }

  /**
   * An assumption exception with a message with the given <i>actual</i> value and a <i>matcher</i>
   * describing the expectation that failed.
   */
  // REMOVED FOR J2CL
  // public <T> AssumptionViolatedException(
  //         String message, T expected, Matcher<T> matcher) {
  //     super(message, expected, matcher);
  // }

  /** An assumption exception with the given message only. */
  public AssumptionViolatedException(String message) {
    super(message);
  }

  /** An assumption exception with the given message and a cause. */
  public AssumptionViolatedException(String message, Throwable t) {
    super(message, t);
  }
}
