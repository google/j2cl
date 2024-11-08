load("//build_defs:rules.bzl", "j2kt_jvm_library", "j2kt_native_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2kt_jvm_library(
    name = "readable-j2kt-jvm",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
        "//third_party:jspecify_annotations-j2kt-jvm",
    ],
)

j2kt_native_library(
    name = "readable-j2kt-native",
    srcs = glob([
        "*.java",
        "*.kt",
    ]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-native",
        "//third_party:jspecify_annotations-j2kt-native",
    ],
)

readable_example(
    srcs = glob(["*.java"]),
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)
