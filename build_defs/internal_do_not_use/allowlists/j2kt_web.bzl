"""J2KT Web allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

# Packages for which J2KT Web is always enabled for, except those specified in J2KT_WEB_DISABLED.
J2KT_WEB_ENABLED = allowlists.of_packages([])

# Packages for which J2KT Web is enabled if the blaze flag is enabled, except those specified in
# J2KT_WEB_DISABLED.
J2KT_WEB_EXPERIMENT_ENABLED = allowlists.of_packages([
    "//samples/box2d/src/main/java/...",
    "//transpiler/javatests/com/google/j2cl/integration/java/...",
    "//transpiler/javatests/com/google/j2cl/readable/java/...",
])

# Targets for which J2KT Web is always disabled.
J2KT_WEB_DISABLED = allowlists.of_targets([
    # Defines the special "await" method that is well-known to J2CL. This doesn't
    # come through J2KT cleanly and will generate invalid code.
    "//transpiler/javatests/com/google/j2cl/integration/java/jsasync:promise-j2cl",
])
