/*
 * Copyright 2022 Google Inc.
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
package javakotlininterop;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.TestUtils.getUndefined;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;
import static javakotlininterop.KotlinClassKt.testJavaKotlinMixedHierarchyFromKotlinWithExplicitOverride;
import static javakotlininterop.KotlinClassKt.testJavaKotlinMixedHierarchyFromKotlinWithoutExplicitOverride;

import javakotlininterop.FromJava.AbstractConsumer;
import javakotlininterop.FromJava.Consumer;
import javakotlininterop.FromJava.TConsumer;

/** Test java calls kotlin interop */
public class Main {
  public static void main(String... args) {
    testTopLevelDeclarations();
    testClassMembers();
    testCompanionObject();
    testObjectDeclaration();
    testJavaKotlinMixedHierarchyFromJavaWithExplicitOverride();
    testJavaKotlinMixedHierarchyFromJavaWithoutExplicitOverride();
    testJavaKotlinMixedHierarchyFromKotlinWithExplicitOverride();
    testJavaKotlinMixedHierarchyFromKotlinWithoutExplicitOverride();
    testDefaultArguments();
  }

  private static void testTopLevelDeclarations() {
    assertEquals(0, KotlinClassKt.getTopLevelProperty());
    KotlinClassKt.setTopLevelProperty(10);
    assertEquals(10, KotlinClassKt.getTopLevelProperty());
    assertEquals(1, KotlinClassKt.topLevelFunction());
  }

  private static void testClassMembers() {
    KotlinClass ktClass = new KotlinClass();
    assertEquals(2, ktClass.getInstanceProperty());
    ktClass.setInstanceProperty(20);
    assertEquals(20, ktClass.getInstanceProperty());
    assertEquals(3, ktClass.instanceFunction());
  }

  private static void testCompanionObject() {
    assertEquals(1, WithCompanion.Companion.getNonJvmStaticProperty());
    assertEquals(2, WithCompanion.Companion.getJvmStaticProperty());
    assertEquals(2, WithCompanion.getJvmStaticProperty());
    assertEquals(3, WithCompanion.Companion.nonJvmStaticFunction());
    assertEquals(4, WithCompanion.Companion.jvmStaticFunction());
    assertEquals(4, WithCompanion.jvmStaticFunction());
    // TODO(b/232172836): Enable when constant of companion object are converted to static final
    //  field of the enclosing class.
    // assertEquals(10, WithCompanion.CONSTANT);

    assertEquals(5, InterfaceWithCompanion.Companion.nonJvmStaticMethod());
    assertEquals(6, InterfaceWithCompanion.Companion.jvmStaticMethod());
    assertEquals(6, InterfaceWithCompanion.jvmStaticMethod());
  }

  private static void testObjectDeclaration() {
    assertEquals(1, KotlinObject.INSTANCE.getNonJvmStaticProperty());
    assertEquals(2, KotlinObject.getJvmStaticProperty());
    assertEquals(3, KotlinObject.INSTANCE.nonJvmStaticFunction());
    assertEquals(4, KotlinObject.jvmStaticFunction());
  }

  private static void testJavaKotlinMixedHierarchyFromJavaWithExplicitOverride() {
    assertEquals(
        "KotlinExtendsJavaTConsumerWithOverride.consume(int)",
        new KotlinExtendsJavaTConsumerWithOverride().consume(1));
    assertEquals(
        "KotlinExtendsJavaTConsumerWithOverride.consume(int)",
        new KotlinExtendsJavaTConsumerWithOverride().consume(Integer.valueOf(1)));

    TConsumer<Integer> t = new KotlinExtendsJavaTConsumerWithOverride();
    assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", t.consume(1));
    assertEquals(
        "KotlinExtendsJavaTConsumerWithOverride.consume(int)", t.consume(Integer.valueOf(1)));

    // All the dispatches down here with primitive parameters will do boxing and call the
    // reference parameter version which is bridged in the class.
    AbstractConsumer<Integer> a = new KotlinExtendsJavaTConsumerWithOverride();
    assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", a.consume(1));
    assertEquals(
        "KotlinExtendsJavaTConsumerWithOverride.consume(int)", a.consume(Integer.valueOf(1)));

    Consumer<Integer> c = new KotlinExtendsJavaTConsumerWithOverride();
    assertEquals("KotlinExtendsJavaTConsumerWithOverride.consume(int)", c.consume(1));
    assertEquals(
        "KotlinExtendsJavaTConsumerWithOverride.consume(int)", c.consume(Integer.valueOf(1)));
  }

  private static void testJavaKotlinMixedHierarchyFromJavaWithoutExplicitOverride() {
    assertEquals(
        "TConsumer.consume(int)", new KotlinExtendsJavaTConsumerWithoutOverride().consume(1));
    if (isJvm()) {
      // TODO(b/262430319): Decide what to do with the quirky kotlin semantics for overrides.

      // Note since the kotlin class did not implement the method the two super methods are not
      // bridged and both are preserved. In the J2CL model the subclass collapses the two methods
      // together.
      assertEquals(
          "TConsumer.consume(T)",
          new KotlinExtendsJavaTConsumerWithoutOverride().consume(Integer.valueOf(1)));
    }

    TConsumer<Integer> t = new KotlinExtendsJavaTConsumerWithoutOverride();
    assertEquals("TConsumer.consume(int)", t.consume(1));
    if (isJvm()) {
      // TODO(b/262430319): Decide what to do with the quirky kotlin semantics for overrides.

      // Note since the kotlin class did not implement the method the two super methods are not
      // bridged and both are preserved. In the J2CL model the subclass collapses the two methods
      // together.
      assertEquals("TConsumer.consume(T)", t.consume(Integer.valueOf(1)));

      AbstractConsumer<Integer> a = new KotlinExtendsJavaTConsumerWithoutOverride();
      assertEquals("TConsumer.consume(T)", a.consume(1));
      assertEquals("TConsumer.consume(T)", a.consume(Integer.valueOf(1)));

      Consumer<Integer> c = new KotlinExtendsJavaTConsumerWithoutOverride();
      assertEquals("TConsumer.consume(T)", c.consume(1));
      assertEquals("TConsumer.consume(T)", c.consume(Integer.valueOf(1)));
    }
  }

  private static class SubtypeWithParentDefaultArguments extends SuperTypeWithOptionalParams {
    SubtypeWithParentDefaultArguments() {
      super("overridden");
    }

    // The generated @JvmOverloads from the supertype are final and delegate to this.
    @Override
    public String openFunWithDefaults(String str) {
      return "overridden: " + super.openFunWithDefaults(str);
    }
  }

  private static void testDefaultArguments() {
    SuperTypeWithOptionalParams kotlinType;

    // TODO(b/433766093): Uncomment when we implicitly generate this constructor overload.
    // kotlinType = new SuperTypeWithOptionalParams();
    // assertEquals("defaulted", kotlinType.getX());

    kotlinType = new SuperTypeWithOptionalParams("foo");
    assertEquals("foo", kotlinType.getStr());

    kotlinType = new SuperTypeWithOptionalParams(getUndefined());
    assertEquals("null", kotlinType.getStr());

    // TODO(b/433768885): Uncomment when @JvmOverloads is supported.
    // assertEquals("defaulted", kotlinType.funWithDefaults());
    assertEquals("foo", kotlinType.funWithDefaults("foo"));
    assertEquals("null", kotlinType.funWithDefaults(getUndefined()));

    // TODO(b/433768885): Uncomment when @JvmOverloads is supported.
    // assertEquals("defaulted", kotlinType.openFunWithDefaults());
    assertEquals("foo", kotlinType.openFunWithDefaults("foo"));
    assertEquals("null", kotlinType.openFunWithDefaults(getUndefined()));

    SubtypeWithParentDefaultArguments javaType = new SubtypeWithParentDefaultArguments();
    assertEquals("overridden", javaType.getStr());

    // TODO(b/433768885): Uncomment when @JvmOverloads is supported.
    // assertEquals("defaulted", javaType.funWithDefaults());
    assertEquals("foo", javaType.funWithDefaults("foo"));
    assertEquals("null", javaType.funWithDefaults(getUndefined()));

    // TODO(b/433768885): Uncomment when @JvmOverloads is supported.
    // assertEquals("overridden: defaulted", javaType.openFunWithDefaults());
    assertEquals("overridden: foo", javaType.openFunWithDefaults("foo"));
    assertEquals("overridden: null", javaType.openFunWithDefaults(getUndefined()));
  }
}
