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

@NullMarked
public final class VisibilityModifiers {
  public static class Public {
    // TODO(b/206898384): Remove once J2KT narrows visibility from referenced types.
    public void publicWithPackagePrivateParameter(PackagePrivate param) {}

    public void publicWithTransitivePackagePrivateParameter(PackagePrivate.Public param) {}

    public PackagePrivate publicReturnsPackagePrivate() {
      throw new UnsupportedOperationException();
    }

    public <T extends PackagePrivate> void publicWithPackagePrivateTypeParameter(T param) {
      throw new UnsupportedOperationException();
    }
  }

  static class PackagePrivate {
    public void publicMethod() {}

    void packagePrivateMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}

    static class Public {}
  }

  public static final class Parameterized {
    // TODO(b/206898384): Remove once J2KT narrows visibility from referenced types.
    public static class PublicOfPackagePrivate<T extends PackagePrivate> {
      public void publicMethod() {}

      void packagePrivateMethod() {}

      protected void protectedMethod() {}

      private void privateMethod() {}
    }

    // TODO(b/206898384): Remove once J2KT narrows visibility from referenced types.
    public static class PublicOfTransitivePackagePrivate<T extends PackagePrivate.Public> {
      public void publicMethod() {}

      void packagePrivateMethod() {}

      protected void protectedMethod() {}

      private void privateMethod() {}
    }
  }
}
