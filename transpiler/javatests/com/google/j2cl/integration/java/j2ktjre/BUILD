load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2kt_native_library")

package(licenses = ["notice"])

j2kt_native_library(
    name = "j2ktjre-j2kt-native",
    srcs = glob(
        [
            "*.java",
            "super-j2kt/*.java",
        ],
        exclude = ["Main.java"],
    ),
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2kt-native",
    ],
)

j2kt_jvm_library(
    name = "j2ktjre-j2kt-jvm",
    srcs = glob(
        [
            "*.java",
            "super-j2kt/*.java",
        ],
        exclude = ["Main.java"],
    ),
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2kt-jvm",
    ],
)

integration_test(
    name = "j2ktjre",
    srcs = glob(["*.java"]),
    enable_wasm = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
