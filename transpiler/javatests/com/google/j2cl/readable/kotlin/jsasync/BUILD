load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.kt"]),
    generate_kt_readables = False,  # @JsAsync not supported in Kotlin.
    generate_wasm_readables = False,  # @JsAsync not supported in wasm.
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
