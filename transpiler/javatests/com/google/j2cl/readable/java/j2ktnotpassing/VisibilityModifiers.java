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

public final class VisibilityModifiers {
  @SuppressWarnings("j2kt:visibility")
  public static class VisibilityWarningsSuppressed {
    public static class Public {
      // This one is translated as public
      protected void protectedMethod() {}
    }
  }

  public static class ProtectedOverrideOfVisibilityWarningsSuppressed
      extends VisibilityWarningsSuppressed.Public {
    // TODO(b/519538678): This one is translated as protected, but should be public because it
    // overrides a public method.
    @Override
    protected void protectedMethod() {}
  }
}
