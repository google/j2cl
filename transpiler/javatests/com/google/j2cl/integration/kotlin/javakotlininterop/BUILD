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
    name = "kotlin_dep",
    srcs = ["KotlinClass.kt"],
    deps = [":java_dep"],
)

integration_library(
    name = "java_dep",
    srcs = ["FromJava.java"],
)

integration_test(
    srcs = glob(["Main.java"]),
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":java_dep",
        ":kotlin_dep",
    ],
)
