package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "morebridgemethods",
    srcs = glob(["*.java"]),
    closure_defines = {
        "jre.checks.checkLevel": "'NORMAL'",
        "jre.checkedMode": "'ENABLED'",
    },
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)
