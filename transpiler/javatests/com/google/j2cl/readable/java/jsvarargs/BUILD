load("//build_defs:rules.bzl", "j2kt_jvm_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

# Exclude native sources from j2kt_jvm_library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    j2kt_j2objc_interop_enabled = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
        "//third_party:jsinterop-annotations-j2kt-jvm",
        "//transpiler/javatests/com/google/j2cl/readable/java/common:common-j2kt-jvm",
    ],
)

readable_example(
    srcs = glob([
        "*.java",
        "native_sources/*.js",
    ]),
    build_kt_native_readables = False,  # JS varargs doesn't apply to J2KT native.
    generate_wasm_custom_descriptors_jsinterop_readables = True,
    generate_wasm_imports = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
