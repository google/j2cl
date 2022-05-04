load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "enummethods",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    enable_kt = False,
    # Disabled because it uses native types to test the enum properties.
    enable_wasm = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
