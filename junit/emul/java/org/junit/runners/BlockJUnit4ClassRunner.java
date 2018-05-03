package org.junit.runners;

import org.junit.runner.Runner;

/**
 * Implements the JUnit 4 standard test case class model, as defined by the annotations in the
 * org.junit package. Many users will never notice this class: it is now the default test class
 * runner, but it should have exactly the same behavior as the old test class runner ({@code
 * JUnit4ClassRunner}).
 *
 * <p>BlockJUnit4ClassRunner has advantages for writers of custom JUnit runners that are slight
 * changes to the default behavior, however:
 *
 * <ul>
 *   <li>It has a much simpler implementation based on {@link Statement}s, allowing new operations
 *       to be inserted into the appropriate point in the execution flow.
 *   <li>It is published, and extension and reuse are encouraged, whereas {@code JUnit4ClassRunner}
 *       was in an internal package, and is now deprecated.
 * </ul>
 *
 * <p>In turn, in 2009 we introduced {@link Rule}s. In many cases where extending
 * BlockJUnit4ClassRunner was necessary to add new behavior, {@link Rule}s can be used, which makes
 * the extension more reusable and composable.
 *
 * @since 4.5
 */
// CHANGED extends for J2CL
public class BlockJUnit4ClassRunner /*extends ParentRunner<FrameworkMethod>*/ extends Runner {
  // CHANGED throws for J2CL
  public BlockJUnit4ClassRunner(Class<?> klass) /*throws InitializationError*/ {}

  // CHANGED removed everything except constructor for J2CL
}
