# **Design Proposal: JsInterop Semantics for Java Records in J2CL**

**Author:** goktug@, duckie@ **Status**: Draft **Last Update:** 2026-03-25
**Self-link:** (e.g., go/j2cl-records-jsinterop)

### 1. Objective

To define and implement JsInterop semantics for Java Record types within the
J2CL transpiler, enabling seamless usage of Java Records from JavaScript. This
includes lifting the current restriction preventing `@JsType` on Records
(b/470146353), providing a clear migration path for existing `@AutoValue`
classes that use `@JsType`, and ensuring a coherent interoperability story with
Kotlin and J2KT.

### 2. Background

Java Records, introduced in Java 16, offer a concise syntax for creating
immutable data-holding classes, superceding much of the need for `@AutoValue`
(go/autovalue/records). As Google modernizes its Java codebase, migrating
`@AutoValue` classes to Records is becoming common.

Many existing `@AutoValue` classes are already exposed to JavaScript via J2CL
and JsInterop annotations like `@JsType`. Currently, J2CL disallows `@JsType` on
Records because the JavaScript contract for such types has not been defined.
This proposal aims to specify these semantics. A key consideration is ensuring
backward compatibility for JavaScript code consuming `@AutoValue` classes that
are refactored into Records, as highlighted in b/470146353#comment7.

Furthermore, J2KT translates Java Records to Kotlin Data Classes. To prevent ABI
breakages across Java, Kotlin, and JavaScript, the JsInterop design for Records
must be coherent with how Kotlin data types are exposed to JavaScript in J2CL.

### 3. Design

General idea:

-   As before `@JsType` is a shortcut to apply corresponding jsinterop
    annotations for the public members of the class.
-   For records this includes component accessors.
-   Component accessors - unlike methods in regular classes - are exposed as
    `@JsProperty`.
-   Component accessors' annotations are defined on the component itself.
-   Everything else continues to follow regular jsinterop rules/semantics.

The rest of the document is built upon this basic set of principles.

#### 3.1. Default Behavior: Components as JavaScript Properties

When a Java Record is annotated with `@JsType`:

1.  **Components to JS Properties:** Each public record component defined in the
    record header (e.g., `String name` in `record MyRecord(String name, int
    value)`) will be exposed as a read-only JavaScript property with the same
    name (e.g., `instance.name`, `instance.value`). This aligns with the
    immutable nature of Record components and idiomatic JavaScript object
    property access.
2.  **Constructor:** The **public** canonical constructor of the Record will be
    exposed as a JavaScript constructor, as if annotated with `@JsConstructor`
    (e.g., `new my.package.MyRecord("test", 123)`). Similar to regular classes:
    *   If the canonical constructor is not public, it will not be exposed.
    *   Non-canonical, user-defined constructors must be non-public or
        explicitly annotated with `@JsIgnore`.
3.  **Methods:** Methods will follow regular JsType semantics.
    *   Public instance methods explicitly defined within the Record's body
        `{...}` will be exposed as JavaScript methods, as if annotated with
        `@JsMethod` (e.g., `instance.myCustomMethod()`).
    *   The compiler-generated `equals()`, `hashCode()`, and `toString()`
        methods will be exposed implicitly to JavaScript, consistent with
        overriden methods from `java.lang.Object`.
    *   Public static methods within the record body will be exposed.
4.  **Fields:** Records can define static fields which will be exposed following
    regular JsType semantics.
5.  **Namespace & Name:** The `namespace` and `name` attributes of `@JsType`
    function as they do for regular classes.

**Example:**

```java
package com.google.myproject;

import jsinterop.annotations.JsType;

@JsType(namespace = "my.js.package")
public record MyRecord(String name, int value) {
  // Custom instance method
  public String display() {
    return this.name + ": " + this.value;
  }
}
```

**JavaScript Usage:**

```javascript
goog.module('my.js.usage');
const MyRecord = goog.require('my.js.package.MyRecord');

const record = new MyRecord('test', 42);
console.log(record.name); // Access component as property: 'test'
console.log(record.value); // Access component as property: 42
console.log(record.display()); // Call custom method: 'test: 42'
// record.name = 'new'; // read-only
```

#### 3.2. JsInterop Annotations on Record Components

*   `@JsIgnore`: Allowed on a component. The component will not be exposed as a
    property in JavaScript.
*   `@JsProperty`: Allowed on a component.
    *   `@JsProperty`: Exposes component even if the record is not JsType.
    *   `@JsProperty(name = "jsName")`: Exposes the component as a JS property
        with the name `jsName`.
*   `@JsMethod`, `@JsOptional`: Disallowed on record components (and by
    extension, parameters of record constructors).

JsInterop annotations will be disallowed on accessor methods to avoid multiple
source of truth. However note that regular JsInterop inheritance/propogation
rules still apply (e.g. applying JsProperty name customizations or JsMethodness
from implemented interface).

**Example:**

```java
package com.google.myproject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "my.js.package")
public record RecordWithAnnotatedComponents(
    @JsProperty(name = "userName") String name,
    int value,
    @JsIgnore String internalId) {}
```

**JavaScript Usage:**

```javascript
goog.module('my.js.usage');
const RecordWithAnnotatedComponents = goog.require('my.js.package.RecordWithAnnotatedComponents');

const record = new RecordWithAnnotatedComponents('test', 42, 'secret');
console.log(record.userName); // Access renamed component: 'test'
console.log(record.name);     // Original name is not available: undefined
console.log(record.value);    // 42
console.log(record.internalId); // Ignored component is not available: undefined
```

#### 3.3. Implementing Custom Component Accessors

Custom implementations of component accessors within the record body (e.g.,
`public String name() { ... }`) are fully supported. The value returned by this
method will be the value of the corresponding JavaScript property. This does not
cause a name clash, as the method *is* the implementation for the component
property.

**Example:**

```java
import jsinterop.annotations.JsType;

@JsType
public record MyRecordWithCustomAccessor(String name) {
  // Custom accessor for the 'name' component.
  @Override
  public String name() {
    return "[[" + this.name + "]]";
  }
}
```

**JavaScript Usage:**

```javascript
const instance = new MyRecordWithOverride("test");
console.log(instance.name); // Calls the overridden name(), outputs "[[test]]"
```

#### 3.4. Name Clashes

JsProperty accessor of component may result in JsInterop name / kind collisions
with other members of the record similar to regular Java classes. Such errors
will be checked and rejected by J2CL as before.

User needs to apply similar resolutions techniques to solve such issues.

**Example:**

```java
import jsinterop.annotations.JsType;
import jsinterop.annotations.JsMethod;

@JsType
public record ClashingRecord(String name) { // Component 'name' -> JS property 'name'

  // ERROR: This implict JsMethod clashes with the 'name' component.
  // public void name(int i) { ... }

  // OK: Resolved with a different JS name.
  @JsMethod(name = "updateName")
  public void name(int i) {
    // ...
  }
}
```

#### 3.5. isNative=true on Records

`@JsType(isNative = true)` will not be permitted on record types. Records are a
Java-specific construct requiring code generation and cannot be nicely mapped to
a native JavaScript type. This restriction will be enforced by
`JsInteropRestrictionsChecker`.

### 4. Migration from @AutoValue & Compatibility

The primary challenge in migrating `@JsType @AutoValue` classes to Records is
reconciling JavaScript API compatibility. By default, Records expose components
as properties (`instance.name`), while `@AutoValue` typically exposes values via
getter methods (`instance.getName()` or `instance.name()`), unless `@JsProperty`
is used to expose a getter as a property.

For example, if JavaScript code calls `instance.name()` on an `@AutoValue`, this
call will break upon migration to a Record since now we exposed a property with
the same name and we cannot bridge existing behavior. To avoid silently breaking
existing JavaScript consumers, the migration tooling must prevent migrations
that change the JS contract in incompatible ways.

**Solution: Enhance`AutoValueToRecord` Tooling to Prevent Breakages**

The `AutoValueToRecord` tool will be enhanced to handle migration based on
whether getters are exposed as methods or properties:

The tool will inspect each abstract getter in the `@AutoValue` class:

*   **If the getter is annotated with `@JsProperty` or `@JsIgnore`**: The getter
    is already exposed as a JS property or is hidden. This is compatible with
    the Record model. The tool will transfer the annotation to the corresponding
    record component.
*   **If the getter is NOT annotated with `@JsProperty` or `@JsIgnore`**: The
    getter is exposed as a JS method.
    *   If the name of the getter method is identical to the component name
        (e.g., `name()` for component `name`), this is a **breaking conflict**.
        The tool will emit a comment that prevents submission without review.
        (e.g. `// DO_NOT_SUBMIT: JS contract change, review callers.`) .
    *   If the name of the getter method differs from the component name (e.g.,
        `getName()` for component `name`), migration is safe. The tool will
        generate a backward-compatible, `@JsMethod` for the getter (e.g.,
        `getName()`).

--------------------------------------------------------------------------------

**Example 1: Migration with Conflict (Requires Manual Review)**

The `User` class cannot be migrated automatically because `name()` is exposed as
a method and conflicts with the `name` component property:

```java
@AutoValue
@JsType
public abstract class User {
  public abstract int name(); // Exposed as method name(), conflicts with component 'name'
}
```

*Result: The tool generates the record but inserts a `DO_NOT_SUBMIT` comment to
ensure manual review:*

```java
// DO_NOT_SUBMIT: JS contract change, review callers.
@JsType
public record User(int name) {}
```

--------------------------------------------------------------------------------

**Example 2: Successful Migration with Compatibility Method**

The `Product` class can be migrated because `getName()` is exposed as a method
but does not conflict with the `name` component property:

```java
@AutoValue
@JsType
public abstract class Product {
  public abstract String getName(); // Exposed as method getName()
}
```

*Result: The tool successfully migrates the class to:*

```java
@JsType
public record Product(String name) {
  @JsMethod
  @InlineMe(replacement = "this.name()")
  public String getName() {
    return name();
  }
}
```

--------------------------------------------------------------------------------

**Example 3: Successful Migration with `@JsProperty`**

The `MyData` class can be migrated because `@JsProperty` ensures `name()` is
exposed as a property, which is compatible with the Record model. The annotation
is transferred to the component.

```java
@AutoValue
@JsType
public abstract class MyData {
  // Exposed as 'data_name' property in JS
  @JsProperty(name = "data_name")
  public abstract String name();
}
```

*Result: The tool successfully migrates the class to:*

```java
@JsType
public record MyData(@JsProperty(name = "data_name") String name) {}
```

### 5. Kotlin and J2KT Interoperability

As J2KT translates Java Records to Kotlin Data Types while maintaining JVM ABI
compatibility, we need to ensure a coherent story for types shared across Java,
Kotlin, and JavaScript.

#### 5.1. Coherent JS ABI

J2CL already defines JsInterop for Kotlin data types where Kotlin properties are
exposed as `JsProperty`s in JavaScript. This aligns perfectly with the Java
Record design proposed here, where record components are also exposed as
read-only JavaScript properties.

By aligning both models, a Java Record and its translated Kotlin Data Class (or
a native Kotlin Data Class) will present the same shape to JavaScript
(properties instead of getter methods), ensuring that migrating a type between
Java and Kotlin does not break JavaScript consumers.

#### 5.2. Customization

If a Kotlin Data Class needs to customize its JS exposure (e.g., renaming a
property), it should use `@JsProperty(name = "...")` on the Kotlin property,
similar to how `@JsProperty` is used on Java Record components (Section 3.2).

### 6. Alternatives Considered

1.  **Expose Components as Methods by Default:** Make the JS contract for Record
    components method calls (e.g., `instance.name()`).
    *   *Pros:* Seamless migration from `@AutoValue`.
    *   *Cons:* Less idiomatic for JavaScript data objects, which typically use
        property access. Negatively impacts clarity for new Records not
        originating from `@AutoValue`.
    *   *Cons (Kotlin):* Breaks consistency with Kotlin Data Classes in J2CL,
        where properties are exposed as properties by default, leading to
        divergent JS APIs. We may attempt to provide JsMethod support for
        properties but then we will need to deal with collisions and stuck with
        a behavior only useful for migration.
2.  **Require Explicit `@JsProperty` on Components:** Do not export components
    by default.
    *   *Pros:* Maximum explicitness.
    *   *Cons:* Overly verbose, contrary to the concise nature of Records. The
        components *are* the primary interface.
3.  **Allow `@JsMethod` on Components**: Another option is to allow `@JsMethod`
    on record components. If present, `@JsMethod` would instruct J2CL to expose
    the component via a getter method (e.g., `instance.id()`) instead of a
    property (`instance.id`). The `AutoValueToRecord` tool could leverage this
    to maintain full backward compatibility in case of conflicts, by generating
    `public record User(@JsMethod int id) {}` instead of aborting the migration.
    *   *Pros*: Unblocks automatic migration for all `@AutoValue` classes while
        maintaining 100% JavaScript backward compatibility.
    *   *Cons:* It can lead to inconsistent APIs in JavaScript, where one
        component on a record is accessed via `record.id()` while another is
        accessed via `record.name` (though maybe we can forbid mix).
    *   *Cons (Kotlin):* Same issues as the "methods by default".
4.  **`@JsRecord` Annotation:** Introduce a new annotation like
    `@JsRecord(componentExposure = METHODS)` to allow per-type control over
    whether components are exposed as properties or methods.
    *   *Pros:* Explicit control.
    *   *Cons:* Adds a new annotation to the JsInterop set, potentially
        complicating the API for a migration-specific scenario.

### 7. Rollout Plan

1.  Implement compiler changes in J2CL to support `@JsType` on Records with the
    semantics described.
2.  Update `JsInteropRestrictionsChecker` to ensure annotation rules and name
    clash detection.
3.  Modify the `AutoValueToRecord` refactoring tool to generate compatibility
    methods.
4.  Document the new semantics and migration guidance on the J2CL site.

--------------------------------------------------------------------------------
