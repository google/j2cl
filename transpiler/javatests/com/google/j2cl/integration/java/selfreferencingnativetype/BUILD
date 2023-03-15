load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    # Google owns the copyright
    licenses = ["unencumbered"],
)

integration_test(
    name = "selfreferencingnativetype",
    srcs = glob(["*.java"]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
)
