load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Exclude native sources from j2kt_jvm_library ad j2kt_native_library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2kt-jvm"],
)

j2kt_native_library(
    name = "readable-j2kt-native",
    srcs = glob(["*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2kt-native"],
)

readable_example(
    srcs = glob([
        "*.java",
        "native_sources/*.js",
    ]),
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    generate_wasm_readables = False,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)
