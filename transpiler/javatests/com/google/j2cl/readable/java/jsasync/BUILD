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
    generate_kt_readables = False,  # JS only APIs.
    generate_kt_web_readables = True,
    generate_wasm_readables = False,  # @JsAsync not supported in WASM.
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
