load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "helper",
    srcs = ["Helper.java"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

readable_example(
    srcs = ["Main.java"],
    build_kt_native_readables = False,  # JS only external code.
    generate_kt_web_readables = True,
    generate_wasm_readables = False,  # @JsAsync not supported in WASM.
    deps = [
        ":helper",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
