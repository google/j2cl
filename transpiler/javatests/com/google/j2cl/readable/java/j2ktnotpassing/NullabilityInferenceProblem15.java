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
package j2ktnotpassing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityInferenceProblem15 {
  public interface JsArray<T extends @Nullable Object> {}

  public interface SpacersCell<T extends SpacersCell<T>> {}

  public interface ArrayTable<T extends @Nullable Object> {
    JsArray<T> getCellsInRow();
  }

  public interface SpacersTable<T extends SpacersCell<T>> extends ArrayTable<@Nullable T> {}

  // Repro for b/464077965
  public static void test(SpacersTable<? extends SpacersCell<?>> table) {
    JsArray<? extends SpacersCell<?>> cells = table.getCellsInRow();
  }
}
