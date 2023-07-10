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
package nativekttypes;

import javaemul.internal.annotations.KtDisabled;

public class Main {
  public NativeTopLevel<String> topLevelField;
  public NativeTopLevel.Nested<String> nestedField;
  public NativeTopLevel<String>.Inner<String> innerField;

  public void methodArguments(
      NativeTopLevel<String> foo,
      NativeTopLevel.Nested<String> nested,
      NativeTopLevel<String>.Inner<String> inner) {}

  public void memberAccess() {
    NativeTopLevel<String> topLevel = new NativeTopLevel<>("foo");
    String fooInstanceMethod = topLevel.instanceMethod("foo");
    String fooStaticMethod = topLevel.staticMethod("foo");
    String fooInstanceField = topLevel.instanceField;
    topLevel.instanceField = "foo";
    Object fooStaticField = topLevel.staticField;
    topLevel.staticField = "foo";
    int i1 = topLevel.fieldToRename;
    int i2 = topLevel.methodToRename();
    int i3 = topLevel.getMethodAsProperty();
    int i4 = topLevel.nonGetMethodAsProperty();
    int i5 = topLevel.methodToRenameAsProperty();
    boolean i6 = topLevel.isFieldToRename;
    boolean i7 = topLevel.isMethodAsProperty();
    int i8 = topLevel.getstartingmethodAsProperty();

    NativeTopLevel.Nested<String> nested = new NativeTopLevel.Nested<>("foo");
    String nestedInstanceMethod = nested.instanceMethod("foo");
    String nestedStaticMethod = nested.staticMethod("foo");
    String nestedInstanceField = nested.instanceField;
    nested.instanceField = "foo";
    Object nestedStaticField = nested.staticField;
    nested.staticField = "foo";

    NativeTopLevel.Nested<String> nestedAnonynous = new NativeTopLevel.Nested<>("foo") {};

    NativeTopLevel<String>.Inner<String> inner = topLevel.new Inner<String>("foo");

    Subclass<String> subclass = new Subclass<>("foo");
    int i9 = subclass.methodToRename();
    int i10 = subclass.interfaceMethod("foo");
    int i11 = subclass.interfaceMethodToRename("foo");

    NativeInterface.NativeFunctionalInterface interfaceAnonymousSubclass =
        new NativeInterface.NativeFunctionalInterface() {
          public void run() {}
        };

    NativeInterface.NativeFunctionalInterface interfaceExpression =
        () -> {
          KFunctionalInterface: // Test name collision of the label.
          {
            return; // Test pointing to right type in return.
          }
        };
  }

  public void bridges() {
    NativeRequiringBridge o = new NativeRequiringBridge();
    o = new NativeRequiringBridge() {};
    o = new BridgeSubclass();

    NativeFunctionalInterfaceRequiringBridge<String> fi;
    fi = s -> s;
    fi =
        new NativeFunctionalInterfaceRequiringBridge<String>() {
          @Override
          public String foo(String s) {
            return s;
          }
        };

    NativeRequiringBridge cast = (NativeRequiringBridge) o;
    boolean instanceofCheck = o instanceof NativeRequiringBridge;
  }

  public void casts() {
    NativeTopLevel<String> o1 = (NativeTopLevel<String>) null;
    NativeTopLevel.Nested<String> o2 = (NativeTopLevel.Nested<String>) null;
    NativeTopLevel<String>.Inner<String> o3 = (NativeTopLevel<String>.Inner<String>) null;
  }

  public void companionObject() {
    NativeWithCompanionObject o = new NativeWithCompanionObject();

    int i1 = o.instanceField;
    o.instanceMethod();

    NativeWithCompanionObject.staticMethod();
    int i2 = NativeWithCompanionObject.staticField;
    NativeWithCompanionObject.staticField = i2;
  }

  public void typeLiterals() {
    Class<?> c1 = NativeTopLevel.class;
    Class<?> c2 = NativeTopLevel.Nested.class;
    Class<?> c3 = NativeTopLevel.Inner.class;
  }

  @KtDisabled
  public void disabledVoidMethod() {
    int i = 0;
  }

  @KtDisabled
  public boolean disabledNonVoidMethod() {
    return true;
  }

  public <I, O> O acceptFn_generic(Fn<I, O> f, I i) {
    return f.apply(i);
  }

  public String acceptFn_parametrized(Fn<String, String> f, String i) {
    return f.apply(i);
  }

  public <I, O> O acceptFn_genericWildcard(Fn<? super I, ? extends O> f, I i) {
    return f.apply(i);
  }

  public String acceptFn_parametrizedWildcard(Fn<? super String, ? extends String> f, String i) {
    return f.apply(i);
  }

  public Object acceptFn_unboundWildcard(Fn<?, ?> f, Object i) {
    return ((Fn<Object, Object>) f).apply(i);
  }

  public Object acceptFn_raw(Fn f, Object i) {
    return f.apply(i);
  }
}

class Subclass<V> extends NativeTopLevel<V> implements NativeInterface<V> {
  Subclass(V v) {
    super(v);
  }

  @Override
  public int methodToRename() {
    return super.methodToRename();
  }

  @Override
  public int getMethodAsProperty() {
    return super.getMethodAsProperty();
  }

  @Override
  public int nonGetMethodAsProperty() {
    return super.nonGetMethodAsProperty();
  }

  @Override
  public int methodToRenameAsProperty() {
    return super.methodToRenameAsProperty();
  }

  @Override
  public int getMethodToRenameAsProperty() {
    return super.getMethodToRenameAsProperty();
  }

  @Override
  public boolean isMethodAsProperty() {
    return super.isMethodAsProperty();
  }

  @Override
  public int getstartingmethodAsProperty() {
    return super.getstartingmethodAsProperty();
  }

  @Override
  public int interfaceMethod(V v) {
    return 0;
  }

  @Override
  public int interfaceMethodToRename(V v) {
    return 0;
  }

  @Override
  public int getInterfaceMethodAsProperty() {
    return 0;
  }

  @Override
  public int interfaceMethodToRenameAsProperty() {
    return 0;
  }
}

class Subsubclass<V> extends Subclass<V> {
  Subsubclass(V v) {
    super(v);
  }

  @Override
  public int methodToRename() {
    return super.methodToRename();
  }

  @Override
  public int getMethodAsProperty() {
    return super.getMethodAsProperty();
  }

  @Override
  public int nonGetMethodAsProperty() {
    return super.nonGetMethodAsProperty();
  }

  @Override
  public int methodToRenameAsProperty() {
    return super.methodToRenameAsProperty();
  }

  @Override
  public int getMethodToRenameAsProperty() {
    return super.getMethodToRenameAsProperty();
  }

  @Override
  public boolean isMethodAsProperty() {
    return super.isMethodAsProperty();
  }

  @Override
  public int getstartingmethodAsProperty() {
    return super.getstartingmethodAsProperty();
  }

  @Override
  public int interfaceMethod(V v) {
    return 0;
  }

  @Override
  public int interfaceMethodToRename(V v) {
    return 0;
  }

  @Override
  public int getInterfaceMethodAsProperty() {
    return 0;
  }

  @Override
  public int interfaceMethodToRenameAsProperty() {
    return 0;
  }
}

class BridgeSubclass extends NativeRequiringBridge {
  @Override
  public void method() {
    super.method();
  }
}
