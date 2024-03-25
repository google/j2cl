load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2wasm_library")
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
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
        "//third_party:jsinterop-annotations-j2kt-jvm",
        "//third_party:jspecify_annotations-j2kt-jvm",
        "//third_party:jsr305_annotations-j2kt-jvm",
    ],
)

j2wasm_library(
    name = "readable-j2wasm",
    srcs = glob(
        [
            "*.java",
            "native_sources/*.js",
        ],
        exclude = [
            "NativeEnum.java",
            "NonNative.java",
        ],
    ),
    javacopts = [
        "-Xep:CheckReturnValue:OFF",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//third_party:jspecify_annotations-j2wasm",
        "//third_party:jsr305_annotations-j2wasm",
    ],
)

readable_example(
    srcs = glob([
        "*.java",
        "*.native.js",
        "native_sources/*.js",
    ]),
    # Native JsType does not make sense for j2kt-native.
    build_kt_native_readables = False,
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    generate_wasm_imports = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)
