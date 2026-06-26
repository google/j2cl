# **Design Proposal: JS-implemented JsType Interfaces in J2Wasm**

**Author:** goktug@ **Status**: Proposal **Last Update:** 2026-06-11 **Bug:**
b/522930548

### 1. Objective

To support non-native `JsType` interfaces to be implementable by JavaScript and
enable calling their methods from J2Wasm.

### 2. Background

This design builds on top of
[Generalized Boundary Type Handling in J2Wasm](wasm-jsinterop-boundary.md) which
introduced `JsObject` as the runtime wrapper for native JavaScript objects.

While calling methods on objects represented as native `JsType` is already
supported, this design addresses the case where a non-native Java `JsType`
interface is implemented by JavaScript and passed into J2Wasm.

### 3. Proposed Design

J2Wasm will use adapter classes to implement `JsType` interfaces.

#### 3.1. Interface Adapters

For every non-native `JsType` interface `I`, J2Wasm generates an adapter class
`I_Adapter` that extends `JsObject` and implements `I`.

```java
// Generated adapter for interface I
class I_Adapter extends JsObject implements I {
  I_Adapter(Object jsRef) {
    super(jsRef);
  }

  @Override
  public void methodOfI(ArgType arg) {
    jsExternMethodOfI(this.getJsRef(), arg);
  }
}
```

#### 3.2. Internalization (Boundary Crossing)

When a JS object enters Wasm at a boundary where the static type is `I`, it is
wrapped in `I_Adapter`.

If the JS object enters as `java.lang.Object`, it is wrapped in the base
`JsObject`.

#### 3.3. Downcasting

When downcasting a `java.lang.Object` `o` to `I` in Wasm (`(I) o`), J2Wasm
evaluates the runtime type of `o` to handle the following scenarios:

1.  **`o` is `null`:** The cast succeeds and returns `null`.
2.  **`o` is a pure Wasm implementor of `I`:** (A Java class implementing `I`
    compiled to Wasm). The cast succeeds and returns `o` as-is.
3.  **`o` is an existing `I_Adapter`:** The cast succeeds and returns `o` as-is.
4.  **`o` is a base `JsObject`:** The cast triggers re-wrapping. A new instance
    of `I_Adapter` is created wrapping the underlying JS reference and returned.
5.  **`o` is an adapter of a different type (e.g., `Other_Adapter`):** Since
    `Other_Adapter` extends `JsObject`, it is treated as a `JsObject`. The cast
    triggers re-wrapping: a new `I_Adapter` is created wrapping the same
    underlying JS reference and returned.
6.  **`o` is any other Java object:** The cast fails and throws a
    `ClassCastException`.

The runtime logic for the downcast `(I) o` is equivalent to:

```java
if (o == null) {
  return null;
}
if (o instanceof I) {
  return (I) o; // Handles pure Wasm implementors and existing I_Adapter
} else if (o instanceof JsObject) {
  return new I_Adapter(((JsObject) o).getJsRef()); // Handles JsObject and Other_Adapter
} else {
  throw new ClassCastException();
}
```

### 4. Implications of Design Choices

#### 4.1. Upcasts

Upcasts to `JsObject` or `java.lang.Object` do not require re-wrapping. Since
`I_Adapter` extends `JsObject`, it can be treated as such directly. When flowing
back to JS, the underlying reference is extracted from the adapter in the same
way as from a base `JsObject`.

#### 4.2. Multiple Interfaces and Identity

Object identity (`==`) is not preserved when a JavaScript object is adapted to
different interfaces, or when it is re-wrapped during downcasting.

For example, `(I1) obj == (I2) obj` evaluates to `false` even if they wrap the
same JavaScript object.

Standard `equals()` will also evaluate to `false` for different adapter
instances wrapping the same JS object, as it defaults to reference equality of
the adapter wrapper. To check if two adapters refer to the same JavaScript
object, developers must compare the underlying JavaScript references directly
(e.g. via a utility method that extracts and compares the raw JS references).

#### 4.3. instanceof and Cast Checks

`instanceof` checks will be compile-time errors for `JsType` interfaces. However
depending on number of violations we can downgrade it to a "no-op" operation.

Note that we can potentially implement `instanceof` support in the future, but
it would be complicated as it requires a specific style of `instanceof`
implementation other than the current interface style (e.g. checking JS
properties/methods or using a metadata registration registry).

Casts to these interfaces are unchecked. If the underlying JavaScript object
does not implement the interface methods, a runtime error will occur when the
method is invoked.

In order to keep compatibility with J2CL/JS, we will need to add stubs for
$markImplementor in our extern adaptor to keep existing code working.
