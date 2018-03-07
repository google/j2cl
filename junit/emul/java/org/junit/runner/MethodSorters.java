package org.junit.runners;

// REMOVED FOR J2CL
// import java.lang.reflect.Method;
// import java.util.Comparator;
// 
// import org.junit.internal.MethodSorter;

/**
 * Sort the methods into a specified execution order. Defines common {@link MethodSorter}
 * implementations.
 *
 * @since 4.11
 */
public enum MethodSorters {
  /**
   * Sorts the test methods by the method name, in lexicographic order, with {@link
   * Method#toString()} used as a tiebreaker
   */
  // REMOVED FOR J2CL
  // NAME_ASCENDING(MethodSorter.NAME_ASCENDING),
  
  // ADDED FOR J2CL
  NAME_ASCENDING,

  /**
   * Leaves the test methods in the order returned by the JVM.
   * Note that the order from the JVM may vary from run to run
   */
  // REMOVED FOR J2CL
  // JVM(null),
  
  /**
   * Sorts the test methods in a deterministic, but not predictable, order
   */
  // REMOVED FOR J2CL
  // DEFAULT(MethodSorter.DEFAULT);
  
  /**
   * Leaves the test methods in the order returned by the JVM. Note that the order from the JVM may
   * vary from run to run
   */
  // We are not supporting the JVM options
  // We would need to replicate JVM sorting order within an APT for different Java version
  // and rather have compilation error out if users are trying to use this ordering.
  // JVM,

  /** Sorts the test methods in a deterministic, but not predictable, order */
  DEFAULT;

// REMOVED FOR J2CL
//   private final Comparator<Method> comparator;
//  
//   private MethodSorters(Comparator<Method> comparator) {
//       this.comparator = comparator;
//   }

// REMOVED FOR J2CL  
//   public Comparator<Method> getComparator() {
//       return comparator;
//   }
}
