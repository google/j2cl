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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class LocalClassWithInnerClass {
  public static class Inner {
    int unrelatedOuterValue() {
      return 100;
    }
  }

  public static class GenericOuter<T> {
    public <M> Object testInGenericContext(T t, M m) {
      class Local {
        @Nullable Inner field = new Inner();

        class Inner {
          T getT() {
            return t;
          }

          M getM() {
            return m;
          }
        }

        Inner id(Inner p) {
          return p;
        }
      }

      class GenericLocal<L> {
        final L l;
        @Nullable GenericInner<M> field;

        GenericLocal(L l) {
          this.l = l;
        }

        class GenericInner<I> {
          final I i;

          GenericInner(I i) {
            this.i = i;
          }

          class DeepNested<D> {
            final D d;

            DeepNested(D d) {
              this.d = d;
            }
          }

          T getT() {
            return t;
          }

          L getL() {
            return l;
          }

          I getI() {
            return i;
          }
        }
      }

      Local local = new Local();
      Local.Inner outside = local.id(local.new Inner());
      GenericLocal<String>.GenericInner<M> outsideGeneric =
          new GenericLocal<>("a").new GenericInner<>(m);
      GenericLocal<String>.GenericInner<M>.DeepNested<T> outsideDeep =
          outsideGeneric.new DeepNested<>(t);
      return outsideDeep.d.equals(t) && outsideGeneric.getI().equals(outside.getM())
          ? outside
          : outsideGeneric;
    }

    public static <S> Object testInStaticGenericContext(S s) {
      class Local<L> {
        final L l;

        Local(L l) {
          this.l = l;
        }

        class Inner<I> {
          final I i;

          Inner(I i) {
            this.i = i;
          }
        }
      }
      Local<S>.Inner<String> instance = new Local<>(s).new Inner<>("ok");
      return instance;
    }
  }

  public static int testOutsideReferencesAndSiblings() {
    class Local1 {
      class Inner {
        int value() {
          return 1;
        }
      }
    }
    class Local2 {
      class Inner {
        int value() {
          return 2;
        }
      }
    }
    Local1.Inner first = new Local1().new Inner();
    Local2.Inner second = new Local2().new Inner();
    return first.value() + second.value();
  }

  public static int testNestedShadowing() {
    class Local {
      class A {
        int tag() {
          return 10;
        }
      }

      class Sub {
        class A {
          int tag() {
            return 20;
          }
        }

        Local.A outerA = Local.this.new A();
        A innerA = new A();
      }
    }
    Local.Sub sub = new Local().new Sub();
    return sub.outerA.tag() + sub.innerA.tag();
  }

  public static <M> int testInheritedInnerInGenericMethod(M m) {
    class Local {
      class LInner {
        M get() {
          return m;
        }

        int tag() {
          return 7;
        }
      }
    }
    class Local2 extends Local {
      LInner unqualifiedInherited = new LInner();
      Local.LInner qualifiedInherited = new LInner();
    }
    Local.LInner fromOutside = new Local2().unqualifiedInherited;
    return fromOutside.tag() + new Local2().qualifiedInherited.tag();
  }
}
