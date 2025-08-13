 <!-- TOC -->

# Limitations of J2CL - Java compatibility

## No Support for Java Reflection

J2CL does not support any of `java.lang.reflect.*` or any kind of runtime class,
object introspection including all methods on `java.lang.Class` that return
`java.lang.reflect` objects.

The main reason is that code that uses reflection hinders global dead code
removal. On the other hand code generation could be a substitute for many tasks
that would normally be accomplished by the use of reflection.

## No Support for Enum Reflective APIs

J2CL doesn't support `Class.getEnumConstants` and `Enum.valueOf` APIs.

## No Bound Checks on Array Access

J2CL doesn't perform bound check for array accesses. J2CL promises minimal to no
overhead for using language primitives to discourage people from writing native
code for simple stuff only for performance considerations. Having
JavaScript-like array semantics help that story.

## Float is emulated with extra precision

J2CL doesn't emulate 32 bit floating point arithmetic for performance reasons.
Instead JavaScript `number` is used to emulate `float` without any
instrumentation and results in 64 bit precision. This makes them effectively
same as the `double` type in Java.

This is rarely noticable in practice but if for your application repeatability
of the floating point arithmetic really matters across different platforms, this
is something to keep in mind.

## Limitations of JRE Emulation

J2CL emulates a substantial portion of the Java Standard Library (a.k.a JRE).
However it's not feasible nor practical to support all of the APIs in the web
platform; APIs like `java.net.*` are intentionally left out.

Another common example of this is `String.format` which requires emulation of a
large set of expensive localization APIs. For this case, Guava provides
`Strings.lenientFormat` which is a compact cross-platform compatible
alternative.

The source of truth for the full-set of emulated API can be found in
`jre/java/`.

Shared code that uses missing APIs can work around this limitation by
[super-sourcing](best-practices.md#super-sourcing-writing-platform-specific-code)
the classes and providing an alternative implementation specific for J2CL.

Note that both Web and Java platforms are evolving; in some cases, an emulation
might have been left out because the Java API is relatively recent or Web didn't
have a good (fast/correct/compact) way to emulate but now can. In such cases,
please contact us and/or send proposal for emulation.

## Minor Semantic Differences

-   **Switch on enum type does not trigger class initialization (`clinit`)**.
    Although
    [JLS §12.4](https://docs.oracle.com/javase/specs/jls/se9/html/jls-12.html#jls-12.4)
    does not explicitly require `switch` to trigger class initialization, both
    javac and ecj generate code that does, but J2CL generated code does not.
