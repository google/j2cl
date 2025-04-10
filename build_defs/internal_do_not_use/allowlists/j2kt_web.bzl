"""J2KT Web allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

# Packages that j2cl_library macro will generate j2kt web packages by default.
J2KT_WEB_ALLOWLIST = allowlists.of_packages([
    "//samples/box2d/src/main/java/...",
    "//transpiler/javatests/com/google/j2cl/integration/java/...",
    "//transpiler/javatests/com/google/j2cl/readable/java/...",
])

J2KT_WEB_DISABLED = allowlists.of_targets([
    # Defines the special "await" method that is well-known to J2CL. This doesn't
    # come through J2KT cleanly and will generate invalid code.
    "//transpiler/javatests/com/google/j2cl/integration/java/jsasync:promise-j2cl",
])
