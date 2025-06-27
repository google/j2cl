/*
 * Copyright 2019 Google Inc.
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
package jsenum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsProperty;

public class Main {
  interface Supplier<T> {
    T get();
  }

  interface Consumer<T> {
    void accept(T t);
  }

  @JsEnum
  enum ComparableJsEnum {
    ZERO,
    ONE,
    TWO;

    public int getValue() {
      return ordinal();
    }
  }

  @JsEnum(hasCustomValue = true)
  enum IntJsEnum {
    MINUSONE(-1),
    TWENTY(2 * 10),
    ELEVEN(11);

    private int value;

    public int getValue() {
      return value;
    }

    IntJsEnum(int value) {
      this.value = value;
    }
  }

  @JsEnum(hasCustomValue = true)
  enum StringJsEnum {
    ONE("ONE"),
    THREE("THREE");

    private String value;

    public String getValue() {
      return value;
    }

    StringJsEnum(String value) {
      this.value = value;
    }
  }

  @JsEnum(hasCustomValue = true)
  enum NonNullableStringJsEnum {
    ONE("ONE"),
    THREE("THREE");

    private @JsNonNull String value;

    public String getValue() {
      return value;
    }

    NonNullableStringJsEnum(String value) {
      this.value = value;
    }
  }

  @JsEnum(isNative = true, namespace = "jsenum.Main", name = "NonNullableStringJsEnum")
  enum NativeStringEnum {
    ONE,
    THREE
  }

  public static void testJsEnumSwitch() {
    ComparableJsEnum comparableJsEnum =
        ComparableJsEnum.ONE.getValue() == 1 ? ComparableJsEnum.TWO : null;
    switch (comparableJsEnum) {
      case TWO:
        break;
      default:
    }

    Comparable comparable = comparableJsEnum;
    comparableJsEnum = (ComparableJsEnum) comparable;

    IntJsEnum intJsEnum = IntJsEnum.ELEVEN.getValue() == 10 ? IntJsEnum.ELEVEN : null;
    switch (intJsEnum) {
      case TWENTY:
        break;
      default:
    }

    Object o = intJsEnum;
    intJsEnum = (IntJsEnum) o;

    // No boxing here.
    boolean equal = intJsEnum == IntJsEnum.TWENTY;
    boolean isInstance = intJsEnum instanceof IntJsEnum;

    isInstance = intJsEnum instanceof Comparable;

    StringJsEnum stringJsEnum = StringJsEnum.ONE.getValue() == "10" ? StringJsEnum.THREE : null;
    switch (stringJsEnum) {
      case ONE:
        break;
      default:
    }

    NativeStringEnum.ONE.compareTo(NativeStringEnum.THREE);
    NativeStringEnum.ONE.equals(NativeStringEnum.THREE);

    Supplier<ComparableJsEnum> supplier = () -> ComparableJsEnum.ONE;
    Consumer<ComparableJsEnum> consummer = e -> e.ordinal();

    acceptsJsFunctionSupplier(() -> ComparableJsEnum.ONE);
    acceptsSupplierOfSupplier(() -> (() -> ComparableJsEnum.ONE));
    acceptsJsFunctionParameterizedByJsEnum(e -> e);
  }

  private static void testExhaustiveJsEnumSwitchExpression() {
    ComparableJsEnum comparableJsEnum =
        ComparableJsEnum.ONE.getValue() == 1 ? ComparableJsEnum.TWO : null;
    int i =
        switch (comparableJsEnum) {
          case TWO -> 2;
          case ONE -> 1;
          case ZERO -> 0;
        };
  }

  private static void testJsEnumAutoboxingSpecialMethods() {
    StringJsEnum stringJsEnum = StringJsEnum.ONE;
    StringJsEnum nullStringJsEnum = null;
    ComparableJsEnum jsEnum = ComparableJsEnum.ONE;
    ComparableJsEnum nullJsEnum = null;
    Object o = ComparableJsEnum.ONE;

    StringJsEnum.ONE.equals(StringJsEnum.THREE);
    StringJsEnum.ONE.equals(stringJsEnum);
    StringJsEnum.ONE.equals(nullStringJsEnum);
    StringJsEnum.ONE.equals(null);
    StringJsEnum.ONE.equals(o);
    o.equals(StringJsEnum.THREE);

    ComparableJsEnum.ONE.compareTo(ComparableJsEnum.ZERO);
    ComparableJsEnum.ONE.compareTo(null);
    ComparableJsEnum.ONE.equals(ComparableJsEnum.ZERO);
    ComparableJsEnum.ONE.equals(jsEnum);
    ComparableJsEnum.ONE.equals(nullJsEnum);
    ComparableJsEnum.ONE.equals(null);
    ComparableJsEnum.ONE.equals(o);
    o.equals(ComparableJsEnum.ZERO);

    StringJsEnum.ONE.equals(jsEnum);

    boxingPassthrough(ComparableJsEnum.ONE).equals(boxingPassthrough(ComparableJsEnum.ONE));
    boxingPassthrough(ComparableJsEnum.ONE).equals(boxingPassthrough(StringJsEnum.ONE));
  }

  @JsFunction
  interface JsFunctionSuppiler<T> {
    T get();
  }

  @JsFunction
  interface SpecializedJsFunction<T> {
    T get(T value);
  }

  private static void acceptsJsFunctionSupplier(JsFunctionSuppiler<ComparableJsEnum> supplier) {}

  private static void acceptsSupplierOfSupplier(Supplier<Supplier<ComparableJsEnum>> supplier) {}

  private static void acceptsJsFunctionParameterizedByJsEnum(
      SpecializedJsFunction<ComparableJsEnum> supplier) {}

  private static void testReturnsAndParameters() {
    ComparableJsEnum returnedValue = returnsJsEnum();
    ComparableJsEnum returnedNullValue = returnsNullJsEnum();
    takesJsEnum(ComparableJsEnum.ONE);
  }

  private static ComparableJsEnum returnsJsEnum() {
    return ComparableJsEnum.ONE;
  }

  private static ComparableJsEnum returnsNullJsEnum() {
    return null;
  }

  private static void takesJsEnum(ComparableJsEnum value) {}

  private static void testBoxUnboxWithTypeInference() {
    // Make sure the enum is boxed even when assigned to a field that is inferred to be JsEnum.
    TemplatedField<ComparableJsEnum> templatedField =
        new TemplatedField<ComparableJsEnum>(ComparableJsEnum.ONE);
    ComparableJsEnum unboxed = templatedField.getValue();
    unboxed = templatedField.value;
    templatedField.value = ComparableJsEnum.ONE;
    Arrays.asList(ComparableJsEnum.ONE);
    templatedField.getValue().ordinal();
    boolean b = ComparableJsEnum.ONE == boxingPassthrough(ComparableJsEnum.ONE);
  }

  private static class TemplatedField<T> {
    T value;

    TemplatedField(T value) {
      this.value = value;
    }

    T getValue() {
      return this.value;
    }
  }

  private static <T> T boxingPassthrough(T t) {
    return t;
  }

  static void boxingWithGenerics() {
    new Foo<>(Optional.of(IntJsEnum.MINUSONE));
  }

  static class Foo<T> {
    Foo(Optional<IntJsEnum> c) {}
  }

  @JsEnum
  enum JsEnumWithRenamedProperties {
    @JsProperty(name = "NAUGHT")
    ZERO,
    ONE,
    TWO;

    public int getValue() {
      return ordinal();
    }
  }

  static class SupplierConsumerImpl<T> implements Supplier<T>, Consumer<T> {

    @Override
    public void accept(T t) {}

    @Override
    public T get() {
      return null;
    }
  }

  interface ComparableJsEnumSupplierConsumer
      extends Supplier<ComparableJsEnum>, Consumer<ComparableJsEnum> {
    @JsMethod
    ComparableJsEnum get();

    void accept(ComparableJsEnum e);
  }

  static class ComparableJsEnumSupplierConsumerImpl extends SupplierConsumerImpl<ComparableJsEnum>
      implements ComparableJsEnumSupplierConsumer {}

  static class ComparableJsEnumSupplierConsumerImplWithOverrides
      extends SupplierConsumerImpl<ComparableJsEnum> implements ComparableJsEnumSupplierConsumer {
    @Override
    public void accept(ComparableJsEnum t) {}

    @Override
    public ComparableJsEnum get() {
      return null;
    }
  }

  @JsEnum
  public enum SomeJsEnum {
    A;
  }

  private static <T> T varargsConsumer(T... args) {
    return args[0];
  }

  private static class BaseVarargs<T> {
    BaseVarargs(T... args) {}
  }

  private static class SubtypeVarargs extends BaseVarargs<SomeJsEnum> {
    SubtypeVarargs() {
      super(SomeJsEnum.A, SomeJsEnum.A);
    }
  }

  private static class SubtypeImplicitVarargs extends BaseVarargs<SomeJsEnum> {}

  private static void testVarargs() {
    varargsConsumer(SomeJsEnum.A, SomeJsEnum.A);
    Consumer<SomeJsEnum> consumer = Main::varargsConsumer;
  }

  private static void testNonNativeJsEnumArrays() {
    IntJsEnum[] arr = new IntJsEnum[] {IntJsEnum.MINUSONE, IntJsEnum.TWENTY};
    boolean b1 = arr[0] == IntJsEnum.MINUSONE;
    boolean b2 = arr[1] == IntJsEnum.TWENTY;
    Object obj = arr[0];
    IntJsEnum v = arr[0];

    IntJsEnum[] arr2 = new IntJsEnum[2];
    arr2[0] = IntJsEnum.MINUSONE;
    arr2[1] = IntJsEnum.TWENTY;

    IntJsEnum[][] nestedArr = new IntJsEnum[][] {{IntJsEnum.MINUSONE}};
    nestedArr[0] = new IntJsEnum[] {IntJsEnum.TWENTY};

    IntJsEnum[] arrayWithNull = new IntJsEnum[] {null};
    arrayWithNull[0] = null;

    List<IntJsEnum> list = new ArrayList<IntJsEnum>();
    obj = list.toArray();

    nonNativeJsEnumVarargs(IntJsEnum.MINUSONE, IntJsEnum.TWENTY);
    nonNativeJsEnumArrayVarargs(
        new IntJsEnum[] {IntJsEnum.MINUSONE}, new IntJsEnum[] {IntJsEnum.TWENTY});

    tVarargs(IntJsEnum.MINUSONE, IntJsEnum.TWENTY);
  }

  private static void nonNativeJsEnumVarargs(IntJsEnum... values) {
    IntJsEnum v = values[0];
  }

  private static void nonNativeJsEnumArrayVarargs(IntJsEnum[]... values) {
    IntJsEnum[] v = values[0];
  }

  private static <T> void tVarargs(T... values) {
    T v = values[0];
  }

  private static void testNonNativeStringJsEnumArrays() {
    StringJsEnum[] arr = new StringJsEnum[] {StringJsEnum.ONE, StringJsEnum.THREE};
    boolean b1 = arr[0] == StringJsEnum.ONE;
    Object obj = arr[0];
    StringJsEnum v = arr[0];

    StringJsEnum[] arr2 = new StringJsEnum[2];
    arr2[0] = StringJsEnum.ONE;

    StringJsEnum[][] nestedArr = new StringJsEnum[][] {{StringJsEnum.ONE}};

    StringJsEnum[] arrayWithNull = new StringJsEnum[] {null};
    arrayWithNull[0] = null;
  }

  private static void testNativeJsEnumArrays() {
    NativeStringEnum[] arr = new NativeStringEnum[] {NativeStringEnum.ONE, NativeStringEnum.THREE};
    boolean b1 = arr[0] == NativeStringEnum.ONE;

    NativeStringEnum[] arr2 = new NativeStringEnum[2];
    arr2[0] = NativeStringEnum.ONE;

    NativeStringEnum[][] nestedArr = new NativeStringEnum[][] {{NativeStringEnum.ONE}};
    nestedArr[0] = new NativeStringEnum[] {NativeStringEnum.THREE};

    NativeStringEnum[] arrayWithNull = new NativeStringEnum[] {null};
    arrayWithNull[0] = null;
  }
}
