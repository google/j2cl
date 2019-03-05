package org.junit.runners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// REMOVED FOR J2CL
// import java.util.Collections;
// import java.util.List;

// REMOVED FOR J2CL
// import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
// import org.junit.runner.Description;
import org.junit.runner.Runner;
// REMOVED FOR J2CL
// import org.junit.runner.notification.RunNotifier;
// import org.junit.runners.model.InitializationError;
// import org.junit.runners.model.RunnerBuilder;

/**
 * Using <code>Suite</code> as a runner allows you to manually
 * build a suite containing tests from many classes. It is the JUnit 4 equivalent of the JUnit 3.8.x
 * static {@link junit.framework.Test} <code>suite()</code> method. To use it, annotate a class
 * with <code>@RunWith(Suite.class)</code> and <code>@SuiteClasses({TestClass1.class, ...})</code>.
 * When you run this class, it will run all the tests in all the suite classes.
 *
 * @since 4.0
 */
// CHANGED extends for J2CL
public class Suite /* extends ParentRunner<Runner> */ extends Runner {
    /**
     * Returns an empty suite.
     */
// REMOVED FOR J2CL
//     public static Runner emptySuite() {
//         try {
//             return new Suite((Class<?>) null, new Class<?>[0]);
//         } catch (InitializationError e) {
//             throw new RuntimeException("This shouldn't be possible");
//         }
//     }

    /**
     * The <code>SuiteClasses</code> annotation specifies the classes to be run when a class
     * annotated with <code>@RunWith(Suite.class)</code> is run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface SuiteClasses {
        /**
         * @return the classes to be run
         */
        Class<?>[] value();
    }

// REMOVED FOR J2CL
//     private static Class<?>[] getAnnotatedClasses(Class<?> klass) throws InitializationError {
//         SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
//         if (annotation == null) {
//             throw new InitializationError(String.format("class '%s' must have a SuiteClasses annotation", klass.getName()));
//         }
//         return annotation.value();
//     }
//
//     private final List<Runner> runners;
//
     /**
     * Called reflectively on classes annotated with <code>@RunWith(Suite.class)</code>
     *
     * @param klass the root class
     * @param builder builds runners for classes in the suite
     */
// REMOVED FOR J2CL
//     public Suite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
//         this(builder, klass, getAnnotatedClasses(klass));
//     }

    /**
     * Call this when there is no single root class (for example, multiple class names
     * passed on the command line to {@link org.junit.runner.JUnitCore}
     *
     * @param builder builds runners for classes in the suite
     * @param classes the classes in the suite
     */
// REMOVED FOR J2CL
//     public Suite(RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
//         this(null, builder.runners(null, classes));
//     }

    /**
     * Call this when the default builder is good enough. Left in for compatibility with JUnit 4.4.
     *
     * @param klass the root of the suite
     * @param suiteClasses the classes in the suite
     */
// REMOVED FOR J2CL
//     protected Suite(Class<?> klass, Class<?>[] suiteClasses) throws InitializationError {
//         this(new AllDefaultPossibilitiesBuilder(true), klass, suiteClasses);
//     }

    /**
     * Called by this class and subclasses once the classes making up the suite have been determined
     *
     * @param builder builds runners for classes in the suite
     * @param klass the root of the suite
     * @param suiteClasses the classes in the suite
     */
// REMOVED FOR J2CL
//     protected Suite(RunnerBuilder builder, Class<?> klass, Class<?>[] suiteClasses) throws InitializationError {
//         this(klass, builder.runners(klass, suiteClasses));
//     }

    /**
     * Called by this class and subclasses once the runners making up the suite have been determined
     *
     * @param klass root of the suite
     * @param runners for each class in the suite, a {@link Runner}
     */
// REMOVED FOR J2CL
//     protected Suite(Class<?> klass, List<Runner> runners) throws InitializationError {
//         super(klass);
//         this.runners = Collections.unmodifiableList(runners);
//     }

// REMOVED FOR J2CL
//     @Override
//     protected List<Runner> getChildren() {
//         return runners;
//     }

// REMOVED FOR J2CL
//     @Override
//     protected Description describeChild(Runner child) {
//         return child.getDescription();
//     }

// REMOVED FOR J2CL
//     @Override
//     protected void runChild(Runner runner, final RunNotifier notifier) {
//         runner.run(notifier);
//     }
}
