load("@bazel_tools//tools/build_defs/kotlin/native:rules.bzl", "kt_native_library")
load("//build_defs:rules.bzl", "j2cl_library", "j2kt_native_import")
load("@rules_java//java:defs.bzl", "java_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# The source files that we want to test. Other files are part of the test fixture.
READABLE_SRCS = [
    "Protobuf.java",
]

java_library(
    name = "testfixture",
    srcs = glob(
        ["*.java"],
        exclude = READABLE_SRCS,
    ),
    deps = [
        "//third_party:jspecify_annotations",
    ],
)

# J2kt-JVM proto dependencies are untranspiled Java libraries
alias(
    name = "testfixture-j2kt-jvm",
    actual = "testfixture",
)

kt_native_library(
    name = "testfixture-kt-native",
    srcs = glob(["*.kt"]),
    tags = ["manual"],
)

# Kotlin Native proto dependencies are imported Kotlin Native libraries
j2kt_native_import(
    name = "testfixture-j2kt-native",
    jar = "testfixture",
    runtime = "testfixture-kt-native",
    tags = ["manual"],
)

# Other transpiled platforms that do not involve Kotlin
j2cl_library(
    name = "testfixture-j2cl",
    srcs = glob(
        ["*.java"],
        exclude = READABLE_SRCS,
    ),
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)

readable_example(
    srcs = READABLE_SRCS,
    experimental_enable_jspecify_support_do_not_enable_without_jspecify_static_checking_or_you_might_cause_an_outage = True,
    deps = [
        ":testfixture-j2cl",
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jspecify_annotations-j2cl",
    ],
)
