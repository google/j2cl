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
 * WITHWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal.ktstubs;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

// TODO(b/235396931): These classes are here to provide implementations of kotlin.jvm.functions
//  types at runtime. Remove this file when the interfaces are directly provided by our stdlib.
final class FunctionsKt {
  /**
   * Represents a value of a functional type, such as a lambda, an anonymous function or a function
   * reference.
   *
   * @param R return type of the function.
   */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function<R> {}

  /** A function that takes 0 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function0<R> extends Function<R> {
    /** Invokes the function. */
    // TODO(dramaix): this should not be needed, investigate!
    @JsIgnore
    R invoke();
  }
  /** A function that takes 1 argument. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function1<P1, R> extends Function<R> {
    /** Invokes the function with the specified argument. */
    @JsIgnore
    R invoke(P1 p1);
  }
  /** A function that takes 2 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function2<P1, P2, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2);
  }
  /** A function that takes 3 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function3<P1, P2, P3, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3);
  }
  /** A function that takes 4 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function4<P1, P2, P3, P4, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4);
  }
  /** A function that takes 5 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function5<P1, P2, P3, P4, P5, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
  }
  /** A function that takes 6 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function6<P1, P2, P3, P4, P5, P6, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
  }
  /** A function that takes 7 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function7<P1, P2, P3, P4, P5, P6, P7, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7);
  }
  /** A function that takes 8 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function8<P1, P2, P3, P4, P5, P6, P7, P8, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8);
  }
  /** A function that takes 9 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9);
  }
  /** A function that takes 10 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10);
  }
  /** A function that takes 11 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11);
  }
  /** A function that takes 12 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12);
  }
  /** A function that takes 13 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13);
  }
  /** A function that takes 14 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14);
  }
  /** A function that takes 15 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15);
  }
  /** A function that takes 16 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16);
  }
  /** A function that takes 17 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function17<
          P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17);
  }
  /** A function that takes 18 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function18<
          P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17,
        P18 p18);
  }
  /** A function that takes 19 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function19<
          P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17,
        P18 p18,
        P19 p19);
  }
  /** A function that takes 20 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function20<
          P1,
          P2,
          P3,
          P4,
          P5,
          P6,
          P7,
          P8,
          P9,
          P10,
          P11,
          P12,
          P13,
          P14,
          P15,
          P16,
          P17,
          P18,
          P19,
          P20,
          R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17,
        P18 p18,
        P19 p19,
        P20 p20);
  }
  /** A function that takes 21 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function21<
          P1,
          P2,
          P3,
          P4,
          P5,
          P6,
          P7,
          P8,
          P9,
          P10,
          P11,
          P12,
          P13,
          P14,
          P15,
          P16,
          P17,
          P18,
          P19,
          P20,
          P21,
          R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17,
        P18 p18,
        P19 p19,
        P20 p20,
        P21 p21);
  }
  /** A function that takes 22 arguments. */
  @JsType(namespace = "kotlin.jvm.functions")
  interface Function22<
          P1,
          P2,
          P3,
          P4,
          P5,
          P6,
          P7,
          P8,
          P9,
          P10,
          P11,
          P12,
          P13,
          P14,
          P15,
          P16,
          P17,
          P18,
          P19,
          P20,
          P21,
          P22,
          R>
      extends Function<R> {
    /** Invokes the function with the specified arguments. */
    @JsIgnore
    R invoke(
        P1 p1,
        P2 p2,
        P3 p3,
        P4 p4,
        P5 p5,
        P6 p6,
        P7 p7,
        P8 p8,
        P9 p9,
        P10 p10,
        P11 p11,
        P12 p12,
        P13 p13,
        P14 p14,
        P15 p15,
        P16 p16,
        P17 p17,
        P18 p18,
        P19 p19,
        P20 p20,
        P21 p21,
        P22 p22);
  }

  /**
   * A function that takes N >= 23 arguments.
   *
   * <p>This interface must only be used in Java sources to reference a Kotlin function type with
   * more than 22 arguments.
   */
  @JsType(namespace = "kotlin.jvm.functions")
  interface FunctionN<R> extends Function<R> {
    /**
     * Invokes the function with the specified arguments.
     *
     * @param args arguments to the function
     */
    @JsIgnore
    R invoke(Object... args);
  }

  private FunctionsKt() {}
}
