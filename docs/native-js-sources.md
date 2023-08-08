# native.js Sources

Warning: the usage of `native.js` files is **discouraged** as many safety checks
that J2CL and the JSCompiler would normally apply are bypassed. Instead users
are encouraged to use [JsInterop](jsinterop-by-example.md) or Elemental2.

J2CL provides a mechanism for users to handwrite implementations of particular
methods by supplying them in a `.native.js` file alongside the source file.

For GWT users, this is J2CL's equivalent of JSNI.

## How do I use it?

Any method marked with the `native` will inform J2CL that the implementation of
the method is expected to just exist at runtime. This can be one of several
ways:

1.  If it's a member of a `@JsType(isNative = true)` then the method should
    exist on the type being overlaid.
2.  If it's a `@JsMethod/@JsProperty` with a custom namespace applied, it's
    expected to be importable from the namespace as runtime.
3.  Otherwise J2CL will expect a corresponding `.native.js` to exist. The
    contents of this file will be concatenated onto the the transpilation result
    for the rest of the file.

## How is the corresponding `native.js` file resolved?

Since users structure and package their sources that are supplied to J2CL in
several different ways, we apply a very different approaches to attempt to find
matching `.native.js` file.

First let's define the phrase *source root*. In this context it's path up to the
first occurance of `java/` or `javatests/` from the absolute file path.

J2CL will attempt to find the `native.js` file in the following order (first one
to match wins):

1.  Using the fully qualified namespace of the Java symbol, we'll look for a
    `native.js` file with that filename in any folder.

    For example, the class: `foo.bar.Buzz` will trigger a lookup for
    `foo.bar.Buzz.native.js`. The namespace used will always be the original
    Java namespace (i.e. any customization to the JS namespace does not apply).

2.  A source root relative path is constructed using the Java fully qualified
    namespace.

    For example: the class `foo.bar.Buzz` will trigger a lookup for a source
    root relative path of `foo/bar/Buzz.native.js`. The qualified name used will
    always be the original Java one (i.e. any customization to the JS
    name/namespace does not apply).

3.  A source root relative path is constructed from original absolute path of
    the Java source file, but with the current symbol substituted for the
    filename.

    For example: the class `Qux` in the file `foo/bar/super/Buzz.java` will
    trigger a lookup for path `foo/bar/super/Qux.native.js`.

If no matching `native.js` file can be found then an error is emitted during
transpilation.

All `native.js` files ***must*** be used by the transpiler or an error is
emitted.

## Example

Consider the Java file `java/com/google/example/Foo.java`:

```java
class Foo {
  public static native String getValue();
}
```

We can provide the implementation for this method by also adding the file
`java/com/google/example/Foo.native.js`:

```javascript
/**
 * @return {string}
 */
Foo.getValue = function() {
  return window.someStr;
}
```

## Tips

*   **DO NOT** add extra goog.requires to `native.js` files. Since these are
    concatenated with the transpiled code for the rest of the file, this is
    effectively putting import in the middle of the file. Additionally, the
    imports you add may conflict with existing imports.

    Instead, ensure the Java code has imports for all the types you'll need.
    These imports will be accessible in the `native.js` by referencing their
    simple names.

*   Be particularly mindful of nullability in `native.js` files. The JSCompiler
    does not enforce nullability checks within J2CL-generated code, and since
    the `native.js` files are just concatenated on, they also don't get these
    checks.

*   If you're unsure about the type annotations that should be used, you can
    find out a commented out function definition in the transpiled output. You
    can copy and uncomment that block into your `native.js` file as a basis.

*   Ensure that your Java method signature stays in sync with your JS function
    signature. There is nothing enforcing that these match except potentially
    hard to interpret errors from JSCompiler.
