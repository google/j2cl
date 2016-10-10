# Contained here is VM bootstrap JS logic.

## Expectations
- Needs to be available to transpiled output
- Is not part of the JRE
- References the JRE.

For example the implementation of the Java "assert" keyword is not
JRE logic so it is implemented in a separate location, but it does
throw the JRE's AssertionError class and so must have access to
the JRE's classes.

So, conceptually it is the third layer of JS because it depends
upon JRE, which is the second layer.

Build integration that synthesizes js_library rules from java_library
rules should synthesize a dependency on VMBootstrap so that an
implementation is available for the VMBootstrap functions that are
called in the transpiled JS.

Files should be named with a .java.js extension to match generated J2CL
source. This ensures that they receive the same special treatment in
JSCompiler's null analysis and the J2CL minifier's renaming that is
given to generated source.
