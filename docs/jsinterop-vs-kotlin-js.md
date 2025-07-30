# J2CL JsInterop vs Kotlin/JS Interop

## Quick Summary

### Exporting to JS

                                            | J2CL JsInterop                                                                       | Kotlin/JS
:------------------------------------------ | :----------------------------------------------------------------------------------- | :--------
Export a class/interface to JS              | `@JsType`                                                                            | `@JsExport` (top-level only)
Export a method to JS                       | `@JsMethod`                                                                          | `@JsExport` (top-level only)
Export a field to JS                        | `@JsProperty`                                                                        | `@JsExport` (top-level only)
Don't export a symbol/method/field          | `@JsIgnore`                                                                          | `@JsExport.Ignore`
Specify a parameter as being optional       | `@JsOptional`                                                                        | Optional argument syntax
Specify a name for a symbol                 | `name` property on `@JsType`/`@JsMethod`/`@JsProperty`                               | `@JsName`
Specify the namespace to export a symbol to | A combination of the `namespace` and `name` from `@JsType`/`@JsMethod`/`@JsProperty` | Based on `package` statement, no additional customization allowed
Define a functional interface               | `@JsFunction`                                                                        | functional type expressions

### Calling JS from Java/Kotlin

|                  | J2CL JsInterop                      | Kotlin/JS           |
| :--------------- | :---------------------------------- | :------------------ |
| Declare          | `@JsType(isNative = true)`          | `external` keyword  |
: class/interface  :                                     :                     :
: that exists in   :                                     :                     :
: JS               :                                     :                     :
| Declare method   | `@JsMethod` with `native` keyword   | `external` keyword  |
: that exists in   :                                     :                     :
: JS               :                                     :                     :
| Declare field    | `@JsProperty` with `native` keyword | Assign a property   |
: that exists in   : on a method, or just a              : to                  :
: JS               : `@JsProperty` in a `isNative =      : `definedExternally` :
:                  : true` class.                        : and/or mark the     :
:                  :                                     : getter and/or       :
:                  :                                     : setter as           :
:                  :                                     : `external`          :
| Specify a        | Add overloads omitting each of the  | Use the optional    |
: parameter as     : optional arguments                  : argument syntax     :
: being optional   :                                     : assigning them to   :
:                  :                                     : `definedExternally` :
| Specify a        | `name` property on                  | `@JsName`           |
: different name   : `@JsType`/`@JsMethod`/`@JsProperty` :                     :
: for a symbol     :                                     :                     :
| Specify the      | A combination of the `namespace`    | `@JsModule` if      |
: module to import : and `name` from                     : importing from a    :
: from             : `@JsType`/`@JsMethod`/`@JsProperty` : CommonJS module, or :
:                  :                                     : `@JsQualifier` if   :
:                  :                                     : it exists in the    :
:                  :                                     : global scope.       :
| Define an        | `@JsOverlay`                        | Extension functions |
: overlay method   :                                     :                     :
: on a JS class    :                                     :                     :
| Define a         | `@JsFunction`                       | functional type     |
: functional       :                                     : expressions         :
: interface        :                                     :                     :

## Detailed Comparison

> [!NOTE] Kotlin/JS interop semantics are experimental and may change over time.
> The analysis here is performed against **Kotlin 2.1.0**.

### The dynamic type

Kotlin/JS has a special
[`dynamic` type](https://kotlinlang.org/docs/dynamic-type.html) which can be
used in situations where the type is unknown or cannot be represented in the
Kotlin type system (ex. union types). Dynamic types can be passed as any type
and can receive any type. They also do not perform any null checking.

Most interestingly, you can directly call functions (or access properties) on
`dynamic` types that are otherwise never defined:

```kotlin
fun doSomething(d: dynamic) {
  // doSomethingElse is never defined in Kotlin
  d.doSomethingElse("foo")
}
```

The call to `doSomethingElse` will be transpiled as-is, so it's just expected to
be there at runtime. This is referred to as a **dynamic call**. They will always
return the dynamic type.

#### Comparison to J2CL JsInterop

J2CL doesn't have any direct comparable feature, but it does have similar
capabilities. For example, a native JsType can be declared that has the type of
`?` in Closure to represent the unknown type. Unchecked casts can then be
performed to go back and forth between that type.

### Exporting to JS

Kotlin/JS performs monolithic compilation and will mangle the names of
symbols/methods/properties. Therefore they have the
[`@JsExport`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.js/-js-export/)
annotation to expose these to external callers. This annotation can be applied
to any **top-level** class/interface, property, or function.

It is not allowed to be used on:

*   `expect` declarations
*   inline functions with reified type parameters
*   suspend functions
*   secondary constructors without `@JsName`
*   extension properties
*   ~~enum classes~~
*   annotation classes

And declarations that are exported can only use "exportable" types:

*   `Any`, `String`, `Boolean`, `Byte`, `Short`, `Int`, `Float`, `Double`
*   `BooleanArray`, `ByteArray`, `ShortArray`, `IntArray`, `FloatArray`,
    `DoubleArray`
*   `Array<exportable-type>`
*   Function types with exportable parameters and return types
*   `external` or `@JsExport` classes and interfaces
*   Nullable counterparts of types above
*   `Unit` return type. Must not be nullable

#### Constructors

Kotlin has a concept of primary and secondary constructors as part of the
language. Classes do not need to define a primary constructor, but if they do
then all secondary constructors must delegate to it. Additionally, if a
supertype has any constructor defined, then subtypes must delegate to any of the
constructors.

When exporting a Kotlin type to JS, Kotlin will always assume there's a primary
constructor. If it's not explicitly defined, then it's implied to be empty. All
secondary constructors become factory methods and all implicitly or explicitly
delegate to the primary constructor. Because secondary constructors don't have
unique names, they must always be ignored or renamed with `@JsName`.

There's no additional checks when subtyping a `@JsExport` class with a primary
constructor. Kotlin/JS does not use ES6 class syntax, rather they construct the
prototype chains manually, so there's no requirement that primary constructors
of subtypes delegate to super primary constructors.

#### Companion Objects

If a companion object's enclosing type is `@JsExport`'d, then the companion
members are also considered exported. However, JS users will need to access the
companion object via the `Companion` property on the enclosing type.

If users wish for some companion members to exist on the enclosing type as a
static member in JS, the
[`@JsStatic`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.js/-js-static/)
annotation can be used to do just that.

#### Comparison to J2CL JsInterop

In general this is similar to the corresponding suite of `@JsType`,
`@JsProperty`, and `@JsMethod` annotations.

Like `@JsType`, when `@JsExport` is applied to a class/interface, it applies to
all public declarations. Unlike `@JsType`, it also applies to nested types
recursively (though this can be undone with `@JsExport.Ignore`, similar to
`@JsIgnore`).

Constructor behavior is quite a bit more flexible in Kotlin/JS since they do not
transpile to ES6 class syntax which requires that constructors always call the
super. While the idea of a primary constructor is similar to J2CL JsInterop's
`@JsConstructor`, there's no requirement for subtypes to delegate to supertypes'
primary constructor.

Otherwise the restrictions on `@JsExport` are similar or reasonable for the
additional syntax supported in Kotlin. One key difference is that enums cannot
be exported, whereas in J2CL JsInterop they can be either via `@JsType` or
`@JsEnum`. Additionally Kotlin only permits exporting top-level declarations,
whereas J2CL JsInterop allows it at any level.

The restrictions on types used in exported declarations are similar to the J2CL
JsInterop restrictions currently imposed, albeit they're only enforced as a
warning in J2CL.

Exporting properties doesn't quite map to the equivalent `@JsProperty` as the
latter permits a bit more flexibility in that it can be applied to a field or a
method (to act as a getter/setter). In Kotlin `@JsExport` applied to a property
will export the backing field if there's no getter/setter, else a property in JS
will be created with JS getter/setter wired to the Kotlin getter/setter.

### Calling JS from Kotlin

Kotlin/JS utilizes the `external` keyword to mark **top-level** declarations as
being defined external to the Kotlin compilation. Non top-level declarations can
omit their implementation if they are enclosed in an external type, for example:

```kotlin
external class Foo {
  val bar: Int
  fun buzz(): Int
}
```

There is also a special
[`definedExternally`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/defined-externally.html)
property that can optionally be used as a marker for an externally implemented
property/function. This is only required when marking an optional parameter in
an externally defined function, for example:

```kotlin
external class Foo {
  val bar: Int = definedExternally // this assignment isn't required
  fun buzz(optionalParam: Int = definedExternally): Int
}
```

Unlike the checks for `@JsExport`, external members do not have their referenced
types checked to ensure that they're exported or external types.

While `external interface`s are supported, it does come with additional
restrictions, namely that they cannot be used in an `is` expression, or used to
special a reified type. Additionally, any type cast will always succeed.

#### Overlays

In J2CL we have the concept of a `@JsOverlay` annotated method which allows
non-native methods to be added a native type. This is particularly useful for
adding bridge methods that don't actually exist on the underlying type. Any
virtual methods are devirtualized and take the receiver as a parameter. Due to
this there are limitations on these methods, namely that they cannot be
polymorphic.

Kotlin supports this as a language feature for *any* type via extension
functions/properties with the same limitations that `@JsOverlay` has.

#### Constructors

Kotlin/JS allows defining as many secondary constructors as desired in
`external` types. If there's a primary constructor, secondary constructors do
not need to delegate to it (as that's assumed to be part of the body of the
constructor). Kotlin/JS considers all primary and secondary constructors to be
the same constructor in JS.

Subtypes are free to delegate to any of the constructors.

#### Static Members

Since Kotlin doesn't have static members, all companion members with an
enclosing external type will be considered as being static members on the
underlying type. That is, to represent the JS code:

```js
class Foo {
  static bar() {
    console.log("hi there!");
  }
}
```

You would write in Kotlin/JS:

```kotlin
external class Foo {
  companion object {
    fun bar()
  }
}
```

#### Comparison to J2CL JsInterop

Kotlin/JS's `external` syntax is very similar to J2CL JsInterop behavior for
`isNative = true` and `native` marked methods, but nicely baked into the
language itself. The restrictions on external types are also similar, wherein
interface types are supported, but with additional restrictions on how the type
can be used (i.e. `instanceof` checks).

In fact, many of these features are already supported in Kotlin/Closure as we
needed to use the `external` syntax as an equivalent for Java's `native` syntax.

### Writing JS in Java

There are cases where both Kotlin/JS and J2CL developers need to handwrite an
implementation that should be used as-is. In J2CL this is done by declaring
`native` methods on non-native enclosing types. It is then expected that the
implementation of the method will be provided in an adjacent `native.js` file.

In Kotlin/JS there are two ways to do this. One way is by using dynamic types
and dynamic calls (as [covered earlier](#the-dynamic-type)), or via the `js`
function which accepts a string literal containing JS code that will be used
as-is. For example:

```kotlin
class Foo {
  fun random(from: Int, to: Int): Int {
    return js("(Math.random() * (to - from + 1) + from) | 0")
  }
}
```

Neither Kotlin/JS nor J2CL does any checking of this code, so it's entirely on
the user to accurately represent the types.

### Functional Interfaces

While Kotlin does have SAM functions (declared with the `fun interface` syntax),
it does not provide interop between Kotlin/JS. All implementations of these SAM
interfaces will be a real class with a real method rather than just a bare
function.

However, a functional type can be used in place of a `fun interface` type to
replicate the behavior of accepting a bare JS function:

```kotlin
@JsExport fun foo(f: () -> Unit) = f()
```

which will transpile to:

```js
function foo(f) {
  f();
}
```

This means there's no direct equivalent to J2CL JsInterop's `@JsFunction`
feature as you cannot declare an interface type that can be reused, however,
using a functional type does serve much of the same purpose. Additionally,
functional types are likely going to be more desirable to users as `@JsFunction`
interfaces don't serve much purpose in Java.

Kotlin does support limited instanceof checks on functional types, namely you
can perform the following checks:

*   `foo is () -> Unit`
*   `foo is Function0<Unit>`
*   `foo is Function0<*>`

Any other instanceof check will fail to compile. All of those examples will
transpile to `typeof foo == 'function'`, so any function will pass the check.

### Enums

Since enums don't actually exist in the JS language, Kotlin/JS only supports
exporting Kotlin-defined enums to JS. Declaring an external enum is not
supported.

J2CL supports exporting an enum type with `@JsType`, which mimics the Kotlin/JS
behavior. It also supports interoperability with Closure enum types when using
the `@JsEnum` annotation. There is no equivalent behavior in Kotlin/JS.
