load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    # Google owns the copyright
    licenses = ["unencumbered"],
)

integration_test(
    name = "selfreferencingnativetype",
    srcs = glob(["*.java"]),
    enable_kt = False,
    # Uses jsinterop features not supported in WASM.
    enable_wasm = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
