package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "/third_party/java_src/j2cl/transpiler/javatests/com/google/j2cl/transpiler/integration/integration_test",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
# TODO: Remove ARRAY_CHECKS_ENABLED_ false when ArrayList works.
integration_test(
    name = "trywithresource",
    srcs = glob(["*.java"]),
    closure_defines = {
        "ARRAY_CHECK_BOUNDS_": "false",
    },
)
