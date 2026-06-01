# **Design Proposal: Generalized Boundary Type Handling in J2Wasm**

**Author:** goktug@, duckie@ **Status**: Proposal **Last Update:** 2026-05-20
**Bug:** b/515144544

### 1. Objective

To generalize the type handling mechanism at the J2Wasm-JavaScript boundary to
transparently and correctly manage:

1.  Wasm types that have specific JavaScript mappings (e.g., `java.lang.String`,
    `java.lang.Double`).
2.  Native JavaScript types.
3.  The `java.lang.Object` type, enabling it to encompass both special mapped
    types and native JS types.

### 2. Background

J2Wasm applications need to interact with JavaScript. This interoperation occurs
at a boundary where types must be converted between the Wasm heap and the JS
heap. Currently, some type conversions are handled (e.g., partially for
`String`), but the system needs to be more comprehensive to cover types like
`Double`, the general `Object`, and native JS types, especially within generic
containers (e.g., `Collection<T>`).

### 3. Problems

*   Inconsistent handling of types with special JS mappings (e.g., `String` is
    partially handled, `Double` is missing).
*   `java.lang.Object` on the boundary doesn't transparently handle subtypes
    with special mappings or native JS objects.
*   Native JS types cannot easily be represented within the Wasm type system
    when crossing the boundary, particularly in generic contexts.

### 4. Proposed Solution

The core idea is to intercept calls at the J2Wasm/JS boundary and inject custom
type conversion logic based on the method signatures.

1.  **Boundary Call Interception:** J2Wasm will intercept method calls and
    returns crossing the boundary to internalize JS values into Wasm and
    externalize Wasm values to JS.
2.  **Type-Specific Conversions:** Instead of generic Wasm `externref` handling,
    the interception layer will invoke type-specific conversion functions:
    *   For `java.lang.String`: Use existing `String.toJs` and `String.fromJs`
        methods consistently at all boundary points.
    *   For `java.lang.Double`: Introduce and use new `Double.toJs` and
        `Double.fromJs` methods to handle boxing/unboxing to JS `number`.
3.  **Generalized `java.lang.Object` Handling:**
    *   At the boundary, `java.lang.Object` will map to the JavaScript type
        `any`.
    *   Implement `Object.toJs` and `Object.fromJs` methods. These methods will
        dynamically check the runtime type and delegate to the specific
        conversion logic if available (e.g., call `String.toJs` if the object is
        a `String`).
4.  **Wrapping Native JS Types:**
    *   To allow native JS objects to be treated as `java.lang.Object` within
        Wasm, introduce an internal `JsObject` wrapper type in the J2Wasm
        runtime.
    *   `Object.fromJs`: When a non-null JS value is received that doesn't
        correspond to a known Java type with special handling, wrap it in a
        `JsObject` instance.
    *   `Object.toJs`: If an object is an instance of `JsObject`, unwrap it to
        return the original native JS object.
5.  **Implications for Generics:** This approach allows collections like
    `Collection<Object>` to seamlessly contain Java Strings, Doubles, other Java
    objects, and wrapped native JS types.

Note that native JsFunctions cannot be bridged at this level since we cannot
generically adapt them. They will need to be handled like other native types.

#### 4.1. `JsObject` Wrapper Considerations

*   **Internal:** Kept as a runtime-internal mechanism. This is friendlier for
    migration and developer experience, as wrapping/unwrapping is automatic at
    the boundary for `Object` types. Auto-unwrapping could potentially be added
    for casts.
*   **Public:** Exposing `JsObject` gives developers explicit control but breaks
    J2CL/JS source compatibility, requiring manual code changes and dummy
    implementations in the J2CL/JS runtime.

The current leaning is towards an **internal `JsObject`** with automatic
wrapping/unwrapping at the boundary when the static type is `java.lang.Object`,
supplemented by potential interceptions for casts.

### 5. Benefits

*   Correct and consistent handling of types like `String` and `Double` across
    the JS/Wasm boundary.
*   Enables native JS objects to be passed into Wasm and held as
    `java.lang.Object`.
*   Improves the usability of generic types containing mixed Java and JS
    objects.
*   Paves the way for mapping `java.lang.Object` to `any` at the boundary, which
    is necessary for full interop.
