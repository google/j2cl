package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :readable_js
integration_test(
    name = "classliteralstripped",
    srcs = glob(["*.java"]),
    closure_defines = {"jre.classMetadata": "'STRIPPED'"},
    # TODO(b/31386321): enable the test after the feature is implemented.
    disable_compiled_test = True,
    disable_uncompiled_test = True,
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)
