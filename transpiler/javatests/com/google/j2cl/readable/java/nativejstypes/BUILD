load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
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
        "//third_party:gwt-jsinterop-annotations-j2wasm",
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
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    # TODO(b/202515129): Allow dependencies on native code (like js & iOS) in J2KT.
    generate_kt_readables = False,
    generate_wasm_imports = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
        "//third_party:jsr305_annotations-j2cl",
    ],
)
