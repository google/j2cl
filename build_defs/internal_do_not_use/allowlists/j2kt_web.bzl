"""J2KT Web allowlists."""

load(":allowlists.bzl", "allowlists")

visibility(["//build_defs/internal_do_not_use/..."])

# Targets for which J2KT Web is always disabled.
J2KT_WEB_DISABLED = allowlists.of_targets([
    # Defines the special "await" method that is well-known to J2CL. This doesn't
    # come through J2KT cleanly and will generate invalid code.
    "//transpiler/javatests/com/google/j2cl/integration/java/jsasync:promise-j2cl",
    "//transpiler/javatests/com/google/j2cl/readable/java/jsasync:helper",
    # Test library for calling non-J2KT transpiled Java from J2KT-transpiled Java.
    "//transpiler/javatests/com/google/j2cl/integration/java/j2ktjvminterop:java-j2cl",
])

# Packages for which J2KT Web is always enabled for, except those specified in J2KT_WEB_DISABLED.
J2KT_WEB_ENABLED = allowlists.of_packages([
], exclude = [J2KT_WEB_DISABLED])

# Packages for which J2KT Web is enabled if the blaze flag is enabled, except those specified in
# J2KT_WEB_DISABLED.
J2KT_WEB_EXPERIMENT_ENABLED = allowlists.of_packages([
    "//samples/box2d/src/main/java/...",
    "//transpiler/javatests/com/google/j2cl/integration/java/...",
    "//transpiler/javatests/com/google/j2cl/readable/java/...",
], exclude = [J2KT_WEB_DISABLED])

# Packages that are building with J2KT Web, but cannot yet be promoted to experimental. Since all
# projects share the experimental set, we might need to delay adding packages to it if there's a
# rollout in progress. Until then, the packages can be staged.
# NOTE: the staged set should _never_ be rolled out to production. It should only be used for local
#   development/testing, benchmarking, and CI.
J2KT_WEB_STAGING_ENABLED = allowlists.of_packages([
], exclude = [J2KT_WEB_DISABLED])
