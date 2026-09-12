load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

# Exclude native sources from j2kt_jvm_library and j2kt_native_library.
j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob(["*.java"]),
    j2kt_j2objc_interop_enabled = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
        "//third_party:jsinterop-annotations-j2kt-jvm",
    ],
)

j2kt_native_library(
    name = "readable-j2kt-native",
    srcs = glob(["*.java"]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-native",
        "//third_party:jsinterop-annotations-j2kt-native",
    ],
)

readable_example(
    srcs = glob([
        "*.java",
        "native_sources/*.js",
    ]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
