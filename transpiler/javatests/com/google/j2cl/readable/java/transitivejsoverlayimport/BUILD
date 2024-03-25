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
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    generate_wasm_readables = False,  # Wasm doesn't allow extending native types.
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
