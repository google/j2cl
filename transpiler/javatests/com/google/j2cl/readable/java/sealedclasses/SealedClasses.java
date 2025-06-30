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

package sealedclasses;

import sealedclasses.sealedclassinanotherpackage.SealedClassInAnotherPackage;

public class SealedClasses {
  public static final class SubClass extends SealedClassInAnotherPackage {}

  public sealed interface Animal {}

  public abstract static sealed class Mammal implements Animal {
    public Mammal() {}

    private Mammal(String name) {
      this();
    }

    public String retrieveName() {
      return "Animal";
    }

    public abstract void changeName(String updatedName);

    // Subclasses can have any visibility
    protected static final class Dolphin extends Mammal {
      @Override
      public void changeName(String updatedName) {}
    }
  }

  public static final class Dog extends Mammal {
    @Override
    public void changeName(String updatedName) {}
  }

  static final class Cat extends Mammal {
    @Override
    public void changeName(String updatedName) {}
  }

  private static final class Whale extends Mammal {
    @Override
    public void changeName(String updatedName) {}
  }

  public static final class Deer implements Animal {
    private Deer() {}
  }

  // Enum classes cannot extend a sealed class, but they can implement sealed interfaces.
  enum EnumImplementsSealedInterface implements Animal {
    V1,
    V2
  }

  public abstract static sealed class Shape {
    public static final class Circle extends Shape {}

    public static final class Rectangle extends Shape {
      private Rectangle() {}
    }
  }

  // The else branch is not mandatory in when expression if when has a subject of sealed type
  // TODO(b/286448526): Implement pattern matching switch.
  // private static String checkShape(Shape e) {
  //   return switch (e) {
  //     case Shape.Circle c -> "Circle";
  //     case Shape.Rectangle r -> "Rectangle";
  //   };
  // }

  // Unlike Kotlin, sealed classes in Java can be concrete and subtypes can be inner classes.
  public static sealed class SealedClassWithInnerClass {
    public final class A extends SealedClassWithInnerClass {}

    public final class B extends SealedClassWithInnerClass {}
  }
}
