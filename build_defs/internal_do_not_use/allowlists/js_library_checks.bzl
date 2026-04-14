"""An allowlist of packages for which library-level checking is disabled."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

OFF = allowlists.of_packages([
    # TODO(b/281534723): Remove after bad react example code is cleaned.
    "//samples/react/java/com/google/j2cl/samples/react/state",
    # TODO(b/275736677): toString method patched by ValueType is not recognized by JSC.
    "//transpiler/javatests/com/google/j2cl/autovalue",
    # TODO(b/280942747): With raw types it is possible to trigger type errors in jscompiler; these
    # tests expose these problems.
    "//transpiler/javatests/com/google/j2cl/integration/java/morebridgemethods",
    # We are testing calling hidden constructor fails at runtime.
    "//transpiler/javatests/com/google/j2cl/integration/java/staticinitfailfast",
    # TODO(b/277822633): Remove after IObject as parameter is correctly mapped.
    "@jsinterop_generator//javatests/jsinterop/generator/externs/iobjectiarraylike",
])

LOOSE = allowlists.of_packages([
    # We don't want to block the readable tests from running even if they have errors.
    "//transpiler/javatests/com/google/j2cl/readable/...",
])

EXTRA_CONFORMANCE_ALLOWLIST = allowlists.of_packages([
    "//transpiler/javatests/com/google/j2cl/readable/...",
    "//jre/...",
    "//ktstdlib/...",
])
