/*
 * Copyright 2024 Google Inc.
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

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PrivateVisibility {
  public static final class InnerConstructorReference {
    private InnerConstructorReference() {}

    public static InnerConstructorReference testInnerConstructorReference() {
      return new InnerConstructorReference();
    }
  }

  public static final class OuterConstructorReference {
    private OuterConstructorReference() {}
  }

  public static OuterConstructorReference testOuterConstructorReference() {
    return new OuterConstructorReference();
  }

  public static final class Outer {
    private static int outerStaticField_outerReference;
    private static int outerStaticField_staticInnerReference;
    private static int outerStaticField_innerReference;

    private int outerField_outerReference;
    private int outerField_staticInnerReference;
    private int outerField_innerReference;

    private static void outerStaticMethod_outerReference() {}

    private static void outerStaticMethod_staticInnerReference() {}

    private static void outerStaticMethod_innerReference() {}

    private void outerMethod_outerReference() {}

    private void outerMethod_staticInnerReference() {}

    private void outerMethod_innerReference() {}

    public static final class StaticInner {
      private static int staticInnerStaticField_outerReference;
      private static int staticInnerStaticField_staticInnerReference;
      private static int staticInnerStaticField_innerReference;

      private int staticInnerField_outerReference;
      private int staticInnerField_staticInnerReference;
      private int staticInnerField_innerReference;

      private static void staticInnerStaticMethod_outerReference() {}

      private static void staticInnerStaticMethod_staticInnerReference() {}

      private static void staticInnerStaticMethod_innerReference() {}

      private void staticInnerMethod_outerReference() {}

      private void staticInnerMethod_staticInnerReference() {}

      private void staticInnerMethod_innerReference() {}

      public void testMemberReference(Outer outer, StaticInner staticInner, Inner inner) {
        outerStaticField_staticInnerReference = 0;
        outerStaticMethod_staticInnerReference();
        outer.outerField_staticInnerReference = 0;
        outer.outerMethod_staticInnerReference();

        staticInnerStaticField_staticInnerReference = 0;
        staticInnerStaticMethod_staticInnerReference();
        staticInner.staticInnerField_staticInnerReference = 0;
        staticInner.staticInnerMethod_staticInnerReference();

        // Inner.innerStaticField_staticInnerReference = 0;
        // Inner.innerStaticMethod_staticInnerReference();
        inner.innerField_staticInnerReference = 0;
        inner.innerMethod_staticInnerReference();
      }
    }

    public final class Inner {
      // TODO(b/517165652): Static members are not allowed in non-static nested classes.
      // private static int innerStaticField_outerReference;
      // private static int innerStaticField_staticInnerReference;
      // private static int innerStaticField_innerReference;

      private int innerField_outerReference;
      private int innerField_staticInnerReference;
      private int innerField_innerReference;

      // private static void innerStaticMethod_outerReference() {}

      // private static void innerStaticMethod_staticInnerReference() {}

      // private static void innerStaticMethod_innerReference() {}

      private void innerMethod_outerReference() {}

      private void innerMethod_staticInnerReference() {}

      private void innerMethod_innerReference() {}

      public void testMemberReference(Outer outer, StaticInner staticInner, Inner inner) {
        outerStaticField_innerReference = 0;
        outerStaticMethod_innerReference();
        outer.outerField_innerReference = 0;
        outer.outerMethod_innerReference();

        StaticInner.staticInnerStaticField_innerReference = 0;
        StaticInner.staticInnerStaticMethod_innerReference();
        staticInner.staticInnerField_innerReference = 0;
        staticInner.staticInnerMethod_innerReference();

        // innerStaticField_innerReference = 0;
        // innerStaticMethod_innerReference();
        inner.innerField_innerReference = 0;
        inner.innerMethod_innerReference();
      }
    }

    public void testMemberReference(Outer outer, StaticInner staticInner, Inner inner) {
      outerStaticField_outerReference = 0;
      outerStaticMethod_outerReference();
      outer.outerField_outerReference = 0;
      outer.outerMethod_outerReference();

      StaticInner.staticInnerStaticField_outerReference = 0;
      StaticInner.staticInnerStaticMethod_outerReference();
      staticInner.staticInnerField_outerReference = 0;
      staticInner.staticInnerMethod_outerReference();

      // Inner.innerStaticField_outerReference = 0;
      // Inner.innerStaticMethod_outerReference();
      inner.innerField_outerReference = 0;
      inner.innerMethod_outerReference();
    }
  }
}
