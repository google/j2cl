load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "readable-j2wasm",
    srcs = glob(
        [
            "*.java",
            "super-wasm/*.java",
        ],
        exclude = ["FooWithNativeProperty.java"],
    ),
    deps = ["//third_party:jsinterop-annotations-j2wasm"],
)

readable_example(
    srcs = glob(["*.java"]),
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    generate_wasm_externs = True,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
