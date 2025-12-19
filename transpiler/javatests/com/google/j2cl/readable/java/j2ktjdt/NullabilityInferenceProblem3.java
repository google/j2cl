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
package j2ktjdt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class NullabilityInferenceProblem3 {
  public abstract static class Generic<T extends Generic<T>> {
    public T concat(Generic<?> generic) {
      throw new RuntimeException();
    }

    public Concrete toConcrete() {
      throw new RuntimeException();
    }
  }

  public static final class Concrete extends Generic<Concrete> {
    private final Generic<?> delegate;

    public Concrete(Generic<?> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Concrete concat(Generic<?> generic) {
      return delegate.concat(generic).toConcrete();
    }
  }
}
