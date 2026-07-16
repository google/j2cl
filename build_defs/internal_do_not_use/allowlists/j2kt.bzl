"""Allowlists common to multiple J2KT platforms."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

# Labels which will have relaxed visibility emitted from J2KT:
# - non-public types will be made public
# - protected methods and overrides will be made public
EMIT_RELAXED_VISIBILITY_ALLOWLIST = allowlists.of_packages([
    # go/keep-sorted start
    "//third_party/java_src/dagger/...",
    "//jre/java",
    "//jre/javatests",
    "//junit/...",
    "//samples/box2d/src/main/java",
    "//transpiler/javatests/com/google/j2cl/integration/java/allsimplebridges",
    "//transpiler/javatests/com/google/j2cl/integration/java/autovalue",
    "//transpiler/javatests/com/google/j2cl/integration/java/backwardbridgemethod",
    "//transpiler/javatests/com/google/j2cl/integration/java/cyclicclinits",
    "//transpiler/javatests/com/google/j2cl/integration/java/innerclassinitorder",
    "//transpiler/javatests/com/google/j2cl/integration/java/instanceinnerclass",
    "//transpiler/javatests/com/google/j2cl/integration/java/j2kt/relaxedvisibilitydep",
    "//transpiler/javatests/com/google/j2cl/integration/java/jsbridgebackward",
    "//transpiler/javatests/com/google/j2cl/integration/java/jsbridgemultipleaccidental",
    "//transpiler/javatests/com/google/j2cl/integration/java/jsbridgemultipleexposing",
    "//transpiler/javatests/com/google/j2cl/integration/java/multipleroottypes",
    "//transpiler/javatests/com/google/j2cl/readable/java/accidentaloverride",
    "//transpiler/javatests/com/google/j2cl/readable/java/autovalue",
    "//transpiler/javatests/com/google/j2cl/readable/java/bridgemethods",
    "//transpiler/javatests/com/google/j2cl/readable/java/cast",
    "//transpiler/javatests/com/google/j2cl/readable/java/genericanddefaultmethods",
    "//transpiler/javatests/com/google/j2cl/readable/java/genericmethod",
    "//transpiler/javatests/com/google/j2cl/readable/java/gwtincompatible",
    "//transpiler/javatests/com/google/j2cl/readable/java/innerclassinheritance",
    "//transpiler/javatests/com/google/j2cl/readable/java/innerclassinitorder",
    "//transpiler/javatests/com/google/j2cl/readable/java/j2kt/relaxedvisibilitydep",
    "//transpiler/javatests/com/google/j2cl/readable/java/jsbridgebackward",
    "//transpiler/javatests/com/google/j2cl/readable/java/multipletopclasses",
    "//transpiler/javatests/com/google/j2cl/readable/java/qualifiedsupercall",
    "//transpiler/javatests/com/google/j2cl/readable/java/staticfieldimport",
    "//transpiler/javatests/com/google/j2cl/readable/java/subclassgenericclass",
    "//transpiler/javatests/com/google/j2cl/readable/java/subnativejstype",
    "//transpiler/javatests/com/google/j2cl/readable/java/supercallnondefault",
    "//transpiler/javatests/com/google/j2cl/readable/java/supermethodcall",
    "//third_party/java_src/owasp_html_sanitizer",
    "//third_party/java_src/xplat/j2kt_mockito",
    "//third_party/java_src/xplat/kmpbench/java",
    # go/keep-sorted end
])

EXEMPT_FROM_NULLMARKED_ALLOWLIST = allowlists.of_packages([
    "//jre/javatests",
    "//junit/...",
    "//samples/box2d/src/main/java",
    "//third_party/java_src/xplat/j2kt/emulation/android",
    "//third_party/java_src/xplat/j2kt/jre/java",
    "//third_party/java_src/xplat/kmpbench/java",
    # We're permissive of tests not being @NullMarked.
    "//javatests/...",
])
