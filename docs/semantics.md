 <!-- TOC -->

# Notes on language semantics for J2CL

## Java to JavaScript Data Type Mapping

Java Type           | JS Type                     | Clarification
------------------- | --------------------------- | ------------------------
boolean             | boolean                     |
byte                | number                      |
char                | number                      |
short               | number                      |
int                 | number                      |
long                | goog.math.Long              |
float               | number                      |
double              | number                      |
java.lang.Boolean   | boolean \| null             | See Boxing section below
java.lang.Byte      | java.lang.Byte \| null      |
java.lang.Character | java.lang.Character \| null |
java.lang.Short     | java.lang.Short \| null     |
java.lang.Integer   | java.lang.Integer \| null   |
java.lang.Long      | java.lang.Long \| null      |
java.lang.Float     | java.lang.Float \| null     |
java.lang.Double    | number \| null              | See Boxing section below
java.lang.String    | string \| null              | See Boxing section below
java.lang.Object    | \*                          | See Boxing section below

### Boxing

[Boxed types](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html)
add a noticeable amount of performance overhead to JavaScript. To eliminate this
overhead, J2CL never uses boxed representation for Double, String and Boolean.
Double becomes a JavaScript number and Boolean becomes a JavaScript boolean.
It's not possible to use plain numbers for boxed Integers because it leads to
ambiguity. For example, the instanceof operator wouldn't be able to distinguish
between Integer and Double:

```java
Object foo = new Double(42)

// If both Integer and Double were represented with plain js number then there would be no way
// to perform this runtime check for the type.
if (foo instanceof Integer) {
  x--;
} else if (foo instanceof Double) {
  x++;
}
```

Note that no such ambiguity exists for primitive ints, which is why they can be
represented using a native JavaScript number.

### String

JavaScript natively supports strings and J2CL automatically unboxes Strings. So
there is a one-to-one mapping between Java String and the native JavaScript
string type.

### Integer

JavaScript does not natively support integers. J2CL emulates integers using the
native JavaScript number type: a 64-bit double. Note that boxed Integers remain
boxed, unlike Double and Boolean.

### Double

JavaScript natively supports doubles and J2CL automatically unboxes Doubles. So
there is a one-to-one mapping between Java Double/double and the native
JavaScript number type.

### Long

JavaScript does not natively support longs. J2CL emulates longs using the
Closure long library:
[goog.math.Long](http://google3/third_party/javascript/closure/math/long.js)

### Boolean

JavaScript natively supports booleans and J2CL automatically unboxes Booleans.
So there is a one-to-one mapping between Java Boolean/boolean and the native
JavaScript boolean type.

### Null vs Undefined

From J2CL's perspective `null` and `undefined` are the same type. If you check
if a value is equal to null then it will return true if the value is `null` *or*
`undefined`.

## Behavior of java.lang.Object methods

TODO(rluble): complete the following table

|                | `toString()` | `equals()` | `hashcode()` | `getClass()` |
| -------------- | ------------ | ---------- | ------------ | ------------ |
| Arrays         |              |            |              |              |
| Native arrays  |              |            |              |              |
| JsFunction     |              |            |              |              |
| JsFunction     |              |            |              |              |
: implementation :              :            :              :              :

## Differences in J2CL JsInterop vs. GWT JsInterop

*   Namespaces on non-native JsMethods are forbidden (i.e. J2CL does not allow
    to define a method out of its module's scope). GWT allowed this only due to
    providing a way to export functions to global scope; J2CL relies on
    goog.export for the same functionality.
*   Namespace JsPackage.GLOBAL is special. Native JsTypes within this namespace
    are considered externs hence no goog.require will be generated for them.
*   Instances of `java.lang.Object[]` are not stamped. `new Object[]{ "a" }` in
    Java and `["a"]` in JS are effectively same.
*   instanceof on a class F implementing a @JsFunction interface returns true if
    and only if the instance is actually an instance of class F.
*   Subclasses of classes that have a `@JsConstructor` are required to have a
    `@JsConstructor`.
*   The constructor of an anonymous inner class that extends a class with a
    `@JsConstructor` is implicitly a `@JsConstructor`.
