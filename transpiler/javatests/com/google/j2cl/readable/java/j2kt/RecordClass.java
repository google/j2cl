/*
 * Copyright 2026 Google Inc.
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
package j2kt;

public final class RecordClass {
  public record RecordWithEmptyCopyMethod(int a, int b) {
    public RecordWithEmptyCopyMethod copy() {
      return new RecordWithEmptyCopyMethod(a, b);
    }
  }

  public record RecordWithPartialCopyMethod(int a, int b) {
    public RecordWithPartialCopyMethod copy(int a) {
      return new RecordWithPartialCopyMethod(a, b);
    }
  }

  public record RecordWithFullCopyMethod(int a, int b) {
    public RecordWithFullCopyMethod copy(int a, int b) {
      return new RecordWithFullCopyMethod(a, b);
    }
  }

  public record RecordWithUnrelatedCopyMethod(int a, int b) {
    public RecordWithUnrelatedCopyMethod copy(String s) {
      return new RecordWithUnrelatedCopyMethod(s.length(), s.length());
    }
  }

  public record RecordWithComponentMethods(int a, int b) {
    public int component1() {
      return a;
    }

    public int component2() {
      return b;
    }
  }

  public static final class FieldAccessTest {
    public record SimpleRecord(int a) {}

    public static void test(SimpleRecord r) {
      // Field needs to be translated as `internal` to be accessible outside of the class.
      int a = r.a;
    }
  }
}
