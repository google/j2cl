/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Miscellaneous tests of the Java to JavaScript compiler. */
@RunWith(JUnit4.class)
@SuppressWarnings({
  "unused",
  "BoxedPrimitiveConstructor",
  "ShortCircuitBoolean",
  "IdentityBinaryExpression",
  "LoopConditionChecker",
  "StaticQualifiedUsingExpression",
  "ClassCanBeStatic",
  "MethodCanBeStatic",
  "ConstantField",
  "MissingDefault",
  "MultipleTopLevelClasses",
  "unchecked",
  "rawtypes"
})
public class CompilerTest {
  interface MyMap {
    Object get(String key);
  }

  interface Silly {}

  interface SillyComparable<T extends Silly> extends Comparable<T> {
    @Override
    int compareTo(T obj);
  }

  private abstract static class AbstractSuper {
    public static String foo() {
      if (FALSE) {
        // prevent inlining
        return foo();
      }
      return "AbstractSuper";
    }
  }

  private abstract static class Apple implements Fruit {}

  private abstract static class Bm2BaseEvent {}

  private abstract static class Bm2ComponentEvent extends Bm2BaseEvent {}

  private static class Bm2KeyNav<E extends Bm2ComponentEvent> implements Bm2Listener<E> {
    @Override
    public int handleEvent(Bm2ComponentEvent ce) {
      return 5;
    }
  }

  private interface Bm2Listener<E extends Bm2BaseEvent> extends EventListener {
    int handleEvent(E be);
  }

  /** Used in {@link #testSwitchOnEnumTypedThis()}. */
  private static enum ChangeDirection {
    NEGATIVE,
    POSITIVE;

    public String getText() {
      switch (this) {
        case POSITIVE:
          return "POSITIVE";
        case NEGATIVE:
          return "NEGATIVE";
        default:
          throw new IllegalArgumentException("Unhandled change direction: " + this);
      }
    }
  }

  private static class ConcreteSub extends AbstractSuper {
    public static String foo() {
      if (FALSE) {
        // prevent inlining
        return foo();
      }
      return "ConcreteSub";
    }
  }

  private static interface Fruit {}

  private static class Fuji extends Apple {}

  private static class Granny extends Apple {}

  private static class NonSideEffectCauser {
    public static final String NOT_A_COMPILE_TIME_CONSTANT = null;
  }

  private static class SideEffectCauser {
    private static Object instance = new Object();

    static {
      CompilerTest.sideEffectChecker++;
    }

    public static Object causeClinitSideEffect() {
      return instance;
    }
  }

  private static class SideEffectCauser2 {
    static {
      CompilerTest.sideEffectChecker++;
    }

    public static Object causeClinitSideEffect() {
      return null;
    }
  }

  private static class SideEffectCauser3 {
    static {
      CompilerTest.sideEffectChecker++;
    }

    public static void causeClinitSideEffect() {}
  }

  private static class SideEffectCauser4 {
    public static String causeClinitSideEffectOnRead = "foo";

    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  private static class SideEffectCauser5 {
    public static String causeClinitSideEffectOnRead = "bar";

    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  private static class SideEffectCauser6 extends SideEffectCauser6Super {
    public static String causeClinitSideEffectOnRead = "bar";
  }

  private static class SideEffectCauser6Super {
    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  /** Ensures that a superclass's clinit is run before supercall arguments are evaluated. */
  private static class SideEffectCauser7 extends SideEffectCauser7Super {
    public SideEffectCauser7() {
      super(SideEffectCauser7Super.SHOULD_BE_TRUE);
    }
  }

  private static class SideEffectCauser7Super {
    public static boolean SHOULD_BE_TRUE = false;

    static {
      SHOULD_BE_TRUE = true;
    }

    public SideEffectCauser7Super(boolean shouldBeTrue) {
      if (shouldBeTrue) {
        CompilerTest.sideEffectChecker++;
      }
    }
  }

  /** Used in test {@link #testPrivateOverride()}. */
  private static class TpoChild extends TpoParent {
    @Override
    public int foo() {
      return callSuper();
    }

    private int callSuper() {
      return super.foo();
    }
  }

  /** Used in test {@link #testPrivateOverride()}. */
  private static class TpoGrandparent {
    public int foo() {
      return 0;
    }
  }

  /** Used in test {@link #testPrivateOverride()}. */
  private static class TpoParent extends TpoGrandparent {
    @Override
    public int foo() {
      // This should call the callSuper in TpoJsniParent, not the one
      // in TpoJsniChild
      return callSuper();
    }

    private int callSuper() {
      return super.foo();
    }
  }

  private static final class UninstantiableType {
    public Object field;
    public int intField;

    private UninstantiableType() {}

    public int returnInt() {
      return intField;
    }

    public Object returnNull() {
      return null;
    }
  }

  private static volatile boolean FALSE = false;

  private static int sideEffectChecker;

  private static volatile int THREE = 3;

  private static volatile boolean TRUE = true;

  private static volatile boolean volatileBoolean;
  private static volatile int volatileInt;
  private static volatile long volatileLong;

  private static String barShouldInline() {
    return "bar";
  }

  private static void foo(String string) {}

  private static void foo(Throwable throwable) {}

  private Integer boxedInteger = 0;

  @Test
  public void testArrayAccessSideEffect() {
    int index = 1;
    int[] array = null;
    try {
      // index should be set before exception is thrown
      array[index = 2]++;
      fail("null reference expected");
    } catch (Exception e) {
      // expected
    }
    assertEquals(2, index);
  }

  @Test
  public void testArrayStore() {
    Object[][] oaa;
    oaa = new Object[4][4];
    oaa[0][0] = "foo";
    assertEquals(oaa[0][0], "foo");

    oaa = new Object[4][];
    oaa[0] = new Object[4];
    oaa[0][0] = "bar";
    assertEquals(oaa[0][0], "bar");

    Apple[] apple = TRUE ? new Granny[3] : new Apple[3];
    Apple g = TRUE ? (Apple) new Granny() : (Apple) new Fuji();
    Apple a = apple[0] = g;
    assertEquals(g, a);

    byte[] bytes = new byte[10];
    bytes[0] = (byte) '1';
    assertEquals(49, bytes[0]);

    Object[] disguisedApple = apple;
    expectArrayStoreException(disguisedApple, "Hello");
    expectArrayStoreException(disguisedApple, true);
    expectArrayStoreException(disguisedApple, 42.0);
  }

  private void expectArrayStoreException(Object[] array, Object o) {
    try {
      array[0] = o;
      fail("Expected ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }
  }

  @Test
  public void testUnboxedArrays() {
    Double[] doubleArray = new Double[1];
    doubleArray[0] = 42.0;

    expectArrayStoreException(doubleArray, true);
    expectArrayStoreException(doubleArray, "Hello");
    expectArrayStoreException(doubleArray, new Integer(23));

    Boolean[] booleanArray = new Boolean[1];
    booleanArray[0] = true;

    expectArrayStoreException(booleanArray, 42.0);
    expectArrayStoreException(booleanArray, "Hello");
    expectArrayStoreException(booleanArray, new Integer(23));

    String[] stringArray = new String[1];
    stringArray[0] = "Hello";

    expectArrayStoreException(stringArray, 42.0);
    expectArrayStoreException(stringArray, true);
    expectArrayStoreException(stringArray, new Integer(23));
  }

  /**
   * Issue 3064: when the implementation of an interface comes from a superclass, it can be
   * necessary to add a bridge method that overrides the interface method and calls the inherited
   * method.
   */
  @Test
  public void testBridgeMethods1() {
    abstract class AbstractFoo {
      public int compareTo(AbstractFoo o) {
        return 0;
      }
    }

    class MyFoo extends AbstractFoo implements Comparable<AbstractFoo> {}

    /*
     * This subclass adds an extra curve ball: only one bridge method should be
     * created, in class MyFoo. MyFooSub should not get its own but instead use
     * the inherited one. Otherwise, two final methods with identical signatures
     * would override each other.
     */
    class MyFooSub extends MyFoo {}

    Comparable<AbstractFoo> comparable1 = new MyFooSub();
    assertEquals(0, comparable1.compareTo(new MyFoo()));

    Comparable<AbstractFoo> comparable2 = new MyFoo();
    assertEquals(0, comparable2.compareTo(new MyFooSub()));
  }

  /** Issue 3304. Doing superclasses first is tricky when the superclass is a raw type. */
  @Test
  public void testBridgeMethods2() {
    Bm2KeyNav<?> obs = new Bm2KeyNav() {};
    assertEquals(5, obs.handleEvent(null));
  }

  /** When adding a bridge method, be sure to handle transitive overrides relationships. */
  @Test
  public void testBridgeMethods3() {
    class AbstractFoo implements Silly {
      public int compareTo(AbstractFoo obj) {
        if (FALSE) {
          return compareTo(obj);
        }
        return 0;
      }
    }
    class MyFoo extends AbstractFoo implements SillyComparable<AbstractFoo> {}

    assertEquals(0, new MyFoo().compareTo(new MyFoo()));
  }

  /**
   * Issue 3517. Sometimes JDT adds a bridge method when a subclass's method differs only by return
   * type. In some versions of GWT, this has resulted in a bridge method overriding its own target,
   * and eventually TypeTightener producing an infinite recursion.
   */
  @Test
  public void testBridgeMethods4() {
    abstract class MyMapAbstract<V> implements MyMap {
      @Override
      public String get(String key) {
        return null;
      }
    }

    final class MyMapImpl<V> extends MyMapAbstract<V> {}

    MyMapImpl<String> mmap = new MyMapImpl<String>();
    assertNull(mmap.get("foo"));
  }

  @Test
  public void testCastOptimizer() {
    Granny g = new Granny();
    Apple a = g;
    Fruit f = g;
    a = (Apple) f;
    g = (Granny) a;
    g = (Granny) f;
  }

  @Test
  public void testClassLiterals() {
    if (Object.class.getName().startsWith("Class$")) {
      // If class metadata is disabled
      return;
    }

    assertEquals("void", void.class.toString());
    assertEquals("int", int.class.toString());
    assertEquals("class java.lang.String", String.class.toString());
    assertEquals("class com.google.j2cl.ported.java6.CompilerTest", CompilerTest.class.toString());
    assertEquals(
        "class com.google.j2cl.ported.java6.CompilerTest$UninstantiableType",
        UninstantiableType.class.toString());
    assertEquals(
        "interface com.google.j2cl.ported.java6.CompilerTest$Fruit", Fruit.class.toString());
    assertEquals("class [I", int[].class.toString());
    assertEquals("class [Ljava.lang.String;", String[].class.toString());
    assertEquals(
        "class [Lcom.google.j2cl.ported.java6.CompilerTest;", CompilerTest[].class.toString());
    assertEquals(
        "class [Lcom.google.j2cl.ported.java6.CompilerTest$UninstantiableType;",
        UninstantiableType[].class.toString());
    assertEquals(
        "class [Lcom.google.j2cl.ported.java6.CompilerTest$Fruit;", Fruit[].class.toString());
  }

  @Test
  public void testClinitSideEffectInlining() {
    sideEffectChecker = 0;
    SideEffectCauser.causeClinitSideEffect();
    assertEquals(1, sideEffectChecker);
    SideEffectCauser2.causeClinitSideEffect();
    assertEquals(2, sideEffectChecker);
    SideEffectCauser3.causeClinitSideEffect();
    assertEquals(3, sideEffectChecker);
    String foo = SideEffectCauser4.causeClinitSideEffectOnRead;
    assertEquals(4, sideEffectChecker);
    foo = SideEffectCauser6.causeClinitSideEffectOnRead;
    assertEquals(5, sideEffectChecker);
    new SideEffectCauser7();
    assertEquals(6, sideEffectChecker);
    String checkRescued = NonSideEffectCauser.NOT_A_COMPILE_TIME_CONSTANT;
    assertEquals(null, checkRescued);
  }

  private static boolean testClassInitialized = false;

  private static class TestClass {
    static {
      testClassInitialized = true;
    }

    public static int staticField = 32;
  }

  @Test
  public void testClinitOverInstance() {
    assertEquals(32, getTest().staticField);
    assertTrue(testClassInitialized);
  }

  private TestClass getTest() {
    assertFalse(testClassInitialized);
    return null;
  }

  @Test
  public void testConditionals() {
    assertTrue(TRUE ? TRUE : FALSE);
    assertFalse(FALSE ? TRUE : FALSE);
    assertFalse(TRUE ? FALSE : TRUE);
    assertTrue(FALSE ? FALSE : TRUE);

    assertTrue(true ? TRUE : FALSE);
    assertFalse(false ? TRUE : FALSE);
    assertFalse(true ? FALSE : TRUE);
    assertTrue(false ? FALSE : TRUE);

    assertTrue(TRUE ? true : FALSE);
    assertFalse(FALSE ? true : FALSE);
    assertFalse(TRUE ? false : TRUE);
    assertTrue(FALSE ? false : TRUE);

    assertTrue(TRUE ? TRUE : false);
    assertFalse(FALSE ? TRUE : false);
    assertFalse(TRUE ? FALSE : true);
    assertTrue(FALSE ? FALSE : true);
  }

  /**
   * Test for issue 7045.
   *
   * <p>Background. GWT does not explicitly model block scopes but allows names across different
   * blocks to be repeated and are represented as different local variables with the same name.
   *
   * <p>The GenerateJavaScriptAST assumes that locals with the same name can be coalesced. This
   * assumption only holds if no optimization introduces a local variable reference in a blockscope
   * that is using a different local of the same name.
   *
   * <p>The dataflow optimizer does not respect this assumption when doing copy propagation.
   */
  @Test
  public void testDataflowDuplicateNamesError() {
    StringBuilder topScope;
    {
      StringBuilder localScopeDuplicatedVar = new StringBuilder();
      localScopeDuplicatedVar.append("initial text");
      topScope = localScopeDuplicatedVar;
    }

    {
      // It's important that this StringBuilder have the same name as the one above.
      StringBuilder localScopeDuplicatedVar = new StringBuilder();
      localScopeDuplicatedVar.append("different text");
    }

    {
      // The value of main should be "initial text" at this point, but if this code
      // is run as compiled JavaScript then it will be "different text". (before the issue was
      // fixed).

      // The copy propagation optimization in the DataflowOptimizer replaces the reference to
      // topScope by localScopeDuplicatedVar (from the previous block).
      // When the JavaScript output is produces these two variables are coalesced.

      // Although unlikely an error like this can occur due to the way temporary variables are
      // generated via the TempLocalVisitor.
      assertEquals("initial text", topScope.toString());
    }
  }

  private int functionWithClashingParameters(int i0) {
    int c = 0;
    for (int i = 0; i < 5; i++) {
      c++;
    }
    for (int i = 0; i < 6; i++) {
      c++;
    }
    // Assumes that GenerateJavaScriptAST will rename one of the "i" variables to i0.
    return i0;
  }

  /** Test for issue 8870. */
  @Test
  public void testParameterVariableClash() {
    assertEquals(1, functionWithClashingParameters(1));
  }

  /** Test for issue 8877. */
  @Test
  public void testOptimizeInstanceOf() {
    class A {
      int counter = 0;

      Object get() {
        if (counter >= 0) {
          counter++;
        }
        return "aString";
      }
    }

    A anA = new A();
    assertFalse(anA.get() instanceof Integer);
    assertEquals(1, anA.counter);
  }

  @Test
  public void testDeadCode() {
    while (returnFalse()) {
      break;
    }

    do {
      break;
    } while (false);

    do {
      break;
    } while (returnFalse());

    for (; returnFalse(); ) {}

    boolean check = false;
    for (check = true; returnFalse(); fail()) {
      fail();
    }
    assertTrue(check);

    if (returnFalse()) {
      fail();
    } else {
    }

    if (!returnFalse()) {
    } else {
      fail();
    }

    // For these following tests, make sure that side effects in conditions
    // get propagated, even if they cause introduction of dead code.
    //
    boolean b = false;
    if ((b = true) ? true : true) {}
    assertTrue(b);

    boolean c = true;
    int val = 0;
    for (val = 1; c = false; ++val) {}
    assertFalse(c);

    boolean d = true;
    while (d = false) {}
    assertFalse(d);

    boolean e = true;
    if (true | (e = false)) {}
    assertFalse(e);
  }

  /** Test that code considered unreachable by the JDT does not crash the GWT compiler. */
  @Test
  public void testDeadCode2() {
    class SillyList {}

    final SillyList outerLocalVariable = new SillyList();

    new SillyList() {
      private void pretendToUse(SillyList x) {}

      void blah() {
        if (true) {
          throw new RuntimeException();
        }
        /*
         * This code is unreachable, and so outerLocalVariable is never actually
         * read by reachable code.
         */
        pretendToUse(outerLocalVariable);
      }
    };
  }

  @Test
  public void testDeadTypes() {
    if (false) {
      new Object() {}.toString();

      class Foo {
        void a() {}
      }
      new Foo().a();
    }
  }

  /** Make sure that the compiler does not crash itself on user code that divides by zero. */
  @SuppressWarnings({"divzero", "ConstantOverflow"})
  @Test
  public void testDivByZero() {
    assertTrue(Double.isNaN(0.0 / 0.0));

    try {
      assertEquals(0, 0 / 0);
      fail("expected an ArithmeticException");
    } catch (ArithmeticException expected) {
    }

    try {
      volatileLong = 0L / 0;
      fail("expected an ArithmeticException");
    } catch (ArithmeticException expected) {
    }

    assertTrue(Double.isNaN(0.0 % 0.0));

    try {
      assertTrue(Double.isNaN(0 % 0));
      fail("expected an ArithmeticException");
    } catch (ArithmeticException expected) {
    }

    try {
      volatileLong = 0L % 0;
      fail("expected an ArithmeticException");
    } catch (ArithmeticException expected) {
    }
  }

  @Test
  public void testEmptyBlockStatements() {
    boolean b = false;
    while (b) {}

    do {} while (b);

    for (; b; ) {}

    for (; ; ) {
      break;
    }

    if (b) {}

    if (b) {
    } else {
      b = false;
    }

    if (b) {
    } else {
    }
  }

  @SuppressWarnings("empty")
  @Test
  public void testEmptyStatements() {
    boolean b = false;

    while (b) ;

    do ;
    while (b);

    for (; b; ) ;

    for (int i = 0; i < 10; ++i) ;

    for (int i : new int[10]) ;

    for (Integer i : new ArrayList<Integer>()) ;

    for (; ; ) break;

    if (b) ;

    if (b) ;
    else b = false;

    if (b) ;
    else ;
  }

  @Test
  public void testEmptyTryBlock() {
    int x = 0;
    try {
    } finally {
      x = 1;
    }
    assertEquals(1, x);
  }

  @Test
  public void testCatchBlockWithKeyword() {
    try {
      throw new RuntimeException();
    } catch (Exception var) {
      assertFalse(var.toString().isEmpty());
    }
  }

  /** Ensure that only final fields are initializers when cstrs run, see issue 380. */
  @Test
  public void testFieldInitializationOrder() {
    ArrayList<String> seenValues = new ArrayList<String>();
    new FieldInitOrderChild(seenValues);
    assertEquals("i1=1,i2=0,i3=null,i4=null,i5=1,i6=1,i7=1", seenValues.get(0));
    assertEquals("i1=1,i2=1,i3=1,i4=2,i5=1,i6=2,i7=2", seenValues.get(1));
  }

  @Test
  public void testForStatement() {
    {
      int i;
      for (i = 0; i < 10; ++i) {}
      assertEquals(i, 10);
    }
    {
      int i;
      int c;
      for (i = 0, c = 10; i < c; ++i) {}
      assertEquals(i, 10);
      assertEquals(c, 10);
    }
    {
      int j = 0;
      for (int i = 0; i < 10; ++i) {
        ++j;
      }
      assertEquals(j, 10);
    }
    {
      int j = 0;
      for (int i = 0, c = 10; i < c; ++i) {
        ++j;
      }
      assertEquals(j, 10);
    }
  }

  /** Issue #615: Internal Compiler Error. */
  @Test
  public void testImplicitNull() {
    boolean b;
    String test = ((((b = true) ? null : null) + " ") + b);
    assertTrue(b);
    assertEquals("null true", test);
  }

  /**
   * Issue 2886: inlining should cope with local variables that do not have an explicit declaration
   * node.
   */
  @Test
  public void testInliningBoxedIncrement() {
    // should not actually inline, because it has a temp variable
    incrementBoxedInteger();
    assertEquals((Integer) 1, boxedInteger);
  }

  @Test
  public void testJavaScriptReservedWords() {
    boolean delete = TRUE;
    for (int in = 0; in < 10; ++in) {
      assertTrue(in < 10);
      assertTrue(delete);
    }
  }

  @Test
  public void testLabels() {
    int i = 0;
    int j = 0;
    outer:
    for (i = 0; i < 1; ++i) {
      inner:
      for (j = 0; j < 1; ++j) {
        break outer;
      }
      fail();
    }
    assertEquals(0, i);
    assertEquals(0, j);

    outer:
    for (i = 0; i < 1; ++i) {
      inner:
      for (j = 0; j < 1; ++j) {
        continue outer;
      }
      fail();
    }
    assertEquals(1, i);
    assertEquals(0, j);

    outer:
    for (i = 0; i < 1; ++i) {
      inner:
      for (j = 0; j < 1; ++j) {
        break inner;
      }
    }
    assertEquals(1, i);
    assertEquals(0, j);

    outer:
    for (i = 0; i < 1; ++i) {
      inner:
      for (j = 0; j < 1; ++j) {
        continue inner;
      }
    }
    assertEquals(1, i);
    assertEquals(1, j);

    /*
     * Issue 2069: a default with a break should not be stripped unless the
     * break has no label.
     */
    outer:
    while (true) {
      switch (THREE) {
        case 0:
        case 1:
          break;
        default:
          break outer;
      }
      fail("should not be reached");
    }

    /*
     * Issue 2770: labeled breaks in a switch statement with no default case
     * should not be pruned.
     */
    outer:
    while (true) {
      switch (THREE) {
        case 0:
        case 1:
        case 2:
        case 3:
          break outer;
        case 4:
        case 5:
          break;
      }
    }
  }

  private static final class RefObject<T> {
    T argvalue;

    RefObject(T refarg) {
      argvalue = refarg;
    }
  }

  /**
   * Test for issue 6471.
   *
   * <p>Cast normally can not appear on the left hand side of an assignment (they are not allowed in
   * Java source). However they might be present in the Java AST as a result of generics and
   * erasure; hence they need to be handled in the proper places.
   */
  @Test
  public void testLhsCastString() {

    RefObject<String> prefix = new RefObject<String>("Hello");

    // Due to erasure, prefix.argvalue is of type Object hence
    // the compiler "inserts" a cast transforming it into
    // ((String) prefix.argvalue) += ", goodbye!"
    // This way a lhs can be a cast (which is illegal in Java source code).
    prefix.argvalue += ", goodbye!";
    assertEquals("Hello, goodbye!", prefix.argvalue);
  }

  @Test
  public void testLhsCastInt() {

    RefObject<Integer> prefix = new RefObject<Integer>(41);

    // Due to erasure, prefix.argvalue is of type Object hence
    // the compiler "inserts" a cast transforming it into
    // ((Integer) prefix.argvalue) ++
    // This way a lhs can be a cast (which is illegal in Java source code).
    prefix.argvalue++;
    assertEquals(new Integer(42), prefix.argvalue);
  }

  @Test
  public void testLhsCastNested() {

    RefObject<Integer> prefix = new RefObject<Integer>(41);

    // Due to erasure, prefix.argvalue is of type Object hence
    // the compiler "inserts" a cast transforming it into
    // ((Integer) prefix.argvalue) ++
    // This way a lhs can be a cast (which is illegal in Java source code).
    Math.max(prefix.argvalue++, 40);
    assertEquals(new Integer(42), prefix.argvalue);
  }

  @Test
  public void testLocalClasses() {
    class Foo {
      public Foo(int j) {
        assertEquals(1, j);
      };
    }
    final int i = 1;
    new Foo(i) {
      {
        assertEquals(1, i);
      }
    };
    assertEquals(1, i);
  }

  // TODO(B/36863860): Uncomment test when bug is fixed.
  // @Test
  public void testCaptureEvaluationOrder() {
    class Foo {
      public Foo(int j) {
        assertEquals(1, j);
      };
    }
    final int i;
    new Foo(i = 1) {
      {
        assertEquals(1, i);
      }
    };
    assertEquals(1, i);
  }

  @Test
  public void testLocalRefs() {
    final String foo = TRUE ? "foo" : "bar";
    final String bar = TRUE ? "bar" : "foo";
    String result =
        new Object() {

          private String a = foo;

          {
            a = foo;
          }

          @Override
          public String toString() {
            return new Object() {

                  private static final String constantString = "wallawalla";

                  private String ai = foo;

                  {
                    ai = foo;
                  }

                  @SuppressWarnings("ReturnValueIgnored")
                  @Override
                  public String toString() {
                    // this line used to cause ICE due to no synthetic path to bar
                    bar.valueOf(false);

                    assertEquals("wallawalla", constantString);
                    return foo + a + ai;
                  }
                }.toString()
                + a;
          }
        }.toString();
    assertEquals(result, "foofoofoofoo");
  }

  /** test for issue 7824. */
  @Test
  public void testMethodNamedSameAsClass() {
    MethodNamedSameAsClass obj = new MethodNamedSameAsClass();
    obj.MethodNamedSameAsClass();
  }

  @Test
  public void testNotOptimizations() {
    assertFalse(!true);
    assertTrue(!false);

    assertTrue(!(TRUE == FALSE));
    assertFalse(!(TRUE != FALSE));

    assertFalse(!(3 < 4));
    assertFalse(!(3 <= 4));
    assertTrue(!(3 > 4));
    assertTrue(!(3 >= 4));

    assertTrue(!(4 < 3));
    assertTrue(!(4 <= 3));
    assertFalse(!(4 > 3));
    assertFalse(!(4 >= 3));

    assertTrue(!!TRUE);
    assertFalse(!!FALSE);

    double nan = Math.random() == 0 ? Double.NaN : Double.NaN;
    assertFalse(nan == (0.0 / 0.0));
    assertTrue(!(nan > 0));
    assertTrue(!(nan < 0));
    assertFalse(!!(nan > 0));
  }

  @Test
  public void testNullFlow() {
    UninstantiableType f = null;

    try {
      f.returnNull().getClass();
      fail();
    } catch (NullPointerException expected) {
    }

    try {
      UninstantiableType[] fa = null;
      fa[4] = null;
      fail();
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testNullFlowArray() {
    UninstantiableType[] uta = new UninstantiableType[10];
    assertEquals(uta.length, 10);
    assertEquals(uta[0], null);
    uta[1] = null;
    assertEquals(uta[1], null);
  }

  @Test
  public void testNullFlowOverloads() {
    foo((Throwable) null);
    foo((String) null);
  }

  @Test
  public void testNullFlowVsClassCastPrecedence() {
    assertThrows(ClassCastException.class, () -> ((UninstantiableType) new Object()).returnNull());
  }

  @Test
  public void testOuterSuperThisRefs() {
    new B();
  }

  /**
   * Test that calling a private instance method does not accidentally call another private method
   * that appears to override it. Private methods don't truly override each other.
   */
  @Test
  public void testPrivateOverride() {
    assertEquals(0, new TpoChild().foo());
  }

  @Test
  public void testReturnStatementInCtor() {
    class Foo {
      int i;

      Foo(int i) {
        this.i = i;
        if (i == 0) {
          return;
        } else if (i == 1) {
          return;
        }
        return;
      }
    }
    assertEquals(new Foo(0).i, 0);
    assertEquals(new Foo(1).i, 1);
    assertEquals(new Foo(2).i, 2);
  }

  @Test
  public void testStaticMethodResolution() {
    // Issue 2922
    assertEquals("AbstractSuper", AbstractSuper.foo());
  }

  @Test
  public void testStringOptimizations() {
    assertEquals("Herro, AJAX", "Hello, AJAX".replace('l', 'r'));
    assertEquals('J', "Hello, AJAX".charAt(8));
    assertEquals(11, "Hello, AJAX".length());
    assertFalse("Hello, AJAX".equals("me"));
    assertTrue("Hello, AJAX".equals("Hello, AJAX"));
    assertTrue("Hello, AJAX".equalsIgnoreCase("HELLO, ajax"));
    assertEquals("hello, ajax", "Hello, AJAX".toLowerCase(Locale.ROOT));

    assertEquals("foobar", "foo" + barShouldInline());
    assertEquals("1bar", 1 + barShouldInline());
    assertEquals("fbar", 'f' + barShouldInline());
    assertEquals("truebar", true + barShouldInline());
    assertEquals("3.5bar", 3.5 + barShouldInline());
    assertEquals("3.5bar", 3.5f + barShouldInline());
    assertEquals("27bar", 27L + barShouldInline());
    assertEquals("nullbar", null + barShouldInline());
  }

  @Test
  public void testSubclassStaticInnerAndClinitOrdering() {
    new CheckSubclassStaticInnerAndClinitOrdering();
  }

  @Test
  public void testSwitchOnEnumTypedThis() {
    assertEquals("POSITIVE", ChangeDirection.POSITIVE.getText());
    assertEquals("NEGATIVE", ChangeDirection.NEGATIVE.getText());
  }

  @Test
  public void testSwitchStatement() {
    switch (0) {
      case 0:
        // Once caused an ICE.
        int test;
        break;
    }
  }

  /** Tests cases where the compiler will convert to a simple if or block. */
  @Test
  public void testSwitchStatementConversions() {
    int i = 1;

    switch (i) {
      case 1:
        i = 2;
    }
    assertEquals(2, i);

    switch (i) {
      case 1:
        i = 3;
    }
    assertEquals(2, i);

    switch (i) {
      default:
        i = 3;
    }
    assertEquals(3, i);
  }

  @Test
  public void testSwitchStatementEmpty() {
    switch (0) {
    }
  }

  @Test
  public void testSwitchStatementFallthroughs() {
    int i = 1;
    switch (i) {
      case 1:
        i = 2;
        // fallthrough
      case 2:
        break;
      case 3:
        fail("Shouldn't get here");
    }
    assertEquals(2, i);

    switch (i) {
      case 1:
        break;
      case 2:
        i = 3;
        break;
      case 3:
        break;
      case 4:
        fail("Shouldn't get here");
        break;
    }
    assertEquals(3, i);
  }

  @Test
  public void testSwitchStatementWithUsefulDefault() {
    switch (1) {
      case 1:
      case 2:
      case 3:
        {
          break;
        }
      case 4:
      case 5:
      case 6:
      default:
        fail("Shouldn't get here");
        break;
    }
  }

  @Test
  public void testSwitchStatementWithUselessCases() {
    switch (1) {
      case 1:
      case 2:
      case 3:
        {
          break;
        }
      case 4:
      case 5:
      case 6:
      default:
        break;
    }
  }

  @Test
  public void testSwitchStatementWontRemoveExpression() {
    class Foo {
      boolean setFlag;

      int setFlag() {
        setFlag = true;
        return 3;
      }
    }

    Foo foo = new Foo();
    switch (foo.setFlag()) {
      case 3:
        break;
    }

    // Make sure that compiler didn't optimize away the switch statement's
    // expression
    assertTrue(foo.setFlag);
  }

  @Test
  public void testUnaryPlus() {
    int x;
    int y = -7;
    x = +y;
    assertEquals(-7, x);
  }

  private void incrementBoxedInteger() {
    // the following will need a temporary variable created
    boxedInteger++;
  }

  private static boolean returnFalse() {
    return false;
  }

  class A {
    public abstract class AA {}
  }

  class B extends A {

    {
      new AA() {};
    }
  }

  private static void assertTrue(boolean b) {
    assertThat(b).isTrue();
  }

  private static void assertFalse(boolean b) {
    assertThat(b).isFalse();
  }

  private static <U, V> void assertEquals(U expected, V value) {
    assertThat(value).isEqualTo(expected);
  }

  private static <U, V> void assertEquals(String message, U expected, V value) {
    assertWithMessage(message).that(value).isEqualTo(expected);
  }

  private static <U, V> void assertSame(String message, U expected, V value) {
    assertWithMessage(message).that(value).isSameInstanceAs(expected);
  }

  private static <U, V> void assertSame(U expected, V value) {
    assertThat(value).isSameInstanceAs(expected);
  }

  private static <U> void assertNotNull(U value) {
    assertThat(value).isNotNull();
  }

  private static <U> void assertNull(U value) {
    assertThat(value).isNull();
  }

  private static <U, V> void assertNotSame(String message, U expected, V value) {
    assertWithMessage(message).that(value).isNotSameInstanceAs(expected);
  }

  private static <U, V> void assertNotSame(U expected, V value) {
    assertThat(value).isNotSameInstanceAs(expected);
  }
}

// This construct used to cause an ICE
class CheckSubclassStaticInnerAndClinitOrdering extends Outer.StaticInner {
  private static class Foo {}

  private static final Foo FOO = new Foo();

  public CheckSubclassStaticInnerAndClinitOrdering() {
    this(FOO);
  }

  public CheckSubclassStaticInnerAndClinitOrdering(Foo foo) {
    // This used to be null due to clinit ordering issues
    assertThat(foo).isNotNull();
  }
}

class Outer {
  public static class StaticInner {}
}

/**
 * A superclass that invokes a method in its cstr, so that subclasses can see their state before
 * their own cstr has run.
 *
 * <p>See {@link CompilerTest#testFieldInitializationOrder()}.
 */
class FieldInitOrderBase {
  FieldInitOrderBase(ArrayList<String> seenValues, int x) {
    method(seenValues, x);
  }

  void method(ArrayList<String> seenValues, int x) {}
}
/**
 * A subclass that overrides {@link #method(ArrayList, int)} to see what the values of its own
 * fields are from within the superclass's cstr (before our own cstr has run).
 *
 * <p>See {@link CompilerTest#testFieldInitializationOrder()}.
 */
@SuppressWarnings("BoxedPrimitiveConstructor")
class FieldInitOrderChild extends FieldInitOrderBase {

  private final int i1 = 1;
  private int i2 = 1;
  private Integer i3 = new Integer(1);
  private Integer i4;
  private static final int i5 = 1;
  private static int i6 = 1;
  private static Integer i7 = new Integer(1);

  FieldInitOrderChild(ArrayList<String> seenValues) {
    // the superclass calls method(), which will record the pre-cstr value of our fields
    super(seenValues, 2);
    recordValues(seenValues);
  }

  // invoked by the super classes before our cstr has run
  @Override
  void method(ArrayList<String> seenValues, int x) {
    recordValues(seenValues);
    // i1 is final
    i2 = x;
    i3 = new Integer(x);
    i4 = new Integer(x);
    // i5 is final
    i6 = 2;
    i7 = new Integer(x);
  }

  private void recordValues(ArrayList<String> seenValues) {
    // i3, i4 and i7 would be directly converted into strings hence show undefined instead of null.
    // String operations should take care of corner cases where a string is null or undefined
    // see issue 8257.
    seenValues.add(
        "i1="
            + i1
            + ",i2="
            + i2
            + ",i3="
            + (i3 == null ? null : i3)
            + ",i4="
            + (i4 == null ? null : i4)
            + ",i5="
            + i5
            + ",i6="
            + i6
            + ",i7="
            + (i7 == null ? null : i7));
  }
}

class MethodNamedSameAsClass {
  public void MethodNamedSameAsClass() {}
}
