/*
 * Copyright 2025 Google Inc.
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
package recordclass;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertThrows;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

import com.google.j2cl.integration.testing.TestUtils;

public class Main {

  public static void main(String... args) {
    testAccessors();
    testEquals();
    testHashCode();
    testToString();
    testConstructor();
    testOverriddenMethods();
    testGeneric();
    testImplementingInterface();
    testStaticFields();
    testAccessorBridges();
    testRecordPatterns();
    testUnconditionalPatterns();
    testRecordPatternEvaluationOrder();
    testExceptionInPattern();
    testCompactConstructorWithComponentReferences();
    testParametricRecord();
  }

  public static void testAccessors() {
    SimpleRecord r = new SimpleRecord(1);
    assertEquals(1, r.value());

    SimpleRecord r2 = new SimpleRecord(2);
    assertEquals(2, r2.value());

    StringRecord sr = new StringRecord("foo");
    assertEquals("foo", sr.value());
  }

  public static void testEquals() {
    SimpleRecord r0 = new SimpleRecord(1);
    SimpleRecord r1 = new SimpleRecord(1);
    assertTrue(r0.equals(r0));
    assertTrue(r0.equals(r1));
    assertTrue(r1.equals(r0));

    SimpleRecord r2 = new SimpleRecord(2);
    assertFalse(r0.equals(r2));
    assertFalse(r2.equals(r0));

    // Records with same structure but different type are not equal.
    OtherRecord or0 = new OtherRecord(1);
    assertFalse(r0.equals(or0));
    assertFalse(or0.equals(r0));

    StringRecord sr0 = new StringRecord("foo");
    StringRecord sr1 = new StringRecord("foo");
    assertTrue(sr0.equals(sr1));
    assertTrue(sr1.equals(sr0));
    assertFalse(sr0.equals(r0));
    assertFalse(r0.equals(sr0));

    // Array values follow reference equality.
    ArrayRecord ar0 = new ArrayRecord(new int[] {1, 2});
    ArrayRecord ar1 = new ArrayRecord(new int[] {1, 2});
    assertFalse(ar0.equals(ar1));
    assertFalse(ar1.equals(ar0));
    int[] arrayValue = new int[] {1, 2};
    ArrayRecord ar2 = new ArrayRecord(arrayValue);
    ArrayRecord ar3 = new ArrayRecord(arrayValue);
    assertTrue(ar2.equals(ar3));
    assertTrue(ar3.equals(ar2));

    // Object values call `equals()` (reference equality unless the type overrides it).
    ObjectRecord objr0 = new ObjectRecord(new Object());
    ObjectRecord objr1 = new ObjectRecord(new Object());
    assertFalse(objr0.equals(objr1));
    assertFalse(objr1.equals(objr0));
    Object objectValue = new Object();
    ObjectRecord objr2 = new ObjectRecord(objectValue);
    ObjectRecord objr3 = new ObjectRecord(objectValue);
    assertTrue(objr2.equals(objr3));
    assertTrue(objr3.equals(objr2));
    ObjectRecord objr4 = new ObjectRecord(new SimpleRecord(234));
    assertTrue(objr4.equals(new ObjectRecord(new SimpleRecord(234))));
    assertFalse(objr4.equals(new ObjectRecord(new SimpleRecord(235))));
    assertFalse(objr4.equals(new ObjectRecord(new OtherRecord(234))));

    EmptyRecord er0 = new EmptyRecord();
    EmptyRecord er1 = new EmptyRecord();
    assertTrue(er0.equals(er1));
    assertTrue(er1.equals(er0));
  }

  public static void testHashCode() {
    SimpleRecord r0 = new SimpleRecord(1);
    SimpleRecord r1 = new SimpleRecord(1);
    assertEquals(r0.hashCode(), r1.hashCode());

    int[] arrayValue = new int[] {1, 2};
    ArrayRecord ar0 = new ArrayRecord(arrayValue);
    ArrayRecord ar1 = new ArrayRecord(arrayValue);
    assertEquals(ar0.hashCode(), ar1.hashCode());

    EmptyRecord er0 = new EmptyRecord();
    EmptyRecord er1 = new EmptyRecord();
    assertEquals(er0.hashCode(), er1.hashCode());
  }

  @SuppressWarnings("ArrayToString")
  public static void testToString() {
    SimpleRecord r0 = new SimpleRecord(1);
    assertTrue(r0.toString().contains(Integer.toString(1)));

    StringRecord sr0 = new StringRecord("foo");
    assertTrue(sr0.toString().contains("foo"));

    // toString with an array component returns the result of toString on the array, not necessarily
    // the array elements.
    int[] arrayValue = new int[] {1, 2};
    ArrayRecord ar0 = new ArrayRecord(arrayValue);
    assertTrue(ar0.toString().contains(arrayValue.toString()));
  }

  static record SimpleRecord(int value) {}

  static record OtherRecord(int value) {}

  static record StringRecord(String value) {}

  @SuppressWarnings("ArrayRecordComponent")
  static record ArrayRecord(int[] value) {}

  static record ObjectRecord(Object value) {}

  static record EmptyRecord() {}

  public static void testConstructor() {
    RecordWithConstructor r0 = new RecordWithConstructor(1, 1);
    assertEquals(2, r0.a());
    assertEquals(0, r0.b());

    RecordWithCompactConstructor r1 = new RecordWithCompactConstructor(-1, 2);
    assertEquals(0, r1.a());
    assertEquals(2, r1.b());

    RecordWithCompactConstructor r2 = new RecordWithCompactConstructor(1, -2);
    assertEquals(1, r2.a());
    assertEquals(-2, r2.b());

    RecordDelegatingToImplicitConstructor r3 = new RecordDelegatingToImplicitConstructor();
    assertEquals(123, r3.value());

    RecordDelegatingToExplicitConstructor r4 = new RecordDelegatingToExplicitConstructor();
    assertEquals(124, r4.value());

    RecordDelegatingToCompactConstructor r5 = new RecordDelegatingToCompactConstructor();
    assertEquals(125, r5.value());

    RecordWithConstructorWithDeepAssignment r6 = new RecordWithConstructorWithDeepAssignment(-1);
    assertEquals(0, r6.value());

    RecordWithConstructorWithSelfConstructorCall r7 =
        new RecordWithConstructorWithSelfConstructorCall(-1);
    assertEquals(-1, r7.value());
    RecordWithConstructorWithSelfConstructorCall r8 =
        new RecordWithConstructorWithSelfConstructorCall(1);
    assertEquals(1, r8.value());

    RecordWithConstructorWithStaticFieldAssignment r9 =
        new RecordWithConstructorWithStaticFieldAssignment(2);
    assertEquals(2, RecordWithConstructorWithStaticFieldAssignment.lastValue);
    assertEquals(2, r9.value());

    RecordWithConstructorWithUnrelatedAssignment r10 =
        new RecordWithConstructorWithUnrelatedAssignment(1);
    assertEquals(1, r10.value());
  }

  static record RecordWithConstructor(int a, int b) {
    public RecordWithConstructor(int a, int b) {
      this.a = a + 1;
      this.b = b - 1;
    }
  }

  static record RecordWithCompactConstructor(int a, int b) {
    public RecordWithCompactConstructor {
      if (a < 0) {
        a = 0;
      }
    }
  }

  // Non-canonical constructors are required to defer to the canonical constructor.
  static record RecordDelegatingToImplicitConstructor(int value) {
    public RecordDelegatingToImplicitConstructor() {
      this(123);
    }
  }

  static record RecordDelegatingToExplicitConstructor(int value) {
    public RecordDelegatingToExplicitConstructor(int value) {
      this.value = value;
    }

    public RecordDelegatingToExplicitConstructor() {
      this(124);
    }
  }

  static record RecordDelegatingToCompactConstructor(int value) {
    public RecordDelegatingToCompactConstructor {}

    public RecordDelegatingToCompactConstructor() {
      this(125);
    }
  }

  static record RecordWithConstructorWithDeepAssignment(int value) {
    public RecordWithConstructorWithDeepAssignment(int value) {
      if (value < 0) {
        this.value = 0;
      } else {
        this.value = value;
      }
    }
  }

  static record RecordWithConstructorWithSelfConstructorCall(int value) {
    // Test that this is still processed as a compact constructor despite calling the record
    // constructor.
    @SuppressWarnings("CheckReturnValue") // Testing something else, not using the return value.
    public RecordWithConstructorWithSelfConstructorCall {
      if (value < 0) {
        new RecordWithConstructorWithSelfConstructorCall(0);
      }
    }
  }

  @SuppressWarnings({"NonFinalStaticField", "StaticAssignmentInConstructor"})
  static record RecordWithConstructorWithStaticFieldAssignment(int value) {
    public static int lastValue = 0;

    // Test that this is still processed as a compact constructor despite assigning a static field.
    public RecordWithConstructorWithStaticFieldAssignment {
      lastValue = value;
    }
  }

  static class C {
    int x;
  }

  static record RecordWithConstructorWithUnrelatedAssignment(int value) {
    // Test that this is still processed as a compact constructor despite calling some other
    // constructor and assigning a field.
    public RecordWithConstructorWithUnrelatedAssignment {
      new C().x = 1;
    }
  }

  public static void testOverriddenMethods() {
    RecordWithOverriddenEquals r0 = new RecordWithOverriddenEquals("a", "b");
    assertEquals(r0, new RecordWithOverriddenEquals("a", "bnot"));
    assertNotEquals(r0, new RecordWithOverriddenEquals("anot", "b"));

    RecordWithOverriddenHashCode r2 = new RecordWithOverriddenHashCode("a");
    assertEquals(1234, r2.hashCode());

    RecordWithOverriddenToString r3 = new RecordWithOverriddenToString("a");
    assertEquals("foo", r3.toString());

    RecordWithOverriddenAccessor r4 = new RecordWithOverriddenAccessor("a");
    assertEquals("foo_instead", r4.a());
  }

  static record RecordWithOverriddenEquals(String a, String b) {
    @Override
    public boolean equals(Object other) {
      // Only check one field.
      return a.equals(((RecordWithOverriddenEquals) other).a);
    }
  }

  static record RecordWithOverriddenHashCode(String a) {
    @Override
    public int hashCode() {
      return 1234;
    }
  }

  static record RecordWithOverriddenToString(String a) {
    @Override
    public String toString() {
      return "foo";
    }
  }

  static record RecordWithOverriddenAccessor(String a) {
    @SuppressWarnings("MissingOverride") // TODO(b/449764809) Allow @Override
    public String a() {
      return "foo_instead";
    }
  }

  public static void testGeneric() {
    GenericRecord<String> r0 = new GenericRecord<>("abc");
    assertEquals("abc", r0.a());

    GenericRecord<String> r1 = new GenericRecord<>("abc");
    assertEquals(r0, r1);

    GenericRecord<Integer> r2 = new GenericRecord<>(123);
    GenericRecord<String> r3 = new GenericRecord<>("123");
    assertNotEquals(r0, r2);
    assertNotEquals(r2, r3);

    // Should be equal because of type erasure.
    GenericRecord<Object> r4 = new GenericRecord<>("abc");
    assertEquals(r0, r4);
  }

  static record GenericRecord<T>(T a) {}

  @SuppressWarnings("BadInstanceof") // Specifically testing instanceof this case.
  public static void testImplementingInterface() {
    RecordImplementingInterface r = new RecordImplementingInterface("a");

    assertTrue(r instanceof InterfaceWithMethod);
    assertEquals("foo", r.foo());
    InterfaceWithMethod iwm = r;
    assertEquals("foo", iwm.foo());

    assertTrue(r instanceof InterfaceWithProperty);
    assertEquals("a", r.a());
    InterfaceWithProperty iwp = r;
    assertEquals("a", iwp.a());
  }

  interface InterfaceWithMethod {
    String foo();
  }

  interface InterfaceWithProperty {
    String a();
  }

  static record RecordImplementingInterface(String a)
      implements InterfaceWithMethod, InterfaceWithProperty {
    @Override
    public String foo() {
      return "foo";
    }
  }

  public static void testStaticFields() {
    StaticFieldRecord r0 = new StaticFieldRecord(true);
    assertEquals(1, r0.nonstaticField());
    assertEquals(1, StaticFieldRecord.staticField);

    StaticFieldRecord r1 = new StaticFieldRecord(123);
    assertEquals(123, r1.nonstaticField());

    StaticFieldRecord.staticField = 2;
    assertEquals(2, StaticFieldRecord.staticField);
    assertEquals(1, r0.nonstaticField());
    assertEquals(123, r1.nonstaticField());
  }

  @SuppressWarnings("NonFinalStaticField")
  static record StaticFieldRecord(int nonstaticField) {
    public static int staticField = 1;

    public StaticFieldRecord(boolean initializeWithStaticField) {
      this(initializeWithStaticField ? StaticFieldRecord.staticField : 0);
    }
  }

  interface PropertyAccessors<T> {
    T parametric();

    String nonParametric();
  }

  record RecordSpecializingInterface(String parametric, String nonParametric)
      implements PropertyAccessors<String> {}

  private static void testAccessorBridges() {
    PropertyAccessors<String> propertyAccessors = new RecordSpecializingInterface("abc", "def");
    assertEquals("abc", propertyAccessors.parametric());
    assertEquals("def", propertyAccessors.nonParametric());
  }

  private static void testRecordPatterns() {
    // TODO(b/466212492): Remove the stripping once the bug is fixed.
  }

  private static void testUnconditionalPatterns() {
    // TODO(b/466212492): Remove the stripping once the bug is fixed.
  }

  private static String sideEffectAccumulator;

  private static void testRecordPatternEvaluationOrder() {
    // TODO(b/466212492): Remove the stripping once the bug is fixed.
  }

  private static class AccessorException extends RuntimeException {}

  private static void testExceptionInPattern() {
    // TODO(b/466212492): Remove the stripping once the bug is fixed.
  }

  private static void testCompactConstructorWithComponentReferences() {

    record R(String s) {
      R {
        // Make sure that references to component fields that are not assignments do not prevent
        // the synthesis of the the implicit component initialization. If that were to happen the
        // test case above would fail.
        Runnable r =
            () -> {
              var unused = R.this.s;
            };
        class Class {
          void m() {
            var unused = R.this.s;
          }
        }
        var unused =
            new Object() {
              void m() {
                var unused2 = R.this.s;
              }
            };
      }
    }

    R r = new R("Hello");
    // Ensure that the component field was initialized.
    assertEquals("Hello", r.s);
  }

  @SuppressWarnings("unchecked") // This test case tests pattern matching with unsafe casts.
  private static void testParametricRecord() {
    record ParametericRecord<T>(T value) {}
    var stringRecord = new ParametericRecord<String>(null);
    // Because the type of genericRecord is ParametricRecord<String>, the unparameterized
    // ParametericRecord below is considered ParametricRecord<String> and the instanceof succeeds.
    assertTrue(stringRecord instanceof ParametericRecord(String s));
    // However if we explicitly specify the parameterization, the instanceof fails because it
    // the binding pattern `String s` is no longer considered unconditional
    assertFalse(stringRecord instanceof ParametericRecord<?>(String s));

    Object o = stringRecord;
    // Because the type of o is not ParametricRecord<String>, the unparameterized
    // ParametericRecord below is considered ParametricRecord<?> and the instanceof fails.
    assertFalse(o instanceof ParametericRecord(String s));

    // This is an unsafe cast, but the pattern below is considered unconditional and hence it
    // succeeds because `null` can be assigned to `Integer`.
    var integerRecord = (ParametericRecord<Integer>) o;
    assertTrue(integerRecord instanceof ParametericRecord(Integer i));

    o = new ParametericRecord<>("abc");
    ParametericRecord<Integer> unsafeCastRecord = (ParametericRecord<Integer>) o;
    // TODO(b/420648962) : The erasure cast here is missing in J2KT and the code crashes instead.
    if (!TestUtils.isJ2KtNative()) {
      assertThrowsClassCastException(
          () -> {
            // Since `unsafeCastRecord` is typed as `ParametricRecord<Integer>`, the pattern below
            // is unconditional, hence there is no instanceof check, but a underlying cast to
            // `Integer` that throws.
            var unused = unsafeCastRecord instanceof ParametericRecord(Integer i);
          });
    }
  }
}

