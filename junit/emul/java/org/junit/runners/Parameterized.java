package org.junit.runners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The custom runner <code>Parameterized</code> implements parameterized tests. When running a
 * parameterized test class, instances are created for the cross-product of the test methods and the
 * test data elements.
 *
 * <p>For example, to test the <code>+</code> operator, write:
 *
 * <pre>
 * &#064;RunWith(Parameterized.class)
 * public class AdditionTest {
 *     &#064;Parameters(name = &quot;{index}: {0} + {1} = {2}&quot;)
 *     public static Iterable&lt;Object[]&gt; data() {
 *         return Arrays.asList(new Object[][] { { 0, 0, 0 }, { 1, 1, 2 },
 *                 { 3, 2, 5 }, { 4, 3, 7 } });
 *     }
 *
 *     private int firstSummand;
 *
 *     private int secondSummand;
 *
 *     private int sum;
 *
 *     public AdditionTest(int firstSummand, int secondSummand, int sum) {
 *         this.firstSummand = firstSummand;
 *         this.secondSummand = secondSummand;
 *         this.sum = sum;
 *     }
 *
 *     &#064;Test
 *     public void test() {
 *         assertEquals(sum, firstSummand + secondSummand);
 *     }
 * }
 * </pre>
 *
 * <p>Each instance of <code>AdditionTest</code> will be constructed using the three-argument
 * constructor and the data values in the <code>&#064;Parameters</code> method.
 *
 * <p>In order that you can easily identify the individual tests, you may provide a name for the
 * <code>&#064;Parameters</code> annotation. This name is allowed to contain placeholders, which are
 * replaced at runtime. The placeholders are
 *
 * <dl>
 *   <dt>{index}
 *   <dd>the current parameter index
 *   <dt>{0}
 *   <dd>the first parameter value
 *   <dt>{1}
 *   <dd>the second parameter value
 *   <dt>...
 *   <dd>...
 * </dl>
 *
 * <p>In the example given above, the <code>Parameterized</code> runner creates names like <code>
 * [2: 3 + 2 = 5]</code>. If you don't use the name parameter, then the current parameter index is
 * used as name.
 *
 * <p>You can also write:
 *
 * <pre>
 * &#064;RunWith(Parameterized.class)
 * public class AdditionTest {
 *     &#064;Parameters(name = &quot;{index}: {0} + {1} = {2}&quot;)
 *     public static Iterable&lt;Object[]&gt; data() {
 *         return Arrays.asList(new Object[][] { { 0, 0, 0 }, { 1, 1, 2 },
 *                 { 3, 2, 5 }, { 4, 3, 7 } });
 *     }
 *
 *     &#064;Parameter(0)
 *     public int firstSummand;
 *
 *     &#064;Parameter(1)
 *     public int secondSummand;
 *
 *     &#064;Parameter(2)
 *     public int sum;
 *
 *     &#064;Test
 *     public void test() {
 *         assertEquals(sum, firstSummand + secondSummand);
 *     }
 * }
 * </pre>
 *
 * <p>Each instance of <code>AdditionTest</code> will be constructed with the default constructor
 * and fields annotated by <code>&#064;Parameter</code> will be initialized with the data values in
 * the <code>&#064;Parameters</code> method.
 *
 * <p>The parameters can be provided as an array, too:
 *
 * <pre>
 * &#064;Parameters
 * public static Object[][] data() {
 *   return new Object[][] {{0, 0, 0}, {1, 1, 2}, {3, 2, 5}, {4, 3, 7}}};
 * }
 * </pre>
 *
 * <h3>Tests with single parameter</h3>
 *
 * <p>If your test needs a single parameter only, you don't have to wrap it with an array. Instead
 * you can provide an <code>Iterable</code> or an array of objects.
 *
 * <pre>
 * &#064;Parameters
 * public static Iterable&lt;? extends Object&gt; data() {
 *   return Arrays.asList(&quot;first test&quot;, &quot;secondtest&quot;);
 * }
 * </pre>
 *
 * <p>or
 *
 * <pre>
 * &#064;Parameters
 * public static Object[] data() {
 *  return new Object[]{&quot;first test&quot;,&quot;second test&quot;};
 * }
 * </pre>
 *
 * <h3>Executing code before/after executing tests for specific parameters</h3>
 *
 * <p>If your test needs to perform some preparation or cleanup based on the parameters, this can be
 * done by adding public static methods annotated with {@code @BeforeParam}/{@code @AfterParam}.
 * Such methods should either have no parameters or the same parameters as the test.
 *
 * <pre>
 * &#064;BeforeParam
 * public static void beforeTestsForParameter(String onlyParameter) {
 *     System.out.println("Testing " + onlyParameter);
 * }
 * </pre>
 *
 * <h3>Create different runners</h3>
 *
 * <p>By default the {@code Parameterized} runner creates a slightly modified {@link
 * BlockJUnit4ClassRunner} for each set of parameters. You can build an own {@code Parameterized}
 * runner that creates another runner for each set of parameters. Therefore you have to build a
 * {@link ParametersRunnerFactory} that creates a runner for each {@link TestWithParameters}. (
 * {@code TestWithParameters} are bundling the parameters and the test name.) The factory must have
 * a public zero-arg constructor.
 *
 * <pre>
 * public class YourRunnerFactory implements ParametersRunnerFactory {
 *     public Runner createRunnerForTestWithParameters(TestWithParameters test)
 *             throws InitializationError {
 *         return YourRunner(test);
 *     }
 * }
 * </pre>
 *
 * <p>Use the {@link UseParametersRunnerFactory} to tell the {@code Parameterized} runner that it
 * should use your factory.
 *
 * <pre>
 * &#064;RunWith(Parameterized.class)
 * &#064;UseParametersRunnerFactory(YourRunnerFactory.class)
 * public class YourTest {
 *     ...
 * }
 * </pre>
 *
 * <h3>Avoid creating parameters</h3>
 *
 * <p>With {@link org.junit.Assume assumptions} you can dynamically skip tests. Assumptions are also
 * supported by the <code>&#064;Parameters</code> method. Creating parameters is stopped when the
 * assumption fails and none of the tests in the test class is executed. JUnit reports a {@link
 * Result#getAssumptionFailureCount() single assumption failure} for the whole test class in this
 * case.
 *
 * <pre>
 * &#064;Parameters
 * public static Iterable&lt;? extends Object&gt; data() {
 *  String os = System.getProperty("os.name").toLowerCase()
 *  Assume.assumeTrue(os.contains("win"));
 *  return Arrays.asList(&quot;first test&quot;, &quot;second test&quot;);
 * }
 * </pre>
 *
 * @since 4.0
 */
public class Parameterized extends Suite {
  /**
   * Annotation for a method which provides parameters to be injected into the test class
   * constructor by <code>Parameterized</code>. The method has to be public and static.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Parameters {
    /**
     * Optional pattern to derive the test's name from the parameters. Use numbers in braces to
     * refer to the parameters or the additional data as follows:
     *
     * <pre>
     * {index} - the current parameter index
     * {0} - the first parameter value
     * {1} - the second parameter value
     * etc...
     * </pre>
     *
     * <p>Default value is "{index}" for compatibility with previous JUnit versions.
     *
     * @return {@link MessageFormat} pattern string, except the index placeholder.
     * @see MessageFormat
     */
    String name() default "{index}";
  }

  /**
   * Annotation for fields of the test class which will be initialized by the method annotated by
   * <code>Parameters</code>. By using directly this annotation, the test class constructor isn't
   * needed. Index range must start at 0. Default value is 0.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Parameter {
    /**
     * Method that returns the index of the parameter in the array returned by the method annotated
     * by <code>Parameters</code>. Index range must start at 0. Default value is 0.
     *
     * @return the index of the parameter.
     */
    int value() default 0;
  }

  // REMOVED FOR J2CL
  // /**
  //  * Add this annotation to your test class if you want to generate a special runner. You have to
  //  * specify a {@link ParametersRunnerFactory} class that creates such runners. The factory must
  //  * have a public zero-arg constructor.
  //  */
  // @Retention(RetentionPolicy.RUNTIME)
  // @Inherited
  // @Target(ElementType.TYPE)
  // public @interface UseParametersRunnerFactory {
  //   /**
  //    * @return a {@link ParametersRunnerFactory} class (must have a default constructor)
  //    */
  //   Class<? extends ParametersRunnerFactory> value() default
  //       BlockJUnit4ClassRunnerWithParametersFactory.class;
  // }

  /**
   * Annotation for {@code public static void} methods which should be executed before evaluating
   * tests with particular parameters.
   *
   * @see org.junit.BeforeClass
   * @see org.junit.Before
   * @since 4.13
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface BeforeParam {}

  /**
   * Annotation for {@code public static void} methods which should be executed after evaluating
   * tests with particular parameters.
   *
   * @see org.junit.AfterClass
   * @see org.junit.After
   * @since 4.13
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface AfterParam {}
}
