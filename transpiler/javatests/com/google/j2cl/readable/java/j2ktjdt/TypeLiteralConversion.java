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
import org.jspecify.annotations.Nullable;

@NullMarked
public class TypeLiteralConversion {
  public interface Simple {}

  public enum SimpleEnum {}

  public interface Generic<T extends @Nullable Object> {}

  public static class Recursive<T extends Recursive<T>> {}

  public static class RecursiveSpecialized extends Recursive<RecursiveSpecialized> {}

  public Class testSimpleToRaw() {
    return Simple.class;
  }

  public Class<Simple> testSimpleToNonRaw() {
    return Simple.class;
  }

  public Class<?> testSimpleToUnboundWildcard() {
    return Simple.class;
  }

  public Class<? extends Simple> testSimpleBoundWildcard() {
    return Simple.class;
  }

  public Class testGenericToRaw() {
    return Generic.class;
  }

  public Class<Generic> testGenericToNonRaw() {
    return Generic.class;
  }

  public Class<?> testGenericToUnboundWildcard() {
    return Generic.class;
  }

  public Class<? extends Generic> testGenericToBoundWildcard() {
    return Generic.class;
  }

  public Class testPrimitiveToRaw() {
    return int.class;
  }

  public Class<Integer> testPrimitiveToNonRaw() {
    return int.class;
  }

  public Class<?> testPrimitiveToUnboundWildcard() {
    return int.class;
  }

  public Class<? extends Number> testPrimitiveToBoundWildcard() {
    return int.class;
  }

  public Class testArrayToRaw() {
    return String[].class;
  }

  public Class<String[]> testArrayToNonRaw() {
    return String[].class;
  }

  public Class<?> testArrayToUnboundWildcard() {
    return String[].class;
  }

  public Class testPrimitiveArrayToRaw() {
    return int[].class;
  }

  public Class<int[]> testPrimitiveArrayToNonRaw() {
    return int[].class;
  }

  public Class<?> testPrimitiveArrayToUnboundWildcard() {
    return int[].class;
  }

  public static Simple testGetSimpleInstance() {
    return getInstance(Simple.class);
  }

  public static Integer testGetIntegerInstance() {
    return getInstance(Integer.class);
  }

  public static int testGetPrimitiveInstance() {
    return getInstance(Integer.TYPE);
  }

  public static String[] testGetArrayInstance() {
    return getInstance(String[].class);
  }

  public static SimpleEnum testGetSimpleEnumInstance() {
    return getInstance(SimpleEnum.class);
  }

  public static Recursive<?> testGetRecursiveInstance() {
    return getInstance(Recursive.class);
  }

  public static RecursiveSpecialized testGetRecursiveSpecializedInstance() {
    return getInstance(RecursiveSpecialized.class);
  }

  public static <T extends @Nullable Object> T getInstance(Class<T> clazz) {
    throw new AssertionError();
  }
}
