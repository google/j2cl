load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "stringescapecodes",
    srcs = glob(["*.java"]),
    # TODO(b/325056681): Invalid UTF-16 string literals are not yet supported in the modular
    # pipeline.
    enable_wasm_modular = False,
)
