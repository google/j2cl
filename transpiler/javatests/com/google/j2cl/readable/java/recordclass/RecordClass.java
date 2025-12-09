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

  private static void testLocalRecordClas() {
    record LocalRecord(int value) {}
    LocalRecord lr = new LocalRecord(1);
  }

  private static void testRecordPatterns() {
    record R2(int i, Object o) {}
    record R1(Object o, String s, R2 n) {}

    R1 r = new R1(new R2(1, "a"), "b", new R2(3, "c"));
    boolean b = r instanceof R1(R2(var i1, String s1), Object s2, R2 n);

    Object o = new R1(new R2(1, "a"), "b", new R2(3, "c"));
    boolean b1 = o instanceof R1(R2(var i1, String s1), Object s2, R2 n);
  }
}
