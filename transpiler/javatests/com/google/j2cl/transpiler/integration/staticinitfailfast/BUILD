package(
    licenses = ["notice"],  # Apache 2.0
)

load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

integration_test(
    name = "staticinitfailfast",
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
