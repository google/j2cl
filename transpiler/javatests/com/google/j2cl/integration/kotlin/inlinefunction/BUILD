load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "java_library",
    srcs = ["JavaClass.java"],
)

integration_library(
    name = "externalinlinefunction",
    srcs = ["InlineFunctionsFromDeps.kt"],
    deps = [":java_library"],
)

integration_test(
    name = "inlinefunction",
    srcs = [
        "ClassWithInlineFunc.kt",
        "Main.kt",
    ],
    deps = [":externalinlinefunction"],
)
