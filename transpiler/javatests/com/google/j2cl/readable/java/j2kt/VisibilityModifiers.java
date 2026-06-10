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

import com.google.auto.value.AutoValue;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class VisibilityModifiers {
  public static class Public {
    public void publicMethod() {}

    void packagePrivateMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}

    public void publicWithPackagePrivateParameter(PackagePrivate param) {}

    // TODO(b/206898384): Re-enable once J2KT narrows visibility from referenced types.
    // public void publicWithTransitivePackagePrivateParameter(PackagePrivate.Public param) {}

    public PackagePrivate publicReturnsPackagePrivate() {
      throw new UnsupportedOperationException();
    }

    // TODO(b/206898384): Re-enable once J2KT narrows visibility from referenced types.
    // public <T extends PackagePrivate> void publicWithPackagePrivateTypeParameter(T param) {
    //   throw new UnsupportedOperationException();
    // }
  }

  static class PackagePrivate {
    public void publicMethod() {}

    void packagePrivateMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}

    static class Public {}
  }

  protected static class Protected {
    public void publicMethod() {}

    void packagePrivateMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}
  }

  private static class Private {
    public void publicMethod() {}

    void packagePrivateMethod() {}

    protected void protectedMethod() {}

    private void privateMethod() {}
  }

  public static final class Parameterized {
    public static class PublicOfPublic<T extends Public> {
      public void publicMethod() {}

      void packagePrivateMethod() {}

      protected void protectedMethod() {}

      private void privateMethod() {}
    }

    // TODO(b/206898384): Re-enable once J2KT narrows visibility from referenced types.
    // public static class PublicOfPackagePrivate<T extends PackagePrivate> {
    //   public void publicMethod() {}
    //   void packagePrivateMethod() {}
    //   protected void protectedMethod() {}
    //   private void privateMethod() {}
    // }

    // public static class PublicOfTransitivePackagePrivate<T extends PackagePrivate.Public> {
    //   public void publicMethod() {}
    //   void packagePrivateMethod() {}
    //   protected void protectedMethod() {}
    //   private void privateMethod() {}
    // }
  }

  abstract static class PackagePrivateCollection implements Collection<PackagePrivate> {
    @Override
    public abstract boolean contains(Object o);
  }

  public static class ProtectedOverrideOfVisibilityWarningsSuppressed
      extends VisibilityWarningsSuppressed.NonFinal.Public {
    @Override
    protected void protectedMethod() {}
  }

  @AutoValue
  abstract static class Value {
    @AutoValue.Builder
    abstract static class Builder {
      abstract Value build();
    }
  }

  // Emulates generated AutoConverter class.
  abstract static class AutoConverter_Converter {}

  // Emulates generated AutoEnumConverter class.
  abstract static class AutoEnumConverter_EnumConverter {}

  public static final class Converter extends AutoConverter_Converter {}

  public static final class EnumConverter extends AutoEnumConverter_EnumConverter {}

  abstract static class NotAutoConverter_Converter {}

  abstract static class NotAutoEnumConverter_EnumConverter {}

  @SuppressWarnings("j2kt:visibility")
  public static class VisibilityWarningsSuppressed {
    public static final class NonFinal {
      public static class Public {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      static class PackagePrivate {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}

        public void publicWithPackagePrivateParameter(PackagePrivate param) {}

        public void publicWithTransitivePackagePrivateParameter(PackagePrivate.Public param) {}

        public PackagePrivate publicReturnsPackagePrivate() {
          throw new UnsupportedOperationException();
        }

        public <T extends PackagePrivate> void publicWithPackagePrivateTypeParameter(T param) {
          throw new UnsupportedOperationException();
        }

        public class Public {}
      }

      protected static class Protected {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      private static class Private {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      public static class PublicExtendsPackagePrivate extends PackagePrivate {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      public static final class Parameterized {
        public static class PublicOfPublic<T extends Public> {
          public void publicMethod() {}

          void packagePrivateMethod() {}

          protected void protectedMethod() {}

          private void privateMethod() {}
        }

        public static class PublicOfPackagePrivate<T extends PackagePrivate> {
          public void publicMethod() {}

          void packagePrivateMethod() {}

          protected void protectedMethod() {}

          private void privateMethod() {}
        }

        public static class PublicOfTransitivePackagePrivate<T extends PackagePrivate.Public> {
          public void publicMethod() {}

          void packagePrivateMethod() {}

          protected void protectedMethod() {}

          private void privateMethod() {}
        }
      }
    }

    public static final class Final {
      public static final class Public {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      static final class PackagePrivate {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}

        public void publicWithPackagePrivateParameter(PackagePrivate param) {}

        public void publicWithTransitivePackagePrivateParameter(PackagePrivate.Public param) {}

        public PackagePrivate publicReturnsPackagePrivate() {
          throw new UnsupportedOperationException();
        }

        public <T extends PackagePrivate> void publicWithPackagePrivateTypeParameter(T param) {
          throw new UnsupportedOperationException();
        }

        public final class Public {}
      }

      protected static final class Protected {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }

      private static final class Private {
        public void publicMethod() {}

        void packagePrivateMethod() {}

        protected void protectedMethod() {}

        private void privateMethod() {}
      }
    }
  }
}
