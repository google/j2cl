load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

readable_example(
    srcs = glob(["*.java"]),
    # @JsAsync not supported in Kotlin.
    generate_kt_readables = False,
    generate_wasm_readables = False,  # JS varargs is not supported in Wasm.
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
