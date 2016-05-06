# Contained here is native bootstrap JS logic.

## Expectations
- Needs to be available to transpiled output
- Is not part of the JRE and does
- Does not reference the JRE

Conceptually it is the lowest layer of JS because it is depended upon
by others but depends on no one.

Build integration that synthesizes js_library rules from java_library
rules should synthesize a dependency on NativeBootstrap so that an
implementation is available for the NativeBootstrap functions that are
called in the transpiled JS.
