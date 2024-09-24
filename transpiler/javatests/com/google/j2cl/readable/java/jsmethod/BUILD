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
    # Native JsMethod does not make sense for j2kt-native.
    build_kt_native_readables = False,
    # JsMethod on non-native types not supported in Wasm.
    generate_wasm_readables = False,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
