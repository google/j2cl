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

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class RecordClass {

  public void main() {
    SimpleRecord r = new SimpleRecord(1);
    r.value();

    ArrayRecord ar = new ArrayRecord(new int[] {1, 2});
    int arValueResult = ar.value()[0];

    RecordWithConstructor rwc = new RecordWithConstructor(1, 2);
    rwc.a();
    rwc.b();
  }

  static record EmptyRecord() {}

  static record SimpleRecord(int value) {}

  static record StringRecord(String value) {}

  static record ArrayRecord(int[] value) {}

  static record ObjectRecord(Object value) {}

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

  static record RecordWithDeferringConstructor(int value) {
    public RecordWithDeferringConstructor() {
      this(123);
    }
  }

  public static record RecordWithOverriddenObjectMethods(String value) {
    @Override
    public boolean equals(Object other) {
      return false;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public String toString() {
      return "foo";
    }
  }

  public static record RecordWithOverriddenAccessor(String value) {
    @SuppressWarnings("MissingOverride") // TODO(b/449764809) Allow @Override
    public String value() {
      return "foo";
    }
  }

  interface I {
    String a();
  }

  static record RecordImplementingInterface(String a) implements I {}

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

  // Repro for b/498931729
  public record PublicRecordWithPublicFactory() {
    public PublicRecordWithPublicFactory factory() {
      return new PublicRecordWithPublicFactory();
    }
  }

  @JsType
  public static record JsInteropRecord(
      @JsProperty(name = "customVal") int value,
      String text,
      @JsIgnore String ignored,
      boolean isFoo) {}

  @JsType
  public static record JsInteropRecordWithExplicitMembers(
      @JsProperty(name = "customVal") int value, String text, @JsIgnore String ignored) {

    public JsInteropRecordWithExplicitMembers(int value, String text, String ignored) {
      this.value = value;
      this.text = text;
      this.ignored = ignored;
    }

    public int value() {
      return value;
    }

    public String text() {
      return text;
    }

    public String ignored() {
      return ignored;
    }
  }

  @JsType
  public static record JsInteropRecordWithOverriddenAccessors(
      @JsProperty(name = "customVal") int value, String text, @JsIgnore String ignored) {

    public JsInteropRecordWithOverriddenAccessors(int value, String text, String ignored) {
      this.value = value;
      this.text = text;
      this.ignored = ignored;
    }

    public int value() {
      return value + 1;
    }

    public String text() {
      return text + "!";
    }

    public String ignored() {
      return ignored + "!";
    }
  }

  interface I2 {
    // Matches record component name so accepted as override.
    @JsProperty(name = "customVal")
    int value();

    // The inherited explicit name should win over record component name.
    @JsProperty(name = "customText")
    String text();

    // The inherited JsProperty annotation should win over for otherwise ignored record component.
    @JsProperty(name = "ignored")
    String ignored();

    // The inherited JsMethod annotation should win over for otherwise ignored record component.
    @JsMethod
    String ignored2();
  }

  @JsType
  public static record JsInteropRecordImplementingInterface(
      @JsProperty(name = "customVal") int value,
      String text,
      @JsIgnore String ignored,
      @JsIgnore String ignored2)
      implements I2 {}

  @JsType
  static record JsInteropRecordWithHiddenCanonical(int value) {
    JsInteropRecordWithHiddenCanonical(int value) {
      this.value = value;
    }
  }

  private static void testLocalRecordClass() {
    record LocalRecord(int value) {}
    LocalRecord lr = new LocalRecord(1);
  }

  private static void testRecordPatterns() {
    record R2(int i, Object o) {
      // The accessors are declared in a different order to test that the pattern matching is not
      // affected by the order of the accessor methods.
      public Object o() {
        return o;
      }

      public int i() {
        return i;
      }
    }
    record R1(Object o, String s, R2 n) {}

    R1 r = new R1(new R2(1, "a"), "b", new R2(3, "c"));
    boolean b =
        r instanceof R1(R2(var i1, String s1), Object s2, R2 n)
            && i1 == 1
            && s1.equals("a")
            && s2.equals("b")
            && n.i() == 3
            && n.o().equals("c");

    Object o = new R1(new R2(1, "a"), "b", new R2(3, "c"));
    boolean b1 = o instanceof R1(R2(var i1, String s1), Object s2, R2 n);

    record RecursiveRecord(RecursiveRecord r) {}
    o = new RecursiveRecord(null);
    b1 = o instanceof RecursiveRecord(RecursiveRecord(RecursiveRecord r1));

    record GenericRecord<T>(T value) {}
    var genericRecord = new GenericRecord<>("abc");
    boolean b2 = genericRecord instanceof GenericRecord<String>(String s);
  }

  record VarargsRecord(String... values) {}

  record VarargsRecordWithCustomAccessor(String... value) {
    public String[] value() {
      return value;
    }
  }
}
