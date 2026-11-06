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

import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AnonymousClassWithInnerClass {
  int OuterFieldCollision = 0;

  public static Object testBasicTypePositions(Object unknown) {
    return new Object() {
      @Nullable Inner nullableField;
      Inner initializedField = new Inner(1);
      @Nullable List<Inner> listField;
      @Nullable List<? extends Inner> covariantListField;
      @Nullable List<? super Inner> contravariantListField;
      Inner @Nullable [] arrayField;

      int anonHelper() {
        return 10;
      }

      class Inner {
        final int value;

        Inner(int value) {
          this.value = value;
        }

        int selfValueViaThis() {
          return Inner.this.value;
        }

        Inner selfViaThis() {
          return Inner.this;
        }

        int valuePlusAnonHelper() {
          return value + anonHelper();
        }
      }

      int localVariablesAndInlineAccess() {
        Inner local = new Inner(5);
        return local.selfValueViaThis() + local.valuePlusAnonHelper() + new Inner(6).value;
      }

      Inner acceptAndReturn(Inner param) {
        final Inner nonNullLocal = initializedField;
        final @Nullable Inner nullableLocal = nullableField;
        nullableField = param;
        if (unknown instanceof Inner patterned && patterned.selfViaThis() == patterned) {
          Inner casted = (Inner) unknown;
          return casted.selfViaThis();
        }
        return nullableLocal != null ? nullableLocal : nonNullLocal;
      }

      <U extends Inner & Runnable> U acceptIntersection(U param) {
        param.run();
        return param;
      }

      <Inner> int shadowWithMethodTypeParam(Inner param) {
        var inferredLocal = this.new Inner(1);
        var inferredFromField = initializedField;
        return inferredLocal.valuePlusAnonHelper()
            + inferredFromField.valuePlusAnonHelper()
            + param.hashCode();
      }
    };
  }

  public static Iterator<String> testStatefulIteratorPattern(String item) {
    return new Iterator<String>() {
      private boolean yielded;
      private @Nullable EntryHolder toRemove;

      class EntryHolder {
        final String value;

        EntryHolder(String value) {
          this.value = value;
        }
      }

      @Override
      public boolean hasNext() {
        return !yielded;
      }

      @Override
      public String next() {
        yielded = true;
        EntryHolder entry = new EntryHolder(item);
        toRemove = entry;
        return entry.value;
      }

      @Override
      public void remove() {
        final @Nullable EntryHolder e = toRemove;
        toRemove = null;
        if (e != null && e.value.isEmpty()) {
          throw new IllegalStateException();
        }
      }
    };
  }

  public static class GenericOuter<T> {
    public <M> Object testGenericsAndNesting(T t, M m) {
      return new Object() {
        @Nullable Inner innerField = new Inner();
        GenericInner<String> genericString = new GenericInner<>("ok");
        GenericInner<M> genericM = new GenericInner<>(m);
        Base<T> baseField = new Base<>(t);
        Base<T>.Nested<M> nestedField = baseField.new Nested<>(m);
        Base<String>.Nested<Integer> concreteNestedField = new Base<>("s").new Nested<>(1);

        class Inner {
          T getT() {
            return t;
          }

          M getM() {
            return m;
          }
        }

        class GenericInner<U> {
          final U u;

          GenericInner(U u) {
            this.u = u;
          }

          T getT() {
            return t;
          }

          M getM() {
            return m;
          }
        }

        <N> GenericInner<N> box(N n) {
          return new GenericInner<>(n);
        }

        class Base<A> {
          final A a;

          Base(A a) {
            this.a = a;
          }

          class Nested<B> {
            final B b;

            Nested(B b) {
              this.b = b;
            }

            A outerA() {
              return a;
            }

            Nested<B> self() {
              return this;
            }
          }

          Nested<M> createNested(M b) {
            return new Nested<>(b);
          }
        }

        boolean localGenericAndNestedAccess() {
          Inner localInner = new Inner();
          GenericInner<M> localGeneric = new GenericInner<>(m);
          Base<T>.Nested<M> localNested = new Base<>(t).new Nested<>(m);
          return localInner.getT().equals(localNested.outerA())
              && localGeneric.getM().equals(localNested.b);
        }

        class Sibling {
          @Nullable Inner fromAnonField = innerField;
          Base<T>.Nested<M> fromNested = nestedField;

          T fromLocalInner() {
            Inner local = new Inner();
            return local.getT();
          }
        }

        Object fromNestedScopes() {
          class LocalInsideAnon {
            @Nullable Inner captured = innerField;
            GenericInner<T> created = new GenericInner<>(t);

            T captureViaLocal() {
              GenericInner<T> localCreated = new GenericInner<>(t);
              return localCreated.u;
            }
          }
          return new Object() {
            @Nullable Inner outerInner = new LocalInsideAnon().captured;

            class DeeperInner {
              @Nullable Inner getOuterInner() {
                return outerInner;
              }

              T getCapturedT() {
                return new LocalInsideAnon().captureViaLocal();
              }
            }

            DeeperInner deeper = new DeeperInner();

            T runDeeper() {
              DeeperInner localDeeper = new DeeperInner();
              return localDeeper.getCapturedT();
            }
          };
        }
      };
    }

    public static <S> Object testStaticGeneric(S s) {
      return new Object() {
        StaticContextInner<S> field = new StaticContextInner<>(s);

        S viaLocal() {
          StaticContextInner<S> local = new StaticContextInner<>(s);
          return local.u;
        }

        class StaticContextInner<U> {
          final U u;

          StaticContextInner(U u) {
            this.u = u;
          }
        }
      };
    }
  }

  public static class OuterShadowTarget {}

  public <ConflictWithTypeParam> Object testNameCollisions(
      int ConflictWithParam, @Nullable ConflictWithTypeParam unused) {
    int ConflictWithLocal = OuterFieldCollision;
    OuterShadowTarget shadowSuper =
        new OuterShadowTarget() {
          OuterShadowTarget selfShadowed = new OuterShadowTarget();

          Object createSelfShadowed() {
            OuterShadowTarget local = new OuterShadowTarget();
            return local;
          }

          class OuterShadowTarget {}
        };
    return new Object() {
      int ConflictWithField = ConflictWithParam + ConflictWithLocal + shadowSuper.hashCode();

      @Nullable Companion companionField = new Companion();
      @Nullable ConflictWithField fieldCollision = new ConflictWithField();
      @Nullable OuterFieldCollision outerFieldCollision = new OuterFieldCollision();
      @Nullable ConflictWithParam paramCollision = new ConflictWithParam();
      @Nullable ConflictWithLocal localCollision = new ConflictWithLocal();
      @Nullable ConflictWithTypeParam typeParamCollision = new ConflictWithTypeParam();
      A anonA = new A();
      NestedShadow.A qualifiedNestedA = new NestedShadow().new A();

      int instantiateAllAsLocals() {
        Companion c = new Companion();
        ConflictWithField f = new ConflictWithField();
        OuterFieldCollision o = new OuterFieldCollision();
        ConflictWithParam p = new ConflictWithParam();
        ConflictWithLocal l = new ConflictWithLocal();
        ConflictWithTypeParam tp = new ConflictWithTypeParam();
        A a = new A();
        NestedShadow.A na = new NestedShadow().new A();
        return c.hashCode()
            + f.hashCode()
            + o.hashCode()
            + p.hashCode()
            + l.hashCode()
            + tp.hashCode()
            + a.hashCode()
            + na.hashCode()
            + ConflictWithField;
      }

      class Companion {}

      class ConflictWithField {}

      class OuterFieldCollision {}

      class ConflictWithParam {}

      class ConflictWithLocal {}

      class ConflictWithTypeParam {}

      class A {}

      class NestedShadow {
        A nestedA = new A();

        Object createNestedA() {
          A localNestedA = new A();
          return localNestedA;
        }

        class A {}
      }
    };
  }

  public static Object testVisibilityModifiers() {
    return new Object() {
      public PublicInner publicField = new PublicInner();
      protected ProtectedInner protectedField = new ProtectedInner();
      PackagePrivateInner packageField = new PackagePrivateInner();
      private PrivateInner privateField = new PrivateInner();
      public PackagePrivateInner publicExposingPackagePrivate = new PackagePrivateInner();

      PrivateInner packageExposingPrivate() {
        return privateField;
      }

      int instantiateAllVisibilities() {
        PublicInner pub = new PublicInner();
        ProtectedInner prot = new ProtectedInner();
        PackagePrivateInner pkg = new PackagePrivateInner();
        PrivateInner priv = new PrivateInner();
        return pub.hashCode() + prot.hashCode() + pkg.hashCode() + priv.hashCode();
      }

      public class PublicInner {}

      protected class ProtectedInner {
        ProtectedInner() {}
      }

      class PackagePrivateInner {}

      private class PrivateInner {}
    };
  }

  public static int testEscapingViaVar() {
    int directInline =
        new Object() {
          class DirectInner {
            int value() {
              return 10;
            }
          }

          int computeInline() {
            return new DirectInner().value();
          }
        }.computeInline();
    var anon =
        new Object() {
          class EscapingInner {
            int value() {
              return 30;
            }
          }

          EscapingInner get() {
            return new EscapingInner();
          }
        };
    var escaped = anon.get();
    int inlineResult =
        new Object() {
          class InlineInner {
            int value() {
              return 2;
            }
          }

          InlineInner get() {
            return new InlineInner();
          }
        }.get().value();
    return directInline + escaped.value() + inlineResult;
  }
}
