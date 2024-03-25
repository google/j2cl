load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "superinject-j2cl",
    srcs = glob([
        "super/**/*.java",
        "super/**/*.native.js",
        "super-j2cl/**/*.java",
        "super-j2cl/**/*.native.js",
    ]),
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

integration_test(
    name = "nativeinjection",
    srcs = glob([
        "*.java",
        "*.js",
        "native_srcs/*.native.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":superinject",
    ],
)
